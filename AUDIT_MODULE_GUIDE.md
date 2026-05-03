# Audit Module Guide

This guide explains the `audit` module in practical terms.

## What Audit Means In This Project

Audit logging answers one simple question:

"If something important happened, can we prove who did it and when?"

That matters for:

- governance
- accountability
- troubleshooting
- leadership review
- compliance discussions

## What Gets Logged

Examples:

- login
- draft creation
- draft update
- request submission
- recommendation
- approval
- authorization
- rejection
- return for correction
- attachment upload
- settings changes

## Main Files

- [AuditLogController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/audit/controller/AuditLogController.java:1)
  Exposes the audit log endpoints.

- [AuditLogServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/audit/serviceImpl/AuditLogServiceImpl.java:1)
  Shapes raw audit rows into API-friendly responses.

## Example: Request Journey

Imagine this sequence:

- Lindiwe creates request `#15`
- Lindiwe updates the draft
- Lindiwe submits it
- Tariro recommends it
- Nyasha approves it
- Rumbidzai authorizes it

Audit logs let a system admin pull that story back out later.

Example call:

```http
GET /api/v1/audit-logs/request/15
Authorization: Bearer <system-admin-token>
```

## Example: User Activity Review

Question:
"Show me what Tariro did in the system this week."

```http
GET /api/v1/audit-logs/user/2?page=0&size=20
Authorization: Bearer <system-admin-token>
```

## Why This Matters

Without audit logs:

- people argue about what happened
- it is hard to debug workflow confusion
- leadership cannot trust the system fully

With audit logs:

- timeline questions become answerable
- role misuse becomes easier to spot
- support investigations become faster
