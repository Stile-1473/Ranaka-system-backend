# Ranaka Backend Humanized Guide

This guide explains the backend like a teammate would, not like a machine dump.
It is meant to help someone new understand what each part of the project does, why it exists, and how to use it with realistic examples.

## 1. How To Read This Project

Think of the backend as a set of conversations between different roles:

- A requester says: "I need something for work."
- An admin says: "I checked it and recommend it."
- A GM says: "I approve it."
- A CEO says: "Final authorization granted."
- The system says: "I recorded everything, notified everyone, and kept the audit trail."

The main code areas are:

- `authentication`: login, current user, password changes
- `security`: JWT validation, endpoint protection, WebSocket authentication
- `request`: create, edit, submit, approve, reject, return, attachments, comments
- `notification`: in-app notifications, email notifications, live WebSocket pushes
- `dashboard`: quick summaries and KPI-style cards
- `report`: analytical and export-style endpoints
- `audit`: who did what and when
- `settings`: SLA hours and priority configuration
- `bootstrap`: demo seed data so the system has something meaningful to show

Focused module guides:

- [AUTHENTICATION_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/AUTHENTICATION_MODULE_GUIDE.md:1)
- [COMMON_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/COMMON_MODULE_GUIDE.md:1)
- [BOOTSTRAP_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/BOOTSTRAP_MODULE_GUIDE.md:1)
- [DTO_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/DTO_GUIDE.md:1)
- [SECURITY_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/SECURITY_MODULE_GUIDE.md:1)
- [ENTITY_MODEL_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/ENTITY_MODEL_GUIDE.md:1)
- [REPOSITORY_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/REPOSITORY_GUIDE.md:1)
- [SERVICE_LAYER_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/SERVICE_LAYER_GUIDE.md:1)
- [SLA_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/SLA_GUIDE.md:1)
- [USER_FLOW_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/USER_FLOW_GUIDE.md:1)
- [FRONTEND_INTEGRATION_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/FRONTEND_INTEGRATION_GUIDE.md:1)
- [NOTIFICATION_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/NOTIFICATION_MODULE_GUIDE.md:1)
- [SETTINGS_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/SETTINGS_MODULE_GUIDE.md:1)
- [USER_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/USER_MODULE_GUIDE.md:1)
- [DEPARTMENT_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/DEPARTMENT_MODULE_GUIDE.md:1)
- [DASHBOARD_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/DASHBOARD_MODULE_GUIDE.md:1)
- [REQUEST_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/REQUEST_MODULE_GUIDE.md:1)
- [REPORT_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/REPORT_MODULE_GUIDE.md:1)
- [AUDIT_MODULE_GUIDE.md](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/AUDIT_MODULE_GUIDE.md:1)

## 2. What Happens In A Normal Request

Example story:

- Lindiwe from Legal Services creates a request for two office printers.
- She saves it as a draft while still checking quantities.
- She submits it when ready.
- Tariro, an Admin, recommends it.
- Nyasha, the GM, approves it.
- Rumbidzai, the CEO, authorizes it.
- The system marks it completed and notifies Lindiwe.

The important status/stage movement looks like this:

1. `DRAFT`
2. `PENDING_ADMIN_RECOMMENDATION`
3. `PENDING_GM_APPROVAL`
4. `PENDING_CEO_AUTHORIZATION`
5. `COMPLETED`

Alternative paths:

- `REJECTED`: the request stops
- `RETURNED_FOR_CORRECTION`: the requester fixes it and resubmits

## 3. The Most Important Files

These are the files new developers usually need first:

- [SecurityConfig.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/security/config/SecurityConfig.java:1)
  Explains which endpoints are public and how JWT-based security is wired.

- [WebSocketConfig.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/config/WebSocketConfig.java:1)
  Explains how live notifications move from the server to the browser.

- [RequestServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/serviceImpl/RequestServiceImpl.java:1)
  This is the heart of the business workflow.

- [NotificationServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/serviceImpl/NotificationServiceImpl.java:1)
  Saves notifications, tries email, then pushes real-time updates.

- [ReportServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/report/serviceImpl/ReportServiceImpl.java:1)
  Turns raw workflow data into report outputs.

## 4. Humanized API Examples

### 4.1 Login

Situation:
Farai opens the app in the morning and needs a token before he can do anything else.

Request:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "requester2@ranaka.org",
  "password": "Password@123"
}
```

Typical response idea:

```json
{
  "token": "eyJhbGciOi...",
  "email": "requester2@ranaka.org",
  "message": "User logged in",
  "role": "REQUESTER"
}
```

### 4.2 Create A Draft Request

Situation:
Farai needs stationery packs for district legal clinics.

```http
POST /api/v1/requests
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Stationery packs for district legal clinics",
  "description": "Bulk stationery replenishment for outreach teams and intake desks.",
  "departmentId": 4,
  "justification": "Field clinics are running low before the next outreach cycle.",
  "priority": "HIGH",
  "requiredByDate": "2026-05-10",
  "lineItems": [
    {
      "itemDescription": "A4 paper boxes",
      "quantity": 10,
      "unitCost": 12.50,
      "unit": "BOX",
      "notes": "For district intake desks"
    },
    {
      "itemDescription": "Blue pens",
      "quantity": 100,
      "unitCost": 0.45,
      "unit": "PCS",
      "notes": "For field officers and intake staff"
    }
  ]
}
```

How to think about it:

- The user is not sending a "perfect final request"; they are starting a working draft.
- The backend calculates total cost from line items.
- The request stays editable until submission.

### 4.3 Submit The Request

Situation:
Farai has finished editing and wants the approval chain to start.

```http
POST /api/v1/requests/12/submit
Authorization: Bearer <token>
```

What the backend does in plain English:

- checks that the request belongs to Farai
- checks that it is still draft/returned
- checks that required fields are present
- moves it to Admin stage
- writes an audit log
- notifies Admin users

### 4.4 Admin Recommendation

Situation:
Tariro reviews the request and sees it is valid.

```http
POST /api/v1/approvals/12/recommend
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "comment": "Budget line confirmed. Forwarding to GM."
}
```

### 4.5 GM Approval

Situation:
Nyasha checks business need and gives approval.

```http
POST /api/v1/approvals/12/approve
Authorization: Bearer <gm-token>
Content-Type: application/json

{
  "comment": "Approved for final executive authorization."
}
```

### 4.6 CEO Authorization

Situation:
Rumbidzai gives final sign-off.

```http
POST /api/v1/approvals/12/authorize
Authorization: Bearer <ceo-token>
Content-Type: application/json

{
  "comment": "Authorized. Proceed with procurement."
}
```

### 4.7 Return For Correction

Situation:
Tariro thinks the request is reasonable, but the cost breakdown is too vague.

```http
POST /api/v1/approvals/12/return
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "comment": "Please split the stationery costs by item and attach a clearer quote."
}
```

What this feels like to the requester:

- The request is not dead.
- It comes back with a reason.
- They fix it and resubmit instead of starting from zero.

## 5. Notifications: What “Real-Time” Means Here

When the backend creates a notification, it tries three things:

1. save it in the database
2. send email if possible
3. push it live through WebSocket/STOMP

That means the UI can show a toast or badge immediately, but the notification is also still available later in the database.

Example frontend subscription flow:

```javascript
const socket = new SockJS("http://localhost:5005/ws");
const client = Stomp.over(socket);

client.connect(
  {
    Authorization: `Bearer ${token}`,
  },
  () => {
    client.subscribe("/user/queue/notifications", (message) => {
      const notification = JSON.parse(message.body);
      console.log("New notification:", notification.title);
    });

    client.subscribe("/user/queue/notifications/unread-count", (message) => {
      console.log("Unread count:", Number(message.body));
    });
  }
);
```

Human example:

- Lindiwe is on the dashboard.
- The CEO authorizes her request.
- She does not need to refresh the page.
- A live notification appears instantly: "Your procurement request has been authorized and completed."

## 6. Report Examples

### Approval Times

Question:
"How long are we really taking at Admin, GM, and CEO levels?"

```http
GET /api/v1/reports/approval-times?startDate=2026-04-01&endDate=2026-04-30
Authorization: Bearer <system-admin-token>
```

### Bottlenecks

Question:
"Which stage is slowing us down the most?"

```http
GET /api/v1/reports/bottlenecks?startDate=2026-04-01&endDate=2026-04-30
Authorization: Bearer <system-admin-token>
```

### Department Usage

Question:
"Which department is submitting the most requests and how much value is flowing through them?"

```http
GET /api/v1/reports/department-usage
Authorization: Bearer <system-admin-token>
```

### Returns And Rejections

Question:
"Are we rejecting too many requests, or are departments sending low-quality submissions?"

```http
GET /api/v1/reports/returns-rejections?startDate=2026-04-01&endDate=2026-04-30
Authorization: Bearer <system-admin-token>
```

### Overdue Requests

Question:
"Show me everything that is currently stuck."

```http
GET /api/v1/reports/overdue-requests
Authorization: Bearer <system-admin-token>
```

### CSV Export

Question:
"I want to open it in Excel and send it to management."

```http
GET /api/v1/reports/export?type=department-usage&startDate=2026-04-01&endDate=2026-04-30
Authorization: Bearer <system-admin-token>
```

## 7. Settings Examples

Situation:
Operations leadership wants Admin actions to be due within 24 hours and reminder alerts 2 hours before breach.

```http
PUT /api/v1/settings/sla
Authorization: Bearer <system-admin-token>
Content-Type: application/json

{
  "adminSlaHours": 24,
  "gmSlaHours": 48,
  "ceoSlaHours": 72,
  "reminderHoursBeforeBreach": 2
}
```

## 8. Audit Log Example

Question:
"Who changed this request and when?"

```http
GET /api/v1/audit-logs/request/12
Authorization: Bearer <system-admin-token>
```

You should expect to see a trail like:

- request created
- draft updated
- submitted
- recommended by admin
- approved by GM
- authorized by CEO

## 9. Test Profile Example

The file [application-test.properties](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/test/resources/application-test.properties:1) uses H2 so tests can run without depending on your local MySQL instance.

That means:

- good for CI
- good for quick local verification
- less "it works only on my laptop"

## 10. Commenting Philosophy

Why I did not comment every single line:

- Comments on obvious code become visual noise very fast.
- They get outdated faster than the code itself.
- They make refactoring harder because every tiny rename becomes a documentation cleanup job.

What I did instead:

- added comments in the most confusing infrastructure files
- kept business-flow comments where the workflow is non-trivial
- added this guide with realistic examples someone can actually follow

That gives you something much closer to "humanized" documentation without turning the codebase into a wall of repetitive commentary.
