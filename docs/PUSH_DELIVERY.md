# Push Notification Delivery (Backend)

How the backend delivers push to the mobile apps on **both** OSes, and what ops
must configure. Implements the vendor app's `PUSH_SETUP.md` contract.

## Transports

Routing is **per registered device**, so all three coexist:

| Device | Transport | Notes |
|--------|-----------|-------|
| `platform: IOS` (raw APNs hex token) | **APNs HTTP/2 + .p8** (`ApnsPushService`) | No Firebase involved |
| `platform: ANDROID` (raw FCM token) | **FCM** via Firebase Admin (`PushNotificationConsumer`) | |
| `ExponentPushToken[...]` (any platform) | **Expo Push API** (`ExpoPushService`) | For apps still on Expo tokens |

## Token registration

`POST /api/v1/device-tokens` (authenticated)

```json
{ "token": "<raw FCM or APNs token>", "platform": "ANDROID", "deviceId": "<stable device id>" }
```

- Legacy field names `deviceToken` / `deviceType` are still accepted (aliases).
- **One row per `deviceId`**: registration upserts on `(userId, deviceId)`, so an
  OS token rotation replaces the row instead of creating a duplicate (which would
  send N copies of every alert). Enforced by a partial unique index (V36).
- Unregister: `DELETE /api/v1/device-tokens` with `{"deviceToken": "..."}`;
  `DELETE /api/v1/device-tokens/all` removes all for the user.

## Payloads

**Android (FCM)** — `priority: HIGH` (mandatory to bypass Doze):
```
channel_id            orders_v2          (app.push.android.channel-id)
sound                 new_order          (no file extension)
notification_priority PRIORITY_MAX
visibility            PUBLIC
vibrate_timings       0s, 0.4s, 0.2s, 0.4s
data                  { type, orderId, ... }
```

**iOS (APNs)** — headers `apns-push-type: alert`, `apns-priority: 10`, `apns-topic`:
```json
{ "aps": { "alert": {"title": "...", "body": "..."},
           "sound": "new_order.wav", "badge": 1,
           "interruption-level": "time-sensitive" },
  "type": "...", "orderId": "1042" }
```

`data.orderId` is emitted **only when numeric** (the apps validate it before
deep-linking to `/order/{orderId}`); a non-numeric reference id is logged and
omitted rather than sent.

> **Channel bump:** if the Android app moves to `orders_v3`, set
> `PUSH_ANDROID_CHANNEL_ID=orders_v3` at the same time — a mismatch means the
> notification arrives silently (no sound/heads-up).

## Dead-token pruning

- **FCM:** `UNREGISTERED` / `INVALID_ARGUMENT` → token deactivated.
- **APNs:** `410 Unregistered` → deactivated. `400 BadDeviceToken` → deactivated
  (usually a **sandbox vs production mismatch**, see below).
- **Expo:** `DeviceNotRegistered` ticket → deactivated.

## Configuration

### iOS / APNs (required for iOS push)

| Env var | Meaning |
|---------|---------|
| `APNS_ENABLED` | `true` to enable (default `false`) |
| `APNS_KEY_BASE64` | the `.p8` contents, base64 — **SECRET**. Nothing on disk; wins over `APNS_KEY_FILE` |
| `APNS_KEY_FILE` | alternative: path to the mounted `.p8` (e.g. `/run/secrets/apns_auth_key.p8`) |
| `APNS_KEY_ID` | 10-char Key ID from the Apple Developer portal |
| `APNS_TEAM_ID` | Apple Team ID (e.g. `VQ56W9S7S9`) |
| `APNS_TOPIC` | bundle id (default `com.zbr.owner`) |
| `APNS_PRODUCTION` | `false` → `api.sandbox.push.apple.com` (Xcode/dev builds); `true` → `api.push.apple.com` (TestFlight/App Store) |

**Two ways to supply the `.p8` — pick one:**

```bash
# A. No file on the server (recommended): paste it into .env as base64
#    Linux/macOS:  base64 -w0 AuthKey_ABC123XYZ9.p8
#    PowerShell:   [Convert]::ToBase64String([IO.File]::ReadAllBytes("AuthKey_ABC123XYZ9.p8"))
APNS_KEY_BASE64=LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0t...

# B. Mount the file: drop it in ./secrets/ (gitignored, mounted read-only at /run/secrets)
APNS_KEY_FILE=/run/secrets/apns_auth_key.p8
```

⚠️ **Device tokens are environment-specific.** A token from a dev build sent to
the production host (or vice versa) is rejected with `400 BadDeviceToken` and the
token is pruned. Point `APNS_PRODUCTION` at the environment the installed build
came from.

### Android / FCM

| Env var | Meaning |
|---------|---------|
| `FIREBASE_ENABLED` | `true` to enable (default `false`) |
| `FIREBASE_CREDENTIALS_BASE64` | service-account JSON, base64 — **SECRET**. Nothing on disk; wins over the file |
| `FIREBASE_CREDENTIALS_FILE` | alternative: path to the mounted JSON (e.g. `/run/secrets/firebase-service-account.json`) |
| `PUSH_ANDROID_CHANNEL_ID`, `PUSH_ANDROID_SOUND` | optional overrides |

The credential is the **service-account private key** from
Firebase Console → Project Settings → Service accounts → *Generate new private
key*. It is **not** `google-services.json` (that one is client-side, ships inside
the app, and is not a secret). It must belong to the same Firebase project as the
app.

**Two ways to supply it — pick one:**

```bash
# A. No file on the server (recommended)
#    Linux/macOS:  base64 -w0 firebase-service-account.json
#    PowerShell:   [Convert]::ToBase64String([IO.File]::ReadAllBytes("firebase-service-account.json"))
FIREBASE_CREDENTIALS_BASE64=eyJ0eXBlIjoic2VydmljZV9hY2NvdW50Iiw...

# B. Mount the file into ./secrets/ (gitignored, mounted read-only at /run/secrets)
FIREBASE_CREDENTIALS_FILE=/run/secrets/firebase-service-account.json
```

⚠️ With `FIREBASE_ENABLED=true` and **no** usable credential, Firebase init fails
in `@PostConstruct` and **the whole app will not start** — set the flag and the
credential together. With Firebase disabled, Android pushes are logged instead of
sent and iOS/Expo delivery is unaffected.

## Not yet implemented (needs a decision)

**"No self-push"** — the vendor spec asks that the device/vendor who *caused* an
event not receive the push for it (e.g. the vendor who tapped Accept). The
notification pipeline currently addresses a **user**, not an originating device,
so suppressing it requires threading an "actor" through the event → notification
path. Say the word and it can be added; today the client dedupes by `orderId`
alongside the WebSocket event, so the practical impact is a redundant buzz.
