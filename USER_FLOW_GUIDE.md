# User Flow Guide

This guide explains the system from the user’s point of view.
For each major feature, it describes:

- what the feature is for
- how it works
- a human example

Think of this as the "how people actually use the product" document.

## 1. Login

### Purpose

To identify the user and load the right role-based experience.

### How it works

1. user enters email and password
2. backend verifies credentials
3. backend returns a JWT token
4. frontend uses that token for future API calls and WebSocket connection

### Example

Farai logs in as a Requester.
The app then shows his dashboard and his request list, not the Admin or CEO queues.

## 2. View Current User

### Purpose

To let the frontend know who is signed in and what role they have.

### How it works

The frontend calls the current-user endpoint after login or refresh, and the backend returns identity details.

### Example

The app checks whether the signed-in user is `REQUESTER`, `ADMIN`, `GM`, `CEO`, or `SYSTEM_ADMIN` and routes them to the correct dashboard.

## 3. Create Draft Request

### Purpose

To let a requester start a procurement request before it is ready for approval.

### How it works

1. requester enters title, description, line items, department, justification, priority, and required-by date
2. backend saves the request as `DRAFT`
3. requester can come back later and edit it

### Example

Lindiwe starts a request for two printers but is still waiting for a vendor quote.
She saves the draft instead of rushing a submission.

## 4. Edit Draft Request

### Purpose

To let a requester improve a request before the workflow starts.

### How it works

Only draft or returned requests can be edited.
Submitted or approved requests are no longer open for casual editing.

### Example

Farai changes the quantity of paper boxes from `8` to `10` after getting an updated stock count.

## 5. Submit Request

### Purpose

To formally send a request into the approval workflow.

### How it works

1. backend validates required fields
2. status changes from `DRAFT` to `PENDING_ADMIN_RECOMMENDATION`
3. current stage becomes `ADMIN_RECOMMENDATION`
4. Admin users get notified
5. audit log is written

### Example

Lindiwe finishes her printer request and clicks Submit.
Now it appears in the Admin queue and is no longer just a personal draft.

## 6. View My Requests

### Purpose

To help requesters track the requests they created.

### How it works

The backend returns only the signed-in requester’s records, usually with paging and filters.

### Example

Farai checks whether his stationery request is still pending, has been returned, or has already been approved.

## 7. View Request Details

### Purpose

To show the full story of one request.

### How it works

The detail view includes:

- main request data
- line items
- requester details
- approval history
- comments
- attachments
- overdue markers

### Example

Nyasha opens a laptop replacement request and sees the budget justification, line items, Admin comment, and attachment list all in one place.

## 8. Add Comments

### Purpose

To let users communicate around a request without editing the request itself.

### How it works

Users add a comment tied to a request.
Some comments may be internal, which is useful for approver-side discussion.

### Example

Admin Tariro adds:
"Please attach a clearer cost breakdown before final authorization."

## 9. Upload Attachments

### Purpose

To support requests with evidence or supporting documents.

### How it works

The backend records file metadata against the request, such as file name and content type.

### Example

Lindiwe uploads a vendor quote PDF for the printer request.

## 10. Admin Recommendation

### Purpose

To perform the first approval-stage review.

### How it works

1. Admin opens pending requests
2. Admin checks the details
3. Admin can recommend, reject, or return
4. if recommended, the request moves to GM

### Example

Tariro reviews Farai’s stationery request, confirms the budget line, and recommends it for GM approval.

## 11. GM Approval

### Purpose

To provide management approval before final executive authorization.

### How it works

Only a GM can act on items in the GM stage.
If approved, the request moves to CEO authorization.

### Example

Nyasha approves a laptop replacement request because the old devices are affecting court preparation and field work.

## 12. CEO Authorization

### Purpose

To give final approval and close the workflow successfully.

### How it works

1. CEO reviews the request
2. CEO authorizes it
3. status becomes completed
4. requester is notified

### Example

Rumbidzai authorizes an urgent software renewal so service continuity is not interrupted.

## 13. Return For Correction

### Purpose

To send a request back for improvement without killing it completely.

### How it works

1. approver gives a reason
2. request goes back to draft-style correction state
3. return count increases
4. requester updates and resubmits

### Example

An Admin returns a data bundle request because the cost breakdown is too vague.

## 14. Reject Request

### Purpose

To stop a request that should not continue.

### How it works

1. approver provides a rejection reason
2. status becomes `REJECTED`
3. active stage is cleared
4. requester is notified

### Example

A non-essential executive refreshments request is rejected because it does not fit current spending priorities.

## 15. Notifications

### Purpose

To keep users informed without making them manually chase every status change.

### How it works

Notifications are:

- stored in the database
- optionally emailed
- pushed live over WebSocket/STOMP

### Example

When Tariro recommends a request, Nyasha can immediately receive an in-app notification that a new approval is waiting.

## 16. Real-Time Notification Badge

### Purpose

To show changes immediately while the user is still on the page.

### How it works

The frontend subscribes to the user’s notification queue over WebSocket.
Unread counts update instantly when a new event arrives or when notifications are marked as read.

### Example

Nyasha is on the dashboard and suddenly sees the unread count change from `2` to `3` without refreshing the page.

## 17. Mark Notifications As Read

### Purpose

To keep the inbox clean and help users focus on what is still new.

### How it works

The user can mark one notification as read or clear all unread notifications.

### Example

Farai reads the message saying his request was returned for correction and marks it as read after opening the request.

## 18. SLA Reminders

### Purpose

To warn users before a request becomes late.

### How it works

The scheduler checks active workflow items against SLA settings and sends reminders shortly before the deadline.

### Example

If the Admin SLA is `24 hours`, the system may remind Admin users around hour `22` that a request is due soon.

## 19. Overdue Escalation

### Purpose

To make late items visible and harder to ignore.

### How it works

When an SLA deadline passes:

- request is marked overdue
- overdue timestamp is stored
- escalation notifications are sent
- dashboards reflect the overdue state

### Example

A request sitting too long in GM approval becomes overdue and is surfaced to GM, System Admin, and the requester.

## 20. Dashboard

### Purpose

To give each role a quick picture of what matters to them.

### How it works

Dashboards summarize:

- pending work
- completed work
- returned/rejected counts
- overdue items
- average timings

### Example

Farai’s dashboard answers:
"What is happening to my requests?"

System Admin’s dashboard answers:
"What is happening in the whole system?"

## 21. Reports

### Purpose

To answer leadership and operational questions, not just single-request questions.

### How it works

Reports aggregate request data across time, stage, department, priority, and outcome.

### Example

Leadership asks:
"Which stage causes the most delay this month?"

The report layer turns workflow history into that answer.

## 22. Audit Logs

### Purpose

To keep a trustworthy record of who did what and when.

### How it works

Important actions are stored with user, action type, description, entity reference, and timestamp.

### Example

The system records when:

- a user logs in
- a request is submitted
- a request is approved
- a setting is changed

## 23. User Management

### Purpose

To let System Admin manage who can access the platform and what role they have.

### How it works

System Admin can:

- create users
- update users
- activate/deactivate users
- assign roles

### Example

Shamiso creates a new Admin account for a colleague who will handle recommendation-stage reviews.

## 24. Department Management

### Purpose

To organize requests by business area.

### How it works

System Admin manages departments such as Legal Services, ICT and Systems, or Community Outreach.

### Example

A request for outreach equipment is linked to Community Outreach, helping reports show which department raises the most requests.

## 25. Settings Management

### Purpose

To let the organization tune operational rules without rewriting code.

### How it works

Settings can store values such as:

- SLA hours
- reminder timing
- priority timing defaults

### Example

Leadership decides the reminder should happen `4 hours` before breach instead of `2`, and the setting is updated.

## 26. Purpose Of The Whole User Flow

The overall purpose of this system is not only to collect requests.

It is to make procurement work:

- visible
- accountable
- time-bound
- auditable
- easier to manage

That is why the user flow is bigger than a simple form submission.
It is really a structured approval and accountability process.
