## ADDED Requirements

### Requirement: Per-account notification preferences UI

The account settings screen MUST include a notification preferences section allowing users to configure threshold alerts per account and per usage window.

#### Scenario: User enables notifications for an account
Given the user is on the account settings screen
And the "Usage Alerts" section shows a master toggle in the off position
When the user taps the toggle to enable alerts
And the POST_NOTIFICATIONS permission has not been granted
Then the system permission dialog is shown
And if granted, the toggle turns on and threshold options become visible

#### Scenario: Permission previously granted — no prompt
Given POST_NOTIFICATIONS permission was already granted
When the user taps the alerts toggle to enable
Then the toggle turns on immediately with no permission dialog

#### Scenario: Permission permanently denied
Given the user previously denied POST_NOTIFICATIONS with "Don't ask again"
When the user taps the alerts toggle
Then inline text appears: "Notifications are blocked. Grant in Settings →"
And tapping the link opens Android's app notification settings

#### Scenario: Default thresholds shown on first enable
Given the user just enabled alerts for an account
Then the 5-Hour Window section shows threshold chips: 50%, 75%, 90%, 100%
And the 7-Day Window section shows threshold chips: 50%, 75%, 90%, 100%
And all thresholds are active by default

#### Scenario: User removes a threshold
Given the notification preferences are visible with 50%, 75%, 90%, 100% chips
When the user taps the 50% chip
Then the 50% threshold is removed from the list
And subsequent polls will not fire notifications for the 50% threshold

#### Scenario: User adds a custom threshold
Given the notification preferences are visible
When the user taps the "+" button in the 5-Hour Window section
Then a number picker dialog appears (range 1-100)
When the user selects 60 and confirms
Then a 60% chip appears in the threshold list
And subsequent polls will evaluate the 60% threshold

#### Scenario: Preferences persist across app restarts
Given the user configured custom thresholds (removed 50%, added 60%)
When the app is killed and restarted
Then the account settings screen shows the saved configuration (60%, 75%, 90%, 100%)

### Requirement: One-time hint card for notification discovery

The app MUST show a one-time dismissable hint card to guide users toward notification setup after their first successful usage fetch.

#### Scenario: Hint card shown after first successful fetch
Given the user has just added their first account
And the first usage fetch succeeded
And the hint card has never been dismissed
When the user views the account list screen
Then a card appears: "Get alerted when usage is high → Set up notifications"

#### Scenario: Hint card dismissed permanently
Given the hint card is visible
When the user taps the dismiss (X) button
Then the card disappears
And it never appears again in future sessions
