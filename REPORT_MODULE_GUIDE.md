# Report Module Guide

This guide explains the `report` module in plain language.

## What This Module Does

The report module answers leadership questions like:

- "How long are approvals taking?"
- "Which stage causes the most delay?"
- "Which departments submit the most requests?"
- "How many requests are returned or rejected?"
- "Which requests are currently overdue?"

It does not create workflow data.
It reads data that already exists in requests and approval history, then turns that into useful summaries.

## Main File

- [ReportServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/report/serviceImpl/ReportServiceImpl.java:1)
  This is where the report math and export logic lives.

- [ReportController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/report/controller/ReportController.java:1)
  This exposes the report endpoints.

## Example Questions And Endpoints

### Approval Times

Question:
"For April 2026, how long did we spend on Admin, GM, and CEO steps on average?"

```http
GET /api/v1/reports/approval-times?startDate=2026-04-01&endDate=2026-04-30
Authorization: Bearer <token>
```

### Bottlenecks

Question:
"Which stage is the slowest right now?"

```http
GET /api/v1/reports/bottlenecks
Authorization: Bearer <token>
```

### Department Usage

Question:
"Which department is creating the most work and how much cost is attached to it?"

```http
GET /api/v1/reports/department-usage
Authorization: Bearer <token>
```

### Returns And Rejections

Question:
"Are requests coming in with poor quality or getting blocked later?"

```http
GET /api/v1/reports/returns-rejections
Authorization: Bearer <token>
```

### Overdue Requests

Question:
"Show me everything that is currently stuck."

```http
GET /api/v1/reports/overdue-requests
Authorization: Bearer <token>
```

### CSV Export

Question:
"I need to send this to management in Excel."

```http
GET /api/v1/reports/export?type=department-usage&startDate=2026-04-01&endDate=2026-04-30
Authorization: Bearer <token>
```

## How To Think About The Calculations

Approval time example:

- request submitted at 09:00 Monday
- Admin recommends at 13:00 Monday
- GM approves at 10:00 Tuesday
- CEO authorizes at 15:00 Wednesday

That roughly becomes:

- Admin step: 4 hours
- GM step: 21 hours
- CEO step: 29 hours
- Overall workflow: from first submission to final completion

## Important Design Choice

The report module uses current request records plus approval history.
That matters because reports should reflect what really happened, not what we assume happened.

If you change workflow rules later, the report module should usually be updated alongside them.
