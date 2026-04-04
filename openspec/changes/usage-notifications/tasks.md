## 1. Notification Channels & Permissions

- [ ] 1.1 Add `POST_NOTIFICATIONS` permission to AndroidManifest.xml
- [ ] 1.2 Create `NotificationChannelManager` object that registers three channels (usage_warnings, usage_critical, account_issues) with correct importance levels
- [ ] 1.3 Call channel registration in `QuotanoiaApp.onCreate()`
- [ ] 1.4 Create `NotificationPermissionHelper` utility for runtime permission check, request, and "denied permanently" detection with Settings deep link intent

## 2. Notification Preferences Storage

- [ ] 2.1 Create `NotificationPrefsStore` class using SharedPreferences: per-account alerts enabled toggle, per-window threshold lists, threshold-seen flags with resetsAt tracking, hint card dismissed flag
- [ ] 2.2 Add default thresholds (50, 75, 90, 100) applied when alerts are first enabled for an account
- [ ] 2.3 Implement `isThresholdSeen(accountId, window, threshold)` and `markThresholdSeen(accountId, window, threshold, resetsAt)` with reset-cycle awareness
- [ ] 2.4 Implement `clearStaleSeenFlags(accountId, window, currentResetsAt)` to auto-clear flags when resetsAt changes

## 3. Notification Evaluator (Core Logic)

- [ ] 3.1 Create `NotificationEvaluator` class that takes `List<AccountState>` and `NotificationPrefsStore`, evaluates all thresholds, returns a list of `PendingNotification` descriptors
- [ ] 3.2 Implement threshold crossing detection: compare current utilization against configured thresholds, filter by unseen-in-current-cycle
- [ ] 3.3 Route notifications to correct channel: ≤75% → usage_warnings, ≥90% → usage_critical
- [ ] 3.4 Generate deterministic notification IDs from hash of accountId + window + threshold
- [ ] 3.5 Handle session expiration: detect `AccountError.TokenExpired` and produce account_issues notification
- [ ] 3.6 Implement session-expiration dedup: only fire once per account while error persists, dismiss on successful poll

## 4. Notification Posting

- [ ] 4.1 Create `NotificationPoster` that takes `PendingNotification` descriptors and posts via `NotificationManagerCompat`
- [ ] 4.2 Build notifications with `NotificationCompat.Builder`: title (account name), body (usage text), channel, auto-cancel, tap intent (open app)
- [ ] 4.3 Check `POST_NOTIFICATIONS` permission before posting; skip silently if not granted
- [ ] 4.4 Dismiss session-expiration notifications when account's session key is updated

## 5. Integration with UsageWorker

- [ ] 5.1 Add `NotificationEvaluator.evaluate()` call in `UsageWorker.doWork()` after `repository.refreshAll()`
- [ ] 5.2 Pass evaluation results to `NotificationPoster`
- [ ] 5.3 Verify notifications fire correctly with debug logging

## 6. Notification Preferences UI

- [ ] 6.1 Add "Usage Alerts" section to account settings screen with master toggle
- [ ] 6.2 Implement permission request flow on first toggle (prompt or "Grant in Settings" link)
- [ ] 6.3 Add 5-Hour Window threshold chip row with add/remove capability
- [ ] 6.4 Add 7-Day Window threshold chip row with add/remove capability
- [ ] 6.5 Add "+" button that opens a number picker dialog (1-100) for custom thresholds
- [ ] 6.6 Wire UI to `NotificationPrefsStore` for persistence

## 7. Hint Card

- [ ] 7.1 Add one-time hint card to AccountListScreen: "Get alerted when usage is high → Set up notifications"
- [ ] 7.2 Implement dismiss button that persists dismissal in SharedPreferences
- [ ] 7.3 Show card only after first successful fetch and only if not previously dismissed

## 8. Testing

- [ ] 8.1 Unit test `NotificationEvaluator`: threshold crossing, dedup, reset-cycle awareness, session expiration
- [ ] 8.2 Unit test `NotificationPrefsStore`: threshold CRUD, seen flag logic, stale flag clearing
- [ ] 8.3 Verify build compiles for both app and wear modules
