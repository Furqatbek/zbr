# Token Refresh — Required for All Three Apps

**Status: production incident, not a future improvement.** On 25 Aug, customer
app and web panel requests were rejected for ~50 minutes because they kept
sending an access token that had expired at 10:46 UTC. Logins succeeded, then
every subsequent call returned 401. Nothing was wrong with the tokens or the
backend — the clients never refreshed.

Every app needs the flow below. None of them has it today.

---

## The two tokens

Login (`/auth/login`) and OTP verification (`/auth/phone/verify-otp`) both return
**two** tokens. Most clients store only the first.

| | Lifetime | Purpose |
|---|---|---|
| `accessToken` | **1 hour** | Sent on every request. Short-lived on purpose. |
| `refreshToken` | **7 days** | Used *only* to get a new access token. |

A correctly-implemented client keeps a user signed in for **7 days** without
re-entering an OTP. Without refresh, the session dies after 1 hour — and until
recently after 15 minutes, which is what the incident above was.

Store both, in secure storage (Keychain / EncryptedSharedPreferences —
`expo-secure-store`, not `AsyncStorage`).

---

## The refresh call

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{ "refreshToken": "<the stored refresh token>" }
```

Success — **200**:

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": 42,
    "roles": ["CONSUMER"]
  }
}
```

Three things to get right:

- The token is at **`data.accessToken`** — every response is wrapped in the
  `{success, message, data}` envelope.
- **`expiresIn` is seconds**, not milliseconds.
- **Store `data.refreshToken` too.** Today it comes back unchanged, but write the
  code as if it rotates so nothing breaks when it does.

No auth header is needed on this call — the refresh token in the body *is* the
credential.

---

## Failure returns 400, not 401

This is the easiest thing to get wrong. A failed refresh is a **400 Bad
Request**:

```json
{ "success": false, "message": "Refresh token has been revoked. Please login again." }
```

If your interceptor only treats 401 as "auth problem", a dead refresh token
looks like a generic request failure and you'll retry it forever. Treat **any
non-200 from `/auth/refresh`** as terminal: clear both tokens and send the user
to login.

Messages you may see, all with status 400:

| Message | Meaning |
|---|---|
| `Refresh token has expired. Please login again.` | Past 7 days |
| `Refresh token has been revoked. Please login again.` | Logged out elsewhere, or password changed |
| `Invalid refresh token. Ensure you are sending the refresh token, not the access token.` | You sent the wrong one |
| `Account is not active. Please contact support.` | Suspended or banned |

---

## REST: what the interceptor must do

On **401** from any API call:

1. Call `/auth/refresh` with the stored refresh token.
2. On success, store the new tokens and **retry the original request once**.
3. On failure, clear tokens and route to login.
4. Never retry more than once — a second 401 after a fresh token is a real
   authorization failure, not an expiry.

**Refresh must be single-flight.** When a screen fires five requests and all five
get 401, you must issue **one** refresh and have the other four wait for it. Five
concurrent refreshes is the classic way to end up with four wasted round trips
and a race over which token gets stored last.

```ts
let inFlight: Promise<string> | null = null;

async function refreshOnce(): Promise<string> {
  if (!inFlight) {
    inFlight = doRefresh().finally(() => { inFlight = null; });
  }
  return inFlight;
}
```

Optionally refresh *proactively* at ~80% of `expiresIn` to avoid the visible
retry. Do it in addition to the 401 handler, never instead of it — device clocks
drift, and the 401 is the only authority on whether a token is actually dead.

---

## WebSocket: the part that will be missed

STOMP authenticates **once**, at CONNECT, and is never re-checked. So an access
token expiring mid-session does **not** drop your socket — an already-open
connection keeps working. The failure appears on the next **reconnect**.

That is exactly the loop seen in production:

```
[WS] WebSocket Closed
[WS] STOMP Error: Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]
[WS] Max reconnect attempts reached, stopping reconnection
```

That STOMP error is the server refusing a CONNECT carrying an expired token. The
client then retried with the *same* cached token, failed identically, and gave
up — leaving the user with no live order updates and no error they can act on.

**Rule: refresh the access token before every reconnect attempt, and build the
CONNECT header fresh each time.**

```ts
const client = new Client({
  brokerURL: 'wss://zbrr.uz/ws',
  beforeConnect: async () => {
    const token = await getValidAccessToken();   // refreshes if near/past expiry
    client.connectHeaders = { Authorization: `Bearer ${token}` };
  },
  reconnectDelay: 5000,
});
```

Two more things:

- The header must be a **STOMP CONNECT native header** named `Authorization`
  with the `Bearer ` prefix. A query parameter will not work.
- **Do not cap reconnect attempts at a small number.** "Max reconnect attempts
  reached, stopping reconnection" means a courier stops receiving offers and a
  vendor stops receiving orders, permanently, until the app is restarted. Use
  backoff without a hard ceiling, and reconnect on app foreground and on network
  regain.

Use `wss://zbrr.uz/ws` — the bare apex. `www.zbrr.uz` redirects, and a redirect
cannot carry a WebSocket upgrade handshake, so it fails outright.

---

## Per-app checklist

Same work in all three apps plus the web panel:

- [ ] Store `refreshToken` alongside `accessToken`, in secure storage
- [ ] 401 interceptor: refresh → retry once → else log out
- [ ] Single-flight refresh guard
- [ ] Treat any non-200 from `/auth/refresh` as terminal (**it is a 400**)
- [ ] `beforeConnect` on the STOMP client refreshes and rebuilds the header
- [ ] Remove the reconnect-attempt ceiling; use backoff instead
- [ ] Reconnect on foreground and on network regain

**Courier app**: a dropped socket that never recovers means missed order offers.
This is the app where the reconnect ceiling hurts most.

**Vendor app**: same, for incoming orders.

---

## How to verify

The bug is invisible for the first hour, so testing needs the clock forced:

1. Log in, note the time.
2. Use **staging**, where access tokens already expire after 60 seconds so the
   whole cycle is testable in two minutes:
   ```
   API        https://staging.zbrr.uz/api/v1
   WebSocket  wss://staging.zbrr.uz/ws
   Login      +998900000000 (or ...01, ...02), OTP code 123456, no SMS arrives
   ```
   On production the same test takes an hour.
3. Call any authenticated endpoint, e.g. `GET /api/v1/auth/me`.
4. Expect: one 401, one silent refresh, request succeeds. The user sees nothing.
5. Background the app for a few minutes, foreground it, confirm the socket
   reconnects and order updates still arrive.

To prove the failure mode still exists in your build, skip step 4's expectation
and watch for the request simply failing — that is what users hit today.

---

Backend contract questions → this repo. Endpoint details → `docs/auth-api.md`.
