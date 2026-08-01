# VRCMomo code standards

These standards adapt Kotlin, Android and Compose Multiplatform conventions to this repository. They are intentionally practical: follow them for changed code; avoid large no-behavior-change rewrites while preparing a test build.

## Review order

Every non-trivial change is checked in this order:

1. **Correctness** — all state transitions and empty/unknown inputs behave safely.
2. **Privacy and authorization** — no secret data leaves the device; owner-only actions verify ownership.
3. **Persistence compatibility** — saved user history survives upgrades.
4. **Concurrency and lifecycle** — no blocking UI work, leaked jobs or unhandled cancellation.
5. **Maintainability** — narrow responsibilities, clear names, no duplicated policy.
6. **UI and locale consistency** — loading/error/empty states and Chinese strings are present.

## Kotlin and shared KMP code

- Prefer immutable `val` values and `data class` DTOs/state.
- Avoid `!!`. Use nullability, early return, `require`/`check`, or a recoverable error path.
- Use `when` for finite external states and keep an `else`/unknown path when the remote API can add values.
- Keep public names descriptive. Use `Id` in names consistently (`userId`, `avatarId`); do not use unexplained abbreviations.
- Put reusable constants next to their owning feature, not in an unrelated global utility.
- Keep a function focused. If it combines parsing, policy, persistence and UI publication, extract private helpers.
- `commonMain` must only depend on common APIs. Put platform code under `androidMain`, `iosMain` or `desktopMain` behind an abstraction where needed.

## Coroutines, files and network

- Use a lifecycle-owned `CoroutineScope`; do not create unbounded global work for screen actions.
- File and network operations run on an IO-safe dispatcher. Do not use blocking calls from a Composable body.
- Do not swallow cancellation in broad `runCatching`/`catch` blocks.
- Treat VRChat/VRCX fields as untrusted: tolerate missing, null, unknown and legacy values.
- Bound imported file sizes and validate formats before persistence.
- User-facing errors should be short and actionable; diagnostic details belong in safe local logs, never in a toast/dialog by default.

## Compose UI

- ScreenModel/service owns requests and mutable feature state; composables receive state and callbacks.
- State declaration → effects → UI is the preferred order inside a composable.
- Use `LazyColumn`/`LazyRow` for unbounded lists and stable keys when available.
- Use `remember`, `derivedStateOf` and stable parameters to avoid needless recomposition, but do not add memoization without a measured reason.
- New UI must cover loading, empty and error states when data is asynchronous.
- Do not hard-code user-visible copy in a screen. Add a `LocaleStrings` property and at minimum a Simplified Chinese implementation.

## Persistence and migrations

- Account-specific data uses account-specific filenames/keys.
- Saved models are append-compatible: add fields with defaults; retain legacy fields while a migration is needed.
- Raise the schema version and add one explicit migration branch per schema step.
- Preserve user data before a structural migration; never silently replace a newer schema with an older build.
- Import is merge-only and idempotent: use a stable source-event key and never duplicate imported totals or timeline events.

## Test and commit checklist

- Add a focused test for changed parsing, state transition or migration behavior.
- Run the focused test before the broader Android test suite.
- Run `:composeApp:assembleDebug` before publishing an APK.
- Run `git diff --check` before commit.
- Keep commits focused and describe the behavior, for example: `fix: preserve activity cache schema migration`.

