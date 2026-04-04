## ADDED Requirements

### Requirement: Session expiration notifications

The system MUST alert the user when an account's session key has expired, preventing further usage monitoring until the user provides a new key.

#### Scenario: API returns 401 indicating expired session
Given an account with a valid session key that has since expired
When the usage worker polls and receives a 401 response
Then a notification is posted to the "account_issues" channel (IMPORTANCE_HIGH)
And the notification title shows the account name
And the notification body shows "Session expired — tap to update"
And tapping the notification opens the app to that account's settings

#### Scenario: API returns 403 indicating expired session
Given an account with session key returning 403
When the usage worker polls and receives a 403 response
Then a notification is posted to the "account_issues" channel
And the behavior is identical to the 401 scenario

#### Scenario: Session expiration notification not repeated while error persists
Given a session expired notification has already been posted for an account
And the session key has not been updated
When subsequent polls continue to return 401/403
Then no additional notifications are posted (same notification ID reused)

#### Scenario: Session renewed — error notification cleared
Given a session expired notification was previously posted
When the user updates the session key and the next poll succeeds
Then the session expired notification is dismissed programmatically
