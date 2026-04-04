## Why

Users have no way to know they're approaching or hitting their Claude subscription limits without manually opening the app. By the time they check, the quota may already be exhausted. Proactive notifications at customizable thresholds let users pace their usage or switch accounts before hitting a wall. Session expiration alerts ensure users aren't silently losing monitoring coverage.

## What Changes

- Add customizable usage threshold alerts per account, per window (5h and 7d independently)
- Default thresholds at 50%, 75%, 90%, 100% — users can add, modify, or remove thresholds
- Alert once per threshold per reset cycle (re-arm after window resets)
- Add session expiration alerts when API returns 401/403
- Add three Android notification channels: usage warnings, usage critical, account issues
- Add notification permission handling (runtime prompt on Android 13+)
- Add notification preferences UI in account settings screen
- Add one-time dismissable hint card on main screen after first successful fetch
- Persist notification preferences and threshold "seen" state

## Capabilities

### New Capabilities
- `usage-alerts`: Threshold-based usage notifications per account per window (5h/7d). Customizable thresholds, once-per-cycle dedup, channel-based priority routing (warnings vs critical).
- `session-alerts`: Reactive alerts when session key expires (401/403 from API). Directs user to re-enter credentials.
- `notification-preferences`: Per-account notification settings UI. Threshold management (add/edit/remove), per-window toggles, permission flow. Inline in account settings with first-enable permission prompt.
- `notification-channels`: Android notification channel setup (warnings/critical/account-issues) with appropriate importance levels.

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- **New dependencies**: None — uses built-in Android `NotificationCompat`, `NotificationChannel`, `NotificationManagerCompat`
- **Manifest**: Add `POST_NOTIFICATIONS` permission
- **UsageWorker**: After each poll, evaluate thresholds and fire notifications
- **UsageRepository**: Expose threshold crossing detection
- **AccountStore / new PreferencesStore**: Persist per-account notification prefs + threshold seen state
- **UI**: New notification preferences section in account settings. Hint card on main screen.
- **Database**: Optional — could use Room (already available) or SharedPreferences for threshold state. SharedPreferences is simpler for key-value flags.
