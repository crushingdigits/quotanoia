## Context

Quotanoia is an Android phone + watch widget that monitors Claude subscription usage across multiple accounts. It polls `claude.ai/api/organizations/{orgId}/usage` every 5 minutes via a WorkManager-based self-rescheduling worker. Usage data includes 5-hour and 7-day utilization windows with reset timestamps. The app already handles session key rotation, exponential backoff on errors, and syncs data to a paired Wear OS watch via MessageClient.

Currently, users must open the app or glance at the widget/tile to see their usage — there is no proactive alerting.

Stakeholder: single developer (personal POC), but designed for future multi-user release.

## Goals / Non-Goals

**Goals:**
- Notify users when usage crosses configurable thresholds (per account, per window)
- Alert on session expiration so users know monitoring has stopped
- Let users customize thresholds (add/modify/remove) with sensible defaults
- Avoid notification spam (once-per-threshold-per-reset-cycle dedup)
- Proper Android 13+ notification permission handling
- Non-disruptive onboarding (permission prompt only on explicit opt-in)

**Non-Goals:**
- Push notifications via a backend server (all local, poll-based)
- Notification scheduling independent of the existing WorkManager cycle
- Rich notification actions (snooze, quick-reply) — keep it simple
- Wear OS custom notification rendering — rely on phone-to-watch mirroring
- Rate limit prediction or forecasting ("you'll hit 90% in 2 hours")

## Decisions

### 1. Notification evaluation hook: inside UsageWorker

**Decision:** Evaluate thresholds in `UsageWorker.doWork()` immediately after `repository.refreshAll()`, before rescheduling.

**Rationale:** The worker already runs every 5 minutes and has fresh data. Adding a `NotificationEvaluator.evaluate(states)` call is the simplest integration point. No new scheduler, no new coroutine — just a function call in the existing cycle.

**Alternative considered:** Observe `UsageRepository.statesFlow` from `QuotanoiaApp` and react to emissions. Rejected because it adds complexity (another coroutine, lifecycle management) for no benefit — the worker IS the data source.

### 2. Threshold state persistence: SharedPreferences (not Room)

**Decision:** Store per-account notification preferences and "threshold seen" flags in a dedicated `NotificationPrefsStore` using SharedPreferences.

**Rationale:** The data is simple key-value pairs: `{accountId}_{window}_{threshold}_seen = true`, `{accountId}_alerts_enabled = true`, `{accountId}_{window}_thresholds = [50,75,90,100]`. SharedPreferences is fast, synchronous for reads, and doesn't require schema migrations. Room is overkill for ~20 key-value pairs per account.

**Alternative considered:** Room table. Rejected — adds migration complexity for simple boolean/list storage. The existing Room DB is for usage history, a different concern.

### 3. Threshold dedup: reset-cycle-aware flags

**Decision:** When a threshold fires, mark it as `seen` with the current reset timestamp. On next evaluation, only fire if: (a) threshold exceeded AND (b) either not seen, or the window's `resetsAt` changed since the flag was set.

**Rationale:** This handles the "usage drops after reset → climbs again → re-crosses threshold" case correctly. Each new reset cycle clears all thresholds for that window automatically by virtue of the `resetsAt` timestamp changing.

**Format:** `{accountId}_5h_75_seen_reset = "2026-04-04T05:00:00Z"` — if current `resetsAt` differs, the flag is stale and threshold can fire again.

### 4. Notification channels: three tiers

**Decision:**
- `usage_warnings` (IMPORTANCE_DEFAULT): 50%, 75% thresholds. Sound + vibration.
- `usage_critical` (IMPORTANCE_HIGH): 90%, 100% thresholds. Heads-up notification.
- `account_issues` (IMPORTANCE_HIGH): Session expired, persistent errors. Heads-up.

**Rationale:** Users can silence low-priority warnings while keeping critical alerts. Android's per-channel controls give users fine-grained power without app-side UI for "importance" settings.

### 5. Notification IDs: deterministic per account + threshold

**Decision:** Notification ID = hash of `"{accountId}_{window}_{threshold}"`. This means posting a new notification for the same threshold on the same account replaces the previous one (no spam).

**Rationale:** Android's `notify(id, notification)` replaces existing notifications with the same ID. Deterministic IDs prevent accumulation while allowing multiple distinct alerts (e.g., 5h:90% and 7d:75% are separate notifications).

### 6. Permission flow: lazy prompt on first toggle

**Decision:** No permission prompt on app launch or account creation. The account settings screen has a "Usage Alerts" section that's off by default. When the user first toggles alerts on, check for `POST_NOTIFICATIONS` permission. If not granted, show the system prompt. If permanently denied, show inline text with a "Grant in Settings" deep link.

**Rationale:** Requesting permissions at the moment of intent yields higher grant rates. Users who don't want notifications are never bothered. Follows Android's best practice of "contextual permission requests."

### 7. Preferences UI location: inline in account settings

**Decision:** Each account's settings screen gets a "Notifications" section with:
- Master toggle (on/off)
- "5-Hour Window" subsection: list of threshold chips (50, 75, 90, 100) with + button
- "7-Day Window" subsection: same
- Thresholds are tappable chips — tap to remove, + to add a custom value (number picker)

**Rationale:** Per-account granularity from day one. Co-locating with account settings avoids a separate "Notifications" screen that users have to discover. The chip-based UI is compact and touch-friendly.

### 8. Hint card: one-time nudge after first fetch

**Decision:** After the first successful usage fetch for any account, show a card on `AccountListScreen`: "Get alerted when usage is high → Set up notifications". Tapping navigates to account settings. Card has an X to dismiss. Dismissal persisted in SharedPreferences.

**Rationale:** Gentle discovery mechanism. Users who want notifications find them easily. Users who don't dismiss once and never see it again.

## Risks / Trade-offs

- **[Risk] Notification fatigue if user has many accounts with similar thresholds** → Mitigation: deterministic notification IDs prevent spam. Each unique (account, window, threshold) is one notification. Consider grouping in a future iteration.

- **[Risk] Worker not running (Doze mode, battery optimization)** → Mitigation: WorkManager handles Doze correctly with `CONNECTED` network constraint. Notifications may be delayed but will fire on next wake. This is acceptable for a usage monitor.

- **[Risk] Permission permanently denied** → Mitigation: Show inline "Grant in Settings" link with `Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)`. No repeated prompts.

- **[Trade-off] SharedPreferences for notification state vs Room** → Simpler but no query capability. Acceptable because we never query "all seen thresholds" — we only check per-account-per-window.

## Open Questions

None — all decisions were aligned with the user during the design discussion.
