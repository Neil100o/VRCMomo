# VRCMomo Boop Progress

## Current findings

### Existing code

- User profile UI already contains a Boop action.
- The UI calls `userProfileScreenModel.boop(userId, successMessage)`.
- Existing notification support includes Boop-related handling:
  - `NotificationType` supports boop.
  - `BoopNotificationResolver` exists for received boop notifications.

## Missing implementation

The following parts still need to be completed:

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

Keep this file updated so future development sessions can resume without re-discovering the architecture.
