# Settings Module Guide

This guide explains the `settings` module in practical terms.

## What This Module Does

Settings answer questions like:

- "How many hours should Admin have before a request is overdue?"
- "How many hours before the system sends a reminder?"
- "How should priority-related SLA values be stored?"

This module gives the system admin a controlled place to change operational behavior without rewriting code.

## Main Files

- [SettingsController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/settings/controller/SettingsController.java:1)
  REST endpoints for reading and updating settings.

- [SettingsServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/settings/serviceImpl/SettingsServiceImpl.java:1)
  Core logic for fetching defaults, updating values, and auditing changes.

##  Example

Story:

- Operations leadership decides Admin should have 24 hours, GM 48 hours, and CEO 72 hours.
- They also want reminders 2 hours before breach.
- The System Admin updates those values through the settings API.
- The scheduler then starts using the new values automatically.

## Example SLA Update

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

## Example Priority Configuration Update

```http
PUT /api/v1/settings/priorities
Authorization: Bearer <system-admin-token>
Content-Type: application/json

{
  "criticalSlaHours": 4,
  "highSlaHours": 8,
  "mediumSlaHours": 24,
  "lowSlaHours": 48
}
```

## Example Workflow Endpoint

Version 1 has a fixed workflow, so this endpoint is mostly descriptive:

```http
GET /api/v1/settings/workflow
Authorization: Bearer <system-admin-token>
```

That tells the frontend or admin user:

- what stages exist
- that stage skipping is not allowed
- that the final outcome is `COMPLETED`

## Why This Matters

Without a settings module:

- every operational change becomes a code change
- small policy updates require deployments
- auditability of config changes becomes weak

With a settings module:

- business rules become easier to tune
- changes are auditable
- reminders and overdue behavior can evolve safely
