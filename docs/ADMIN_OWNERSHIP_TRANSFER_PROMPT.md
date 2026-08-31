# AI prompt — admin panel: restaurant ownership transfer

Paste everything below the line into your AI coding tool. It is written against
the backend as it actually is, including the two places the supporting APIs will
bite you.

---

> You are adding a **restaurant ownership transfer** feature to the ZBR admin
> panel (React). The backend endpoint already exists and is deployed.
>
> ## What this feature is for
>
> A restaurant's owner is set once, implicitly, from whoever created it. In
> practice the platform admin creates restaurants, so almost every restaurant is
> currently owned by the admin account and the real owner is linked to nothing.
> This feature is how an operator links a restaurant to its real owner, and how
> a restaurant is later moved between owners when a business changes hands.
>
> Treat it as a **destructive, irreversible-from-the-user's-side action**. The
> previous owner loses all access the instant it commits — there is no
> co-ownership and no handover period — and undoing it requires another
> transfer. Design the UI to match that weight: this is not an inline dropdown.
>
> ## API
>
> **Base URL:** `https://zbrr.uz/api/v1`. Every response is wrapped:
> `{ success, message, data, timestamp }` — read `.data`.
>
> ### The transfer
>
> ```
> PATCH /restaurants/{id}/owner
> Authorization: Bearer <admin access token>
> Content-Type: application/json
>
> { "newOwnerId": 5 }
> ```
>
> Returns the updated restaurant:
>
> ```json
> { "success": true, "message": "Ownership transferred",
>   "data": { "id": 3, "name": "Osh Markazi", "slug": "osh-markazi", "ownerId": 5, "status": "ACTIVE", … } }
> ```
>
> Requires `ADMIN` or `PLATFORM`. A restaurant owner cannot transfer their own
> restaurant — do not build this into the owner-facing app.
>
> **Behaviour you must reflect in the UI:**
>
> - The new owner is granted the `RESTAURANT_OWNER` role automatically if they
>   lack it. Do not call the role endpoint yourself first — it is redundant, and
>   a partial failure would leave a user holding a role for a restaurant they
>   were never given.
> - The **previous owner keeps** the `RESTAURANT_OWNER` role, because they may
>   still own other restaurants. If they are leaving the platform entirely,
>   that is a separate deliberate action:
>   `DELETE /users/{id}/roles/RESTAURANT_OWNER`. Surface this as a follow-up
>   suggestion after a successful transfer **only** when the previous owner now
>   owns zero restaurants — never do it automatically.
> - Transferring to the user who already owns it is a **no-op that returns 200**,
>   not an error. A retry after a network timeout is safe: retry rather than
>   asking the operator to check whether it went through.
>
> ### Errors
>
> | Status | `message` | What the UI should do |
> |---|---|---|
> | 400 | `Cannot transfer to a SUSPENDED account. Activate the user first.` | Block, and offer a link to that user's admin page. The backend refuses because a restaurant owned by a suspended account is unreachable by its owner. |
> | 404 | `User not found with id: '99'` | Only reachable if the picker is stale — refresh the user list. |
> | 404 | `Restaurant not found with id: '3'` | Restaurant was deleted in another tab. |
> | 403 | — | The operator is not ADMIN/PLATFORM. Hide the action for these users rather than letting them discover it via an error. |
>
> Surface the backend's `message` directly; it is written to be read.
>
> ### Choosing the new owner
>
> Two endpoints, both `ADMIN`/`PLATFORM`, both paginated
> (`?page=0&size=20`, response `{ content, page, size, totalElements, totalPages, first, last, empty }`):
>
> ```
> GET /users/role/RESTAURANT_OWNER      — existing owners
> GET /users/search?q=<query>           — everyone, by name or email
> ```
>
> `/users/search` is case-insensitive and matches **email, first name, last
> name, the full name, and phone number**. So all of these find Asad Karimov:
> `asad`, `KARIMOV`, `Asad Karimov`, `asad@example.com`, `+998 90 123 45 67`,
> `998901234567`, `901234`.
>
> Phone numbers are matched on digits alone, so whatever punctuation the
> operator pastes is irrelevant. A query containing letters is never treated as
> a phone number, so searching an email with a digit in it does not drag in
> unrelated phone matches.
>
> Label the field "Search by name, email or phone". Do not filter results
> client-side — the endpoint is paginated and client filtering breaks past the
> first page.
>
> Default the picker to `GET /users/role/RESTAURANT_OWNER` (the common case is
> moving a restaurant between existing owners) with search as the escape hatch
> for a first-time owner.
>
> ## UI requirements
>
> Put the action on the restaurant's admin detail page, in a section clearly
> separated from ordinary edits — the same visual weight you would give
> "suspend" or "delete". Not in the main edit form, where it could be triggered
> by a mis-click while editing a phone number.
>
> **The flow:**
>
> 1. Show the current owner: name, email, and user id. The id matters — operators
>    verify against it.
> 2. "Transfer ownership" opens a modal.
> 3. In the modal, the operator searches and selects the new owner. Show the
>    candidate's **name, email, id, and account status** in the result row.
>    Render `SUSPENDED`/`INACTIVE` candidates as visibly disabled with the reason
>    — the backend will reject them, so let the operator see that before they
>    commit rather than after.
> 4. A confirmation step that states the consequence in full and names both
>    parties:
>
>    > Transfer **Osh Markazi** from **Asad Karimov** (asad@example.com) to
>    > **Dilnoza Rahimova** (dilnoza@example.com)?
>    >
>    > Asad Karimov will lose all access to this restaurant immediately. This
>    > cannot be undone except by transferring it back.
>
>    Require an explicit confirm. Do not use a type-the-name-to-confirm pattern
>    here — it is a legitimate routine operation during onboarding and that
>    friction would push operators back to running SQL.
> 5. On success, refresh the restaurant and show the new owner.
>
> **Disable the confirm button while the request is in flight** and keep it
> disabled until the response arrives. A double-submit is harmless here (the
> second is a no-op) but a spinner-less double-click looks broken.
>
> ## After a successful transfer
>
> Invalidate every cached view of this restaurant on the client — the detail
> page, the restaurant list, and any owner-scoped list. If you use React Query,
> invalidate the restaurant queries rather than patching the cache by hand: the
> response only tells you about this restaurant, and the previous owner's
> restaurant list has also changed.
>
> Note the backend serves `GET /restaurants/{id}` from a 5-minute cache, which
> the transfer evicts correctly — so a refetch returns the new owner
> immediately. You do not need a delay or a retry loop.
>
> ## What NOT to build
>
> - No bulk transfer. Moving several restaurants at once multiplies the cost of
>   a mis-selected owner, and there is no undo.
> - No "pending transfer" or approval workflow. The backend commits immediately;
>   a UI that implies otherwise would be lying.
> - No owner self-service transfer, anywhere. The endpoint rejects it, and it is
>   rejected deliberately: it would turn a compromised owner account into a way
>   to move a business out of the platform's reach.
