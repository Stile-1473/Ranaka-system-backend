# Entity Model Guide

This guide explains how the main database entities relate to each other in business terms.

## 1. The Big Idea

If you want to understand the backend data model quickly, start with this sentence:

"A requester creates a procurement request, the request contains line items, different approvers act on it over time, and the system records notifications plus audit history around it."

That sentence covers most of the important entities already.

## 2. The Core Aggregate: `ProcurementRequest`

The center of the system is [ProcurementRequest.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/entity/ProcurementRequest.java:1).

Think of it as the business record for one procurement need.

Example:

- title: "Laptop replacements for legal officers"
- requester: Lindiwe
- department: ICT and Systems
- priority: `HIGH`
- stage: `GM_APPROVAL`
- status: `PENDING_GM_APPROVAL`

This one entity answers the most important business question:

"What is being requested, by whom, and where is it in the workflow right now?"

## 3. Child Records Hanging Off A Request

### `RequestLineItem`

File:
[RequestLineItem.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/entity/RequestLineItem.java:1)

This breaks one request into concrete purchasable items.

Example:

- 3 laptops at 1200.00 each
- 2 network printers at 425.00 each

Why it matters:

- totals become more trustworthy
- approvers can review cost detail
- the request is less vague

### `RequestApproval`

File:
[RequestApproval.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/entity/RequestApproval.java:1)

This is the approval timeline.

Every meaningful workflow action becomes a separate history row.

Example history:

1. Admin recommended
2. GM approved
3. CEO authorized

That means the current request status and the approval history are different things:

- request status tells you where it is now
- approval rows tell you how it got there

### `RequestComment`

This stores discussion around the request.

Some comments can be internal, which helps approvers collaborate without exposing every note to the requester.

### `RequestAttachment`

This stores metadata about supporting files.

Example:

- vendor quote PDF
- item specification sheet
- budget breakdown spreadsheet

## 4. Supporting Master Data

### `User`

File:
[User.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/user/entity/User.java:1)

Users represent the people using the system.

Important fields:

- name
- email
- phone number
- role
- active/inactive flag

Important detail:
this entity also implements Spring Security `UserDetails`, which means the same record is used for authentication too.

### `Department`

File:
[Department.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/department/entity/Department.java:1)

Departments give the request organizational context.

Example:

- Legal Services
- Community Outreach
- Finance and Administration

### `SystemSetting`

File:
[SystemSetting.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/settings/entity/SystemSetting.java:1)

This is where configurable values live.

Example settings:

- admin SLA hours
- GM SLA hours
- CEO SLA hours
- priority configuration flags

## 5. Operational Side Entities

### `Notification`

File:
[Notification.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/notification/entity/Notification.java:1)

This stores messages the system sends to users.

Example:

- "Your request was returned for correction"
- "A request is waiting for your recommendation"
- "This item is now overdue"

Important detail:
notifications are both persisted and pushed live through WebSocket/STOMP.

### `AuditLog`

File:
[AuditLog.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/audit/entity/AuditLog.java:1)

This is the governance history for system activity.

Example:

- user logged in
- request submitted
- request approved
- user deactivated
- SLA setting changed

If notifications tell the user what happened, audit logs tell administrators who did what and when.

## 6. Relationship Picture In Words

The main relationships are:

- one `User` can create many `ProcurementRequest` records
- one `Department` can own many `ProcurementRequest` records
- one `ProcurementRequest` can have many `RequestLineItem` records
- one `ProcurementRequest` can have many `RequestApproval` records
- one `ProcurementRequest` can have many `RequestComment` records
- one `ProcurementRequest` can have many `RequestAttachment` records
- one `User` can receive many `Notification` records
- one `User` can generate many `AuditLog` records

That is the mental model most new developers need.

## 7. Why This Model Fits The MVP

For an MVP, this model is strong because it separates:

- current business state
- detailed workflow history
- operational messaging
- governance/audit records

That keeps the system understandable while still being rich enough for dashboards, reports, and compliance-style review.
