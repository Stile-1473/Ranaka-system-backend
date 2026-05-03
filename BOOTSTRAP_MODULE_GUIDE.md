# Bootstrap Module Guide

This guide explains the demo seed data that gets loaded when the app starts.

## 1. Why Bootstrap Data Exists

The `bootstrap` package is there so the system does not feel empty in development.

Without seed data, a teammate would need to:

- create departments manually
- create users manually
- log in as each role
- create several requests by hand
- push them through different stages just to test dashboards and reports

With bootstrap data, the app starts with realistic examples already in place.

## 2. The Main File

The important class is [DemoDataInitializer.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/bootstrap/DemoDataInitializer.java:1).

It runs at startup and does three main things:

1. create departments if they do not already exist
2. create demo users if they do not already exist
3. create sample requests if the request table is empty

That “if not already there” pattern is important because it keeps startup idempotent.

## 3. Seeded Departments

The current seed data creates realistic departments such as:

- Legal Services
- Finance and Administration
- ICT and Systems
- Community Outreach

These are helpful because reports and dashboards look more believable than if everything used generic names like `Department A`.

## 4. Seeded Users

The initializer creates one user for each major role:

- System Admin
- Admin
- GM
- CEO
- two Requesters

Example demo logins:

- `systemadmin@ranaka.org`
- `admin@ranaka.org`
- `gm@ranaka.org`
- `ceo@ranaka.org`
- `requester1@ranaka.org`
- `requester2@ranaka.org`

Shared demo password:

```text
Password@123
```

That makes it easy to test the workflow from different viewpoints without extra setup.

## 5. Seeded Request Scenarios

The best part of the seed data is that it demonstrates several workflow states at once.

### Draft request

Example story:
Lindiwe has prepared a printer request but has not submitted it yet.

Why this helps:

- create/edit screens have data to show
- draft filters can be tested immediately

### Pending Admin request

Example story:
Farai submitted stationery replenishment and it is waiting for Admin recommendation.

Why this helps:

- Admin queue endpoints can be checked quickly

### Pending GM request

Example story:
The Admin has already recommended a laptop replacement request, so now it sits in the GM queue.

Why this helps:

- approval history can show that one stage is already complete

### Pending CEO request

Example story:
An office seating request already passed Admin and GM, and is now ready for final authorization.

Why this helps:

- CEO dashboards and final-stage endpoints have something real to render

### Returned request

Example story:
An internet bundle request came back because the cost breakdown was not detailed enough.

Why this helps:

- the system can demonstrate correction and resubmission flows

### Rejected request

Example story:
A non-essential hospitality request was rejected.

Why this helps:

- rejection reporting and audit timelines become testable

### Authorized/completed request

Example story:
A case-management software renewal completed the full approval chain.

Why this helps:

- completion metrics and turnaround reporting work better with actual closed requests

### Overdue request

Example story:
A backup power unit request is still waiting in GM approval after the expected action window.

Why this helps:

- overdue dashboards, reminders, and escalation logic have realistic data

## 6. Attachments, Comments, And History

The bootstrap data does not only create top-level requests.
It also creates:

- approval history entries
- comments
- at least one attachment

That is important because request detail pages are much more useful when they show the full story, not just a title and status.

## 7. Why This Matters For MVP Demos

For MVP demos, bootstrap data saves a lot of time and stress.

It means you can immediately show:

- different queues by role
- status transitions
- realistic analytics
- audit and history views
- attachment and comment lists

In short, bootstrap data turns a blank system into a believable working product.
