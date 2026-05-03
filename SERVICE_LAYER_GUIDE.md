# Service Layer Guide

This guide explains how the service layer turns raw API calls into real business behavior.

## 1. What The Service Layer Does

Controllers receive requests.
Repositories talk to the database.
The service layer is the part in the middle that decides the business rules.

In this backend, services are where the real workflow lives:

- who can do what
- when a request can move to the next stage
- when notifications are sent
- when audit logs are written
- when overdue logic should kick in

## 2. The Main Business Service

The heart of the workflow is [RequestServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/serviceImpl/RequestServiceImpl.java:1).

This is where most business actions happen:

- create draft
- update draft
- submit
- recommend
- approve
- authorize
- reject
- return
- add comments
- upload attachments

If you want to understand the product behavior, this is one of the first files to read.

## 3. Example Service Story: Submit Request

Business story:
Farai finishes a stationery request and clicks Submit.

What the service does:

1. load the request
2. verify Farai is allowed to act on it
3. confirm it is still in draft/returned state
4. validate required fields
5. move it to Admin stage
6. save it
7. notify Admin users
8. write audit history

That is a good example of why service code matters:
one button click triggers several coordinated business outcomes.

## 4. Example Service Story: Recommend, Approve, Authorize

These methods all follow the same general pattern:

1. verify the request exists
2. verify the current stage matches the action
3. verify the acting role is correct
4. create approval history
5. update request status/stage
6. notify the next audience
7. log the action

Example chain:

- Admin recommends
- GM approves
- CEO authorizes

By the end, the request is completed and the requester is notified.

## 5. Softer Vs Harder Outcomes

The service layer clearly separates:

### Return for correction

Meaning:
the request can still continue after fixes

What happens:

- return count increases
- stage goes back to draft
- requester is notified

### Reject

Meaning:
the workflow stops

What happens:

- status becomes rejected
- active stage is cleared
- requester is notified

This is a good example of business nuance living in the service layer, not the controller.

## 6. Notification Service

File:
[NotificationServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/serviceImpl/NotificationServiceImpl.java:1)

This service handles the full notification path:

1. save notification in the database
2. try to send email
3. push live update over WebSocket/STOMP

That means notifications are both durable and real-time.

## 7. Reminder And Overdue Service

File:
[ReminderSchedulerService.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/service/ReminderSchedulerService.java:1)

This service runs on a schedule and checks whether workflow items are:

- nearing SLA breach
- already overdue

Business purpose:

- remind approvers before a deadline is missed
- escalate when a request is officially late

## 8. Auth Service

File:
[AuthServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/authentication/serviceImp/AuthServiceImpl.java:1)

This service handles:

- login
- registration
- password changes
- current-user lookup

Important idea:
the auth service is not just about token creation, it also enforces account activity and logs login events.

## 9. Dashboard And Report Services

Files:

- [DashboardServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/dashboard/serviceImpl/DashboardServiceImpl.java:1)
- [ReportServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/report/serviceImpl/ReportServiceImpl.java:1)

These services answer management questions rather than single-request questions.

Examples:

- how many requests are overdue?
- which department submits most requests?
- what is the average approval time?
- where is the bottleneck?

That is why they feel more analytical than transactional.

## 10. Settings Service And SLA

File:
[SettingsServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/settings/serviceImpl/SettingsServiceImpl.java:1)

This service manages configurable values such as:

- SLA hours
- reminder timing
- priority timing defaults

Simple explanation:
it lets the business tune behavior without rewriting workflow code.

## 11. Why This Layer Matters For MVP

A project can have clean endpoints and still fail as a product if the business rules are weak.

Your service layer is where the MVP becomes a real workflow platform, because this is where the system actually:

- enforces stage order
- protects role boundaries
- records history
- drives reminders
- triggers notifications
- keeps behavior consistent

That is why service documentation is worth having for onboarding and future maintenance.
