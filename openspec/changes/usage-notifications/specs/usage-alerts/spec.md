## ADDED Requirements

### Requirement: Threshold-based usage notifications

The system MUST evaluate usage thresholds after each successful poll and fire Android notifications when thresholds are crossed for the first time in a reset cycle.

#### Scenario: User crosses 75% on 5-hour window for the first time in current cycle
Given an account with 5h alerts enabled and 75% threshold configured
And current 5h utilization is 78%
And the 75% threshold has not been seen since the last 5h reset
When the usage worker completes a poll
Then a notification is posted to the "usage_warnings" channel
And the notification title contains the account name
And the notification body shows "5h usage at 78% (threshold: 75%)"
And the 75% threshold is marked as seen for the current reset cycle

#### Scenario: Same threshold already seen in current cycle — no duplicate
Given an account with 5h utilization at 80%
And the 75% threshold was already seen in this reset cycle (same resetsAt)
When the usage worker completes a poll
Then no notification is posted for the 75% threshold

#### Scenario: Window resets and usage climbs past threshold again
Given the 75% threshold was seen for the previous reset cycle
And the 5h window has since reset (resetsAt changed)
And current 5h utilization is now 76%
When the usage worker completes a poll
Then a new notification is posted for the 75% threshold
And the seen flag is updated with the new resetsAt

#### Scenario: Critical threshold fires heads-up notification
Given an account with 7d utilization at 92%
And the 90% threshold has not been seen in this cycle
When the usage worker completes a poll
Then a notification is posted to the "usage_critical" channel (IMPORTANCE_HIGH)
And the notification appears as a heads-up popup

#### Scenario: Multiple thresholds crossed simultaneously
Given an account jumped from 40% to 95% since last poll
And thresholds 50%, 75%, 90% are all configured and unseen
When the usage worker completes a poll
Then notifications are posted for 50%, 75%, and 90% thresholds
And each uses a deterministic notification ID based on account + window + threshold

#### Scenario: Alerts disabled for an account
Given an account with alerts toggle set to off
When the usage worker completes a poll
Then no threshold notifications are posted for that account regardless of utilization

#### Scenario: Custom threshold added by user
Given a user adds a custom threshold at 60% for the 7d window
When 7d utilization crosses 60%
Then a notification fires for the 60% threshold following the same dedup rules
