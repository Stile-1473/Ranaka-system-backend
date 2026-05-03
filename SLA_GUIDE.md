# SLA Guide

This guide explains what an SLA means in this procurement system and why it matters.

## 1. What SLA Means

`SLA` stands for `Service Level Agreement`.

In simple terms, it is the expected time window in which a stage should be completed.

In this backend, SLA is used as an internal time promise for workflow stages.

Example:

- Admin should recommend within `24 hours`
- GM should approve within `48 hours`
- CEO should authorize within `72 hours`

That does not mean the request is automatically approved when time runs out.
It means the system starts treating the request as late and responds accordingly.

## 2. Why SLA Exists In This Project

Without SLA timing, the workflow can become passive:

- requests sit too long
- nobody notices the delay quickly
- leadership cannot see bottlenecks clearly
- critical requests can quietly stall

With SLA timing, the system can:

- send reminders before a breach
- mark requests overdue after a breach
- surface overdue work on dashboards
- escalate delayed items to the right people

## 3. How SLA Works In Practice

Imagine Farai submits a request at `Monday 09:00`.

If it enters `ADMIN_RECOMMENDATION` and the Admin SLA is `24 hours`, then:

- due time is around `Tuesday 09:00`
- if reminder lead time is `2 hours`, the system may remind around `Tuesday 07:00`
- if no action happens by `Tuesday 09:00`, the request becomes overdue

That overdue status then affects:

- dashboards
- notifications
- escalation behavior

## 4. Stage-Based SLA Example

### Admin stage

Purpose:
make sure submitted requests get an initial review quickly

Example:
Lindiwe submits a printer request. Tariro should recommend, reject, or return it within the Admin SLA window.

### GM stage

Purpose:
prevent management approval from becoming a bottleneck

Example:
The Admin already recommended a laptop request. Now the GM should act within the GM SLA window.

### CEO stage

Purpose:
keep final authorization visible and time-bound

Example:
An urgent software renewal has already passed Admin and GM. The CEO should authorize within the final SLA window.

## 5. Reminder Vs Overdue

These are not the same thing.

### Reminder

A reminder happens before the deadline is breached.

Meaning:
"This item is getting close to being late."

### Overdue

Overdue happens after the deadline has passed.

Meaning:
"This item is now late and should be escalated."

## 6. Where SLA Is Configured

The settings layer stores SLA values.

Relevant code:

- [SettingsServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/settings/serviceImpl/SettingsServiceImpl.java:1)
- [ReminderSchedulerService.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/service/ReminderSchedulerService.java:1)

Typical configuration values include:

- `SLA_ADMIN_HOURS`
- `SLA_GM_HOURS`
- `SLA_CEO_HOURS`
- `SLA_REMINDER_HOURS`

## 7. Why SLA Matters For MVP Readiness

SLA support makes the MVP feel like a real operational system instead of a simple approval form.

It helps prove that the platform can:

- manage accountability
- reduce forgotten requests
- highlight delays
- support leadership visibility

That is exactly the kind of business value your procurement workflow is supposed to show.
