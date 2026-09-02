# AI prompt — profile integration (customer, courier, restaurant owner)

Paste the **Shared contract** section plus **one** role section into your AI coding
tool. The three role sections are independent; do not paste all three at once —
each app only owns its own role, and giving the model the other two produces
endpoints your app is not authorized to call.

Everything below was read out of the backend source, not inferred. Where the API
is inconsistent it says so rather than pretending otherwise — those are the
places integrations actually break.

---

## Shared contract

> You are integrating a React Native (Expo) app with the ZBR backend.
>
> **Base URL:** `https://zbrr.uz/api/v1`
> **Staging:** `https://staging.zbrr.uz/api/v1` (Swagger is enabled there:
> `https://staging.zbrr.uz/swagger-ui.html`. It is off in production.)
>
> ### Response envelope
>
> Almost every endpoint wraps its payload:
>
> ```json
> {
>   "success": true,
>   "message": "Profile retrieved",
>   "data": { },
>   "timestamp": "2026-08-31T09:42:19Z"
> }
> ```
>
> Always read `response.data.data`, never `response.data`. Write one API client
> that unwraps `.data` once, so no call site has to remember.
>
> **Exception:** the `/notifications` endpoints return the DTO at the top level
> with no envelope. Do not unwrap those. This is an inconsistency in the backend,
> not something you should work around silently — special-case it in the client
> and leave a comment.
>
> ### Nulls are absent, not null
>
> The backend serializes with `default-property-inclusion: non_null`. A field with
> no value is **missing from the JSON entirely**. So:
>
> - Never distinguish "field is null" from "field is absent" — they are the same.
> - Never assume a documented field will be present. `avatarUrl`, `defaultAddress`,
>   `lastSeenAt`, `email` are all routinely absent.
> - In TypeScript, type every optional field as `field?: T`, not `field: T | null`.
>
> ### Sending nulls back
>
> On update endpoints, the backend treats `null`/omitted as **"do not change this
> field"**, not "clear it". To clear a text field, send an empty string `""`.
>
> This matters most for addresses: sending a partial body used to wipe the fields
> you left out. That is fixed, but the semantics are now explicit — omit to keep,
> `""` to clear.
>
> ### Timestamps
>
> All timestamps are UTC ISO-8601 **with a trailing `Z`, and no fractional
> seconds**: `2026-09-01T13:06:32Z`. Pinned by a test
> (`JacksonTimestampFormatTest`), so it will not drift.
>
> `new Date(raw)` is therefore correct on its own — the `Z` makes it
> unambiguous. Do not append one, and do not strip one.
>
> Two consequences worth stating:
>
> - **Second precision only.** Milliseconds are truncated, not rounded. Never
>   use these values to order two events that can happen within the same second;
>   use the server's ordering (list endpoints are already sorted) instead.
> - **Display in `Asia/Tashkent`.** The business day runs on Tashkent
>   wall-clock, so a midnight-to-5am order belongs to the day the customer
>   thinks it does.
>
> ### Money
>
> Amounts are Uzbek som (UZS) as decimal numbers. There are no cents in practice.
> Format with thousands separators and the `so'm` suffix; never render a currency
> symbol or two decimal places.
>
> ### Authentication
>
> Send `Authorization: Bearer <accessToken>` on every authenticated request.
>
> Access tokens are short-lived; refresh tokens last 7 days. On any `401`,
> refresh once and retry the original request; if the refresh also fails, clear
> tokens and send the user to login. Implement this as a single-flight queue —
> concurrent 401s must trigger **one** refresh, not one per request.
>
> ```
> POST /auth/refresh
> { "refreshToken": "<token>" }
> → { "data": { "accessToken": "...", "refreshToken": "<same token back>", "expiresIn": 3600 } }
> ```
>
> Note `expiresIn` here is in **seconds**. Store tokens in `expo-secure-store`,
> never `AsyncStorage`.
>
> ### Errors
>
> Errors come back with `success: false` and a human-readable `message`. Surface
> `message` directly — it is written for end users ("Email is already in use",
> "Invalid verification code. 2 attempts remaining."). Validation failures return
> `400` with the field message from the constraint.
>
> `401` on an endpoint you expect to be public usually means you sent a `HEAD`
> request — the backend's public-route matchers are `GET`-specific.

---

## 1. Customer app

> ### Sign-in: phone + OTP
>
> Customers have **no password**. The whole flow is phone + 6-digit OTP.
>
> Phone format accepted: `+998901234567`, `998901234567`, or `901234567`. The
> backend normalizes all three. Send whatever the input mask produces.
>
> **Step 1 — request the code**
>
> ```
> POST /auth/phone/request-otp
> { "phone": "998901234567" }
> ```
> ```json
> { "data": {
>     "phone": "9989****67",
>     "message": "Verification code sent to your phone",
>     "expiresInSeconds": 300,
>     "isNewUser": true,
>     "remainingAttempts": 3
> } }
> ```
>
> Use `isNewUser` to branch: `true` → collect a name after verifying;
> `false` → straight into the app. Use `expiresInSeconds` to drive the resend
> countdown; do not hardcode 300.
>
> **Step 2a — existing user**
>
> ```
> POST /auth/phone/verify-otp
> { "phone": "998901234567", "code": "483920" }
> ```
> ```json
> { "data": {
>     "accessToken": "eyJ...", "refreshToken": "eyJ...",
>     "tokenType": "Bearer", "expiresIn": 3600000,
>     "user": { "id": 42, "phone": "998901234567", "fullName": "Asad Karimov", "role": "CONSUMER" }
> } }
> ```
>
> **Careful:** `expiresIn` is **milliseconds** on this endpoint and **seconds** on
> `/auth/refresh`. This is a real inconsistency in the backend. Do not compute
> expiry from it — refresh reactively on 401 instead.
>
> **Step 2b — new user** (same OTP, do not request a second one)
>
> ```
> POST /auth/phone/complete-registration
> { "phone": "998901234567", "otp": "483920", "fullName": "Asad Karimov", "email": "optional@example.com" }
> ```
>
> Wrong codes return `400` with the attempts remaining in `message`. After the
> attempts are exhausted the code is dead and the user must request a new one.
>
> ### Profile
>
> ```
> GET /consumers/profile      — requires ROLE_CONSUMER
> PUT /consumers/profile
> ```
>
> Both return the **same shape**, so you can replace your cached profile with the
> PUT response directly:
>
> ```json
> { "data": {
>     "id": 42,
>     "phone": "998901234567",
>     "firstName": "Asad", "lastName": "Karimov", "fullName": "Asad Karimov",
>     "email": "asad@example.com",
>     "avatarUrl": "https://zbrr.uz/uploads/profiles/42/abc.jpg",
>     "profileImageUrl": "https://zbrr.uz/uploads/profiles/42/abc.jpg",
>     "memberSince": "2026-03-14T08:11:02Z",
>     "createdAt": "2026-03-14T08:11:02Z",
>     "lastSeenAt": "2026-08-31T09:41:55Z",
>     "totalOrders": 12,
>     "defaultAddress": { "id": 7, "label": "Home", "fullAddress": "Amir Temur 1", "isDefault": true },
>     "role": "CONSUMER", "status": "ACTIVE",
>     "phoneVerified": true, "emailVerified": false
> } }
> ```
>
> `avatarUrl`/`profileImageUrl` and `memberSince`/`createdAt` are aliases of each
> other — both are sent, pick one and be consistent.
>
> **Updating.** Send only what changed. Omitted fields are untouched.
>
> ```
> PUT /consumers/profile
> { "fullName": "Asad Karimov" }
> ```
>
> `fullName` splits on the first space; a single word clears the surname. You can
> send `firstName`/`lastName` instead — if you send both forms, the explicit
> `firstName`/`lastName` win. Prefer one form throughout the app.
>
> Changing `email` sets `emailVerified` back to `false` and returns `400` if the
> address belongs to another account. There is no email verification flow yet, so
> do not gate anything on `emailVerified`.
>
> Field limits: `firstName`/`lastName` 50 chars, `fullName` 101, `email` 100.
>
> **Avatar upload** is a separate multipart call that returns the whole profile:
>
> ```
> POST /consumers/profile/picture     multipart/form-data, field name: "file"
> ```
>
> ### Saved addresses
>
> ```
> GET    /consumers/addresses
> POST   /consumers/addresses              → 201
> PUT    /consumers/addresses/{id}
> DELETE /consumers/addresses/{id}
> PUT    /consumers/addresses/{id}/default
> ```
>
> ```json
> {
>   "label": "Home",
>   "fullAddress": "Amir Temur ko'chasi 1, Toshkent",
>   "latitude": 41.311081, "longitude": 69.240562,
>   "apartmentNumber": "42", "entrance": "2",
>   "instructions": "Ring twice, dog is friendly"
> }
> ```
>
> Only `fullAddress` is required. The **first** address a user creates is
> automatically the default. Setting a new default clears the old one server-side
> — do not try to unset the previous one yourself.
>
> On `PUT`, omitted fields are preserved and `""` clears. So a "move the pin"
> update sends only `fullAddress`, `latitude`, `longitude` and keeps the apartment
> details intact.
>
> The list comes back default-first, then newest-first. Render in that order; do
> not re-sort.
>
> ### Referrals
>
> ```
> GET /referrals/my
> ```
> ```json
> { "data": {
>     "referralCode": "ASAD7X2K",
>     "referralLink": "https://zbrr.uz/invite/ASAD7X2K",
>     "totalReferrals": 3, "earnedCredits": 45000, "pendingCredits": 15000
> } }
> ```
>
> A code is generated on first read, so `referralCode` is always present — you do
> not need a "generate code" button. Share `referralLink` verbatim; do not build
> the URL yourself.
>
> ### Building the profile screen
>
> Fetch `GET /consumers/profile` once on mount. It already contains
> `defaultAddress` and `totalOrders`, so **do not** additionally call
> `/consumers/addresses` or the orders list just to populate the header.
>
> `lastSeenAt` is the user's own last activity — it is not useful on their own
> profile screen. Ignore it here.

---

## 2. Courier app

> ### Becoming a courier
>
> A courier is a **consumer account with a courier profile attached**, not a
> separate account type. The order is fixed:
>
> 1. Sign in with phone + OTP exactly as the customer app does
>    (`/auth/phone/request-otp` → `/auth/phone/verify-otp`). This yields a
>    `CONSUMER`.
> 2. `POST /couriers/register` — requires `ROLE_CONSUMER`, adds `ROLE_COURIER`.
>
> ```
> POST /couriers/register
> { "vehicleType": "MOTORCYCLE", "vehicleNumber": "01A123BC", "licenseNumber": "AB1234567", "preferredRadiusKm": 5 }
> ```
>
> `vehicleType` is required, one of: `WALKING`, `BICYCLE`, `E_BIKE`,
> `MOTORCYCLE`, `CAR`. Everything else is optional; `preferredRadiusKm` defaults
> to 5.
>
> The new courier lands in **`PENDING_APPROVAL`** and an admin must verify them.
> Until then, every `ROLE_COURIER` endpoint is unavailable. Build an explicit
> "awaiting approval" screen — poll `GET /couriers/me` on app focus, not on a
> timer, and show the user what is happening. Do not show an error.
>
> **You do not need to refresh the token after registering.** The backend
> re-reads the user's roles from the database on every request rather than
> trusting the token's claims, so `ROLE_COURIER` takes effect on the very next
> call. (The same mechanism means a suspension takes effect immediately, without
> waiting for the token to expire.)
>
> ### Profile
>
> ```
> GET /couriers/me      — requires ROLE_COURIER
> PUT /couriers/me
> ```
> ```json
> { "data": {
>     "id": 9, "userId": 42,
>     "userName": "Asad Karimov", "phone": "998901234567", "email": "asad@example.com",
>     "status": "AVAILABLE",
>     "vehicleType": "MOTORCYCLE", "vehicleNumber": "01A123BC",
>     "currentLat": 41.311081, "currentLng": 69.240562,
>     "totalDeliveries": 217, "averageRating": 4.8,
>     "verified": true, "currentOrderCount": 1
> } }
> ```
>
> Note `id` (courier id) and `userId` are different. Courier endpoints use
> `/me`, so you rarely need either — but if you cache, key on `userId`.
>
> `PUT /couriers/me` accepts `vehicleType`, `vehicleNumber`, `licenseNumber`,
> `preferredRadiusKm`, `maxConcurrentOrders`. It does **not** change the person's
> name, phone or avatar — those live on the user record:
>
> ```
> GET /users/me
> PUT /users/me     { "firstName": "Asad", "lastName": "Karimov", "phone": "...", "profileImageUrl": "..." }
> ```
>
> So the courier profile screen reads from **two** endpoints and writes to two.
> Fetch them in parallel and merge; treat `/users/me` as identity and
> `/couriers/me` as work data.
>
> ### Going online and offline
>
> ```
> PATCH /couriers/me/status?status=AVAILABLE      ← query parameter
> PUT   /couriers/me/status   { "status": "AVAILABLE" }   ← JSON body
> ```
>
> Both exist and do the same thing. **Use the `PUT` with a JSON body** — the
> `PATCH` variant takes the value as a query parameter, which is easy to get
> wrong and awkward to log. Pick one and use it everywhere.
>
> Statuses: `OFFLINE`, `AVAILABLE`, `BUSY`, `ON_BREAK`, `SUSPENDED`.
> Only `OFFLINE`, `AVAILABLE` and `ON_BREAK` are user-selectable. `BUSY` is set
> by the system when the courier hits their concurrent-order limit, and
> `SUSPENDED` is set by an admin — the backend rejects self-service attempts to
> leave `SUSPENDED` or `PENDING_APPROVAL`. Render those two as read-only states
> with an explanation, not as options in the picker.
>
> ### Location updates
>
> ```
> PUT  /couriers/me/location   { "latitude": 41.311081, "longitude": 69.240562 }
> POST /couriers/me/location?lat=41.311081&lng=69.240562
> ```
>
> Two variants again, and note the field names differ (`latitude`/`longitude` in
> the body, `lat`/`lng` in the query). **Use the `PUT` with the JSON body.**
>
> Send updates only while `status === 'AVAILABLE'` or an order is active. Throttle
> to one update every 10–15 seconds and skip sends when the device has not moved
> more than ~20m — this runs on the courier's battery and data.
>
> ### Work endpoints
>
> ```
> GET  /couriers/me/available-orders
> GET  /couriers/me/orders/active
> GET  /couriers/me/orders/history        (paged)
> GET  /couriers/me/orders/{orderId}
> POST /couriers/me/orders/{orderId}/accept
> PUT  /couriers/me/orders/{orderId}/pickup
> PUT  /couriers/me/orders/{orderId}/transit
> POST /couriers/me/orders/{orderId}/complete
> POST /couriers/me/orders/{orderId}/issue
> GET  /couriers/me/earnings
> GET  /couriers/me/reviews               (paged)
> ```
>
> `accept` is racy by design — two couriers can tap the same order and one will
> lose. Handle the failure as a normal outcome ("this order was just taken"), not
> an error dialog, and refresh the available list.
>
> Earnings returns today/week/month/total for both amounts and delivery counts,
> plus `cashEarnings`, `cardEarnings`, `pendingPayout` and `withdrawableBalance`.
> All UZS.
>
> ### Live updates
>
> Connect STOMP over WebSocket to `wss://zbrr.uz/ws` with
> `Authorization: Bearer <accessToken>` as a **CONNECT header**.
>
> Authentication happens only at CONNECT. When the access token expires the
> socket does not re-authenticate — refresh the token first, then reconnect with
> the new one. A CONNECT with a missing or expired token is rejected outright, so
> treat connect failure as "refresh and retry once", then back off.

---

## 3. Restaurant owner app

> ### Sign-in
>
> Restaurant owners use **email + password**, not OTP.
>
> ```
> POST /auth/login
> { "emailOrPhone": "owner@example.com", "password": "...", "deviceInfo": "iPhone 15 / iOS 18" }
> ```
>
> Registration, if you expose it in-app:
>
> ```
> POST /auth/register
> { "email": "...", "password": "...", "firstName": "...", "lastName": "...", "role": "RESTAURANT_OWNER" }
> ```
>
> Passwords must contain a lowercase letter, an uppercase letter, a digit and one
> of `@#$%^&+=!`. Validate client-side against exactly that set before submitting,
> and show the rule up front rather than after a rejection.
>
> `role` is only honoured for `CONSUMER`, `COURIER` and `RESTAURANT_OWNER`;
> anything else is rejected. Admin roles are granted server-side.
>
> ### The owner's own profile
>
> There is no restaurant-owner-specific user endpoint. Use the generic one:
>
> ```
> GET /users/me
> PUT /users/me     { "firstName": "...", "lastName": "...", "phone": "...", "profileImageUrl": "..." }
> ```
>
> ### Restaurants
>
> An owner can have **more than one** restaurant. Design for a list with a
> selector, not a single implicit restaurant — even if today every owner has one.
>
> ```
> GET  /restaurants/my        → RestaurantDto[]
> POST /restaurants           → 201, creates one owned by the caller
> PUT  /restaurants/{id}      partial update — only provided fields change
> ```
>
> Creating requires `name`, `phone`, `addressLine1`, `city`. Optional and worth
> collecting at creation time: `description`, `email`, `latitude`, `longitude`,
> `minimumOrder`, `deliveryFee`, `deliveryRadiusKm`, `averagePrepTimeMinutes`,
> `opensAt`, `closesAt`, `acceptsDelivery`, `acceptsTakeaway`, `acceptsDineIn`.
>
> `opensAt`/`closesAt` are local times as `"09:00:00"`.
>
> A restaurant reads back as:
>
> ```json
> { "data": {
>     "id": 3, "ownerId": 42, "name": "Osh Markazi", "slug": "osh-markazi",
>     "phone": "998712001122", "fullAddress": "Amir Temur 1, Toshkent",
>     "latitude": 41.311081, "longitude": 69.240562,
>     "status": "ACTIVE",
>     "isOpen": true, "isCurrentlyOpen": true,
>     "opensAt": "09:00:00", "closesAt": "23:00:00",
>     "minimumOrder": 30000, "deliveryFee": 15000, "deliveryRadiusKm": 7,
>     "averagePrepTimeMinutes": 25,
>     "acceptsDelivery": true, "acceptsTakeaway": true, "acceptsDineIn": false,
>     "averageRating": 4.6, "totalRatings": 128, "totalOrders": 1902,
>     "logoUrl": "...", "coverImageUrl": "...",
>     "createdAt": "2026-03-14T08:11:02Z"
> } }
> ```
>
> **`isOpen` vs `isCurrentlyOpen`.** `isOpen` is the owner's manual switch.
> `isCurrentlyOpen` is `status === 'ACTIVE' && isOpen === true` — it accounts for
> the restaurant being suspended, but **not** for `opensAt`/`closesAt`. Opening
> hours are currently descriptive only: the backend does not close a restaurant
> automatically when its hours end.
>
> Bind the toggle to `isOpen` and show the effective state from
> `isCurrentlyOpen`. Do **not** compute openness from `opensAt`/`closesAt`
> yourself — you would disagree with the backend, and customers would see a
> different answer than the owner. If automatic hour-based closing is wanted,
> that is a backend change; raise it rather than emulating it client-side.
>
> ```
> PATCH /restaurants/{id}/toggle-open
> ```
>
> ### Images and location
>
> ```
> POST  /restaurants/{id}/logo            multipart, field "file"
> POST  /restaurants/{id}/cover-image     multipart, field "file"
> PATCH /restaurants/{id}/location?latitude=41.311081&longitude=69.240562
> ```
>
> The location endpoint takes **query parameters**, unlike everything else. It is
> equivalent to sending `latitude`/`longitude` through `PUT /restaurants/{id}` —
> prefer the `PUT` and treat this one as legacy.
>
> ### What the owner cannot do
>
> `PATCH /restaurants/{id}/status` (`ACTIVE`, `SUSPENDED`, …) is **admin-only**.
> An owner cannot suspend or reactivate their own restaurant. Do not put it in
> the UI; if a restaurant is suspended, show a message directing them to support.
>
> ### Reports
>
> ```
> GET /restaurants/{restaurantId}/financial-report?startDate=...&endDate=...
> GET /restaurants/{restaurantId}/reviews                        (public, paged)
> ```
>
> `startDate` and `endDate` are **required** and are ISO date-**times**, not
> dates: `2026-08-30T19:00:00`. They are compared against UTC-stored columns, so
> **you must send UTC instants** — a plain date is rejected, and sending Tashkent
> local time silently shifts the report by 5 hours.
>
> To ask for "today" as the owner understands it, convert the Tashkent day
> boundaries to UTC before sending — Tashkent is UTC+5 year-round, so a local day
> runs from `19:00` the previous UTC day to `19:00` the current one:
>
> ```ts
> // "Today" in Tashkent, expressed as the UTC instants the API expects.
> const startOfDayUtc = (tashkentDate: string) =>
>   new Date(`${tashkentDate}T00:00:00+05:00`).toISOString().slice(0, 19);
>
> startDate = startOfDayUtc('2026-08-31');  // "2026-08-30T19:00:00"
> ```
>
> Put this in one helper and use it for every report range. Getting it wrong is
> not a visible error — the numbers are just quietly wrong near midnight.
>
> ### Authorization
>
> Ownership is checked server-side on every mutation — an owner passing another
> restaurant's `id` gets `403`, not silent success. Still scope the UI to
> `/restaurants/my` and never let a restaurant id reach a request from anywhere
> but that list.

---

## Known inconsistencies

Do not "fix" these client-side by guessing. They are listed so you recognise
them rather than lose an afternoon:

| Where | What |
|---|---|
| `/notifications/*` | Returns the DTO unwrapped; everything else uses the `ApiResponse` envelope. |
| `expiresIn` | Milliseconds from `/auth/phone/verify-otp`, seconds from `/auth/refresh`. |
| `/couriers/me/status` | `PUT` takes a JSON body, `PATCH` takes a query parameter. |
| `/couriers/me/location` | `PUT` body uses `latitude`/`longitude`; `POST` query uses `lat`/`lng`. |
| `/restaurants/{id}/location` | Query parameters, unlike every other update. |
| Consumer vs generic profile | `/consumers/profile` (CONSUMER only) and `/users/me` (any role) both return `UserDto` but populate different subsets — only `/consumers/profile` fills `defaultAddress`, `totalOrders`, `avatarUrl`, `memberSince`. |

If you hit something that contradicts this document, the document is wrong —
report it rather than working around it.
