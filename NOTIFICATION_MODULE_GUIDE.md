# Notification Module Guide

This guide explains the `notification` module in plain, practical terms.

## What This Module Does

Notifications answer:

- "Who should know about this workflow event?"
- "Should they see it in the app?"
- "Should they get an email?"
- "Should the UI update immediately?"

In this project, notifications are not just nice extras.
They are part of how the workflow stays visible and responsive.

## Main Files

- [NotificationController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/controller/NotificationController.java:1)
  Endpoints for listing notifications, reading them, and counting unread items.

- [NotificationServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/serviceImpl/NotificationServiceImpl.java:1)
  Saves notifications, attempts email delivery, and pushes WebSocket updates.

- [ReminderSchedulerService.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/service/ReminderSchedulerService.java:1)
  Checks SLA timing and creates reminder or overdue notifications automatically.

- [WebSocketConfig.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/config/WebSocketConfig.java:1)
  WebSocket/STOMP setup for live updates.

## Human Example

Story:

- Lindiwe submits a request.
- Tariro, an Admin, should know immediately.
- The backend stores a notification, tries to email Tariro, and pushes a live message to the UI.

Later:

- if Tariro ignores the request too long, the reminder scheduler notices the SLA window getting close
- the system sends a reminder
- if it passes the SLA, the system marks it overdue and escalates

## Example Frontend Behavior

The frontend can:

- show a bell icon with unread count
- show a toast when a new notification arrives
- mark an item read when the user opens it

Example subscriptions:

```javascript
client.subscribe("/user/queue/notifications", onNotification);
client.subscribe("/user/queue/notifications/unread-count", onUnreadCount);
```

## Example API Calls

### List My Notifications

```http
GET /api/v1/notifications/my?page=0&size=10
Authorization: Bearer <token>
```

### Mark One As Read

```http
PATCH /api/v1/notifications/45/read
Authorization: Bearer <token>
```

### Mark All As Read

```http
PATCH /api/v1/notifications/read-all
Authorization: Bearer <token>
```

### Get Unread Count

```http
GET /api/v1/notifications/unread-count
Authorization: Bearer <token>
```

## Why This Matters

Without notifications:

- requests disappear into silence
- approvers forget tasks
- requesters keep asking for manual updates

With notifications:

- workflow movement becomes visible
- follow-up pressure reduces
- the system feels alive instead of passive
