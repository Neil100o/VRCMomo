# VRCMomo Boop Progress

## References

- Official/community API reference provided during development:
  - https://vrchat.community/reference/boop

## Current findings

### Existing code paths

#### User profile Boop entry

Path:

`composeApp/src/commonMain/kotlin/presentation/screens/user/UserProfileScreen.kt`

Finding:

- User profile action sheet already contains a Boop action.
- The UI calls:

```kotlin
userProfileScreenModel.boop(
    userId = currentUser.id,
    boopData = selectedEmoji,
    successMessage = localeStrings.profileBoopSuccess,
)
```

Status: UI exists and opens the emoji selector before sending.

---

#### User profile model

Path:

`composeApp/src/commonMain/kotlin/presentation/screens/user/UserProfileScreenModel.kt`

Finding:

- Contains friend operations:
  - `sendFriendRequest`
  - `deleteFriendRequest`
  - `unfriend`
  - `acceptFriendRequest`
- Contains a working `boop(userId, boopData, successMessage)` implementation.
- Sends the request through `UsersApi.boop(...)`.

---

#### Notification model

Path:

`composeApp/src/commonMain/kotlin/network/api/notification/data/NotificationDataV2.kt`

Finding:

Notification already contains:

- `type`
- `details`
- `message`
- `senderUserId`

The existing fields should be enough for receiving boop information.

---

#### Boop notification resolver

Path:

`composeApp/src/commonMain/kotlin/presentation/screens/home/data/BoopNotificationResolver.kt`

Finding:

Already handles received boop notifications.

Current responsibility:

- Detect notifications where `type == boop`.
- Resolve sender information.
- Add display information.

Still needed:

- Parse emoji information from the notification payload.

The home model now keeps an initial notification snapshot, so opening the app does not replay historical Boops. Later WebSocket-triggered refreshes enqueue newly received Boops for an in-app dialog.

---

## Current implementation

- [x] Implement Boop API client (`network/api/boop/BoopApi.kt`).
- [x] Add Boop request data model (`network/api/boop/data/SendBoopData.kt`).
- [x] Verify and use the official endpoint: `POST /api/1/users/{userId}/boop`.
- [x] Add the Boop emoji selector to the user profile action sheet.
- [x] Pass `emojiId`, `emojiVersion`, and `inventoryItemId` through the request model.
- [x] Add a foreground in-app dialog for newly received Boops.

## Remaining work

- [ ] Confirm the exact default emoji string constants against a captured official request.
- [ ] Parse emoji information from received boop notifications.
- [ ] Add Android/iOS notification support for background boops.

## Design direction

First milestone:

`User Profile -> Boop button -> VRChat API -> Friend receives Boop`

Second milestone:

`Received Boop -> App overlay popup` (foreground implementation complete)

Third milestone:

`VRC+ inventory emoji selection -> custom emoji support`

Keep this file updated with discovered paths, APIs and implementation notes so future development sessions can resume without rediscovering the architecture.
