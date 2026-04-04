## ADDED Requirements

### Requirement: Android notification channel setup

The app MUST create notification channels at startup with appropriate importance levels to enable user-facing notification controls.

#### Scenario: Channels created on app startup
Given the app is launched for the first time (or after an update)
When QuotanoiaApp.onCreate() runs
Then three notification channels are registered with NotificationManager:
And channel "usage_warnings" has IMPORTANCE_DEFAULT and name "Usage Warnings"
And channel "usage_critical" has IMPORTANCE_HIGH and name "Usage Critical"
And channel "account_issues" has IMPORTANCE_HIGH and name "Account Issues"

#### Scenario: Channels survive app updates
Given channels were created in a previous app version
When the app is updated and relaunched
Then existing channels are not duplicated (Android handles this)
And user's per-channel settings (if modified) are preserved

#### Scenario: Usage warnings channel behavior
Given a notification is posted to "usage_warnings"
Then it appears in the notification shade with default sound and vibration
And it does NOT appear as a heads-up popup

#### Scenario: Usage critical channel behavior
Given a notification is posted to "usage_critical"
Then it appears as a heads-up popup (high importance)
And it plays the default notification sound

#### Scenario: Account issues channel behavior
Given a notification is posted to "account_issues"
Then it appears as a heads-up popup (high importance)
And tapping it opens the app
