# Repository Guide

This guide explains how the repository layer is being used in the backend and what the main query patterns mean in real life.

## 1. What Repositories Do Here

Repositories are the database access layer.

In this project they mainly do three jobs:

- simple CRUD through `JpaRepository`
- common finder methods derived from method names
- a few focused custom queries for dashboards, reports, reminders, and audit views

This is a good fit for the project because the query needs are meaningful but not overly exotic.

## 2. The Most Important Repository

The most central data access file is [ProcurementRequestRepository.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/repository/ProcurementRequestRepository.java:1).

Why it matters:

- request lists drive almost every screen
- workflow queues depend on request state
- dashboards and reminders depend on request counts and filters

## 3. Common Query Patterns In Human Terms

### Find requests by requester

Example method:

```java
findByRequesterId(...)
```

Business meaning:

"Show Farai only the requests he created."

### Find requests by status

Example method:

```java
findByStatus(...)
```

Business meaning:

"Show all returned requests" or "show all completed requests."

### Find requests by current stage

Example method:

```java
findByCurrentStage(...)
```

Business meaning:

"Show everything waiting for GM right now."

That is especially useful for queue screens.

### Find overdue requests

Example method:

```java
findOverdueRequests(...)
```

Business meaning:

"Which active workflow items have passed their SLA cutoff and need escalation?"

This is more operational than user-facing, because schedulers and dashboard summaries depend on it.

## 4. Notification Repository Patterns

File:
[NotificationRepository.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/repository/NotificationRepository.java:1)

Key ideas:

- fetch newest notifications first
- count unread notifications quickly
- bulk mark everything as read
- avoid duplicate reminders for the same event

Real example:

If Admin Tariro already got an overdue reminder for request `42`, the duplicate-check query can stop the system from sending the exact same notification repeatedly.

## 5. Audit Repository Patterns

File:
[AuditLogRepository.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/audit/repository/AuditLogRepository.java:1)

These queries support governance questions like:

- what did this user do recently?
- what happened to this request?
- what actions happened this week?

That is a good reminder that audit repositories serve administrators and compliance review, not just the main workflow UI.

## 6. User Repository Patterns

File:
[UserRepository.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/user/repository/UserRepository.java:1)

Common examples:

- `findByEmail(...)`
  Used for login and identity lookup.

- `existsByEmail(...)`
  Used for uniqueness validation before creating or updating users.

- `findByRole(...)`
  Useful when the system needs to notify all Admins, all GMs, or all CEOs.

This repository is a good example of one table serving both business admin features and security features.

## 7. Why There Are Few Custom Queries

A good sign in this codebase is that many repository methods are still readable derived queries.

That keeps things simple:

- less boilerplate
- less custom JPQL to maintain
- easier onboarding for newer Spring developers

Custom queries only show up where they actually add value, such as:

- date ranges
- aggregate counts
- sorted priority queues
- bulk updates

## 8. A Note On Mappers

At the moment, the project does not really have a dedicated mapper package in active use.

That means DTO conversion is mostly being handled in service code.

That is acceptable for an MVP, but it also means one possible future cleanup would be:

- introduce explicit mapper classes
- centralize entity-to-DTO conversion
- reduce repeated mapping logic in services

It is not a blocker for the MVP, but it is a good refactor candidate later.

## 9. Why The Repository Layer Is MVP-Friendly

This repository layer is strong enough for the MVP because it supports:

- role-based request queues
- notifications and unread counts
- audit lookups
- dashboard metrics
- report filtering

In other words, it is not just saving data; it already supports the actual operational questions the product needs to answer.
