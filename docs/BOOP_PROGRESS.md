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
    successMessage = localeStrings.profileBoopSuccess,
)
```

Status: UI exists, backend implementation missing.

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
- Does not currently contain a working `boop()` implementation.

Needed changes:

- Inject Boop API.
- Implement `boop(userId, successMessage)`.

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

- Parse emoji information.
- Trigger popup/overlay display.

---

## Missing implementation

- [ ] Implement Boop API client.
- [ ] Add Boop request data model.
- [ ] Inject Boop API into `UserProfileScreenModel`.
- [ ] Implement `UserProfileScreenModel.boop()`.
- [ ] Verify official endpoint: `POST /users/{userId}/boop`.
- [ ] Parse emoji information from received boop notifications.
- [ ] Add in-app popup for received boops.
- [ ] Add Android/iOS notification support for background boops.

## Design direction

First milestone:

`User Profile -> Boop button -> VRChat API -> Friend receives Boop`

Second milestone:

`Received Boop -> App overlay popup`

Third milestone:

`Emoji picker -> custom/VRC+ emoji support`

Keep this file updated with discovered paths, APIs and implementation notes so future development sessions can resume without rediscovering the architecture.
