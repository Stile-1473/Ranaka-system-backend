# Dashboard Module Guide

This guide explains the `dashboard` module in plain language.

## What This Module Does

Dashboards answer:

- "What should this user notice first when they log in?"
- "How many requests are waiting?"
- "How many are overdue?"
- "What patterns are visible across the system?"

This module does not create data.
It summarizes existing workflow data into quick views that are easier to act on.

## Main Files

- [DashboardController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/dashboard/controller/DashboardController.java:1)
  Exposes dashboard summary and analytics endpoints.

- [DashboardServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/dashboard/serviceImpl/DashboardServiceImpl.java:1)
  Builds the actual counts, distributions, and trend data.

## Human Example

When different roles log in, they should care about different things:

- Requester:
  "How many of my requests are pending, returned, approved, or rejected?"

- Admin:
  "How many requests are waiting for recommendation right now?"

- GM:
  "What is waiting for approval and which ones are urgent or overdue?"

- CEO:
  "Which requests need final authorization, especially expensive or critical ones?"

- System Admin:
  "What is happening across the whole system?"

## Example Summary Call

```http
GET /api/v1/dashboard/summary
Authorization: Bearer <token>
```

The response changes based on the signed-in user’s role.

## Example Trends Call

```http
GET /api/v1/dashboard/request-trends?startDate=2026-04-01&endDate=2026-04-30
Authorization: Bearer <token>
```

This is useful for charts like:

- submitted per day
- completed per day
- rejected per day
- returned per day

## Why This Matters

Without dashboards:

- users spend too much time hunting for important information
- overdue work stays hidden longer
- leadership has poor visibility

With dashboards:

- the system becomes action-oriented
- queues become easier to manage
- decision-makers get quicker context
