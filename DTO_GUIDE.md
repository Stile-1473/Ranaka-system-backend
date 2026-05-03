# DTO Guide

This guide explains the main request and response payloads in plain language.

## 1. What A DTO Is In This Project

A DTO is the shape of data that crosses the API boundary.

Simple way to think about it:

- entities are for the database
- DTOs are for the outside world

That separation matters because the frontend should receive a clean contract, not the raw JPA object graph.

## 2. Authentication DTOs

### Login request

File:
[LoginRequest.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/authentication/dto/request/LoginRequest.java:1)

Example:

```json
{
  "email": "requester1@ranaka.org",
  "password": "Password@123"
}
```

### Auth response

File:
[AuthResponse.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/authentication/dto/response/AuthResponse.java:1)

Example:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "requester1@ranaka.org",
  "message": "User logged in",
  "role": "REQUESTER"
}
```

Why it matters:

- the token is used for later API calls
- the role helps the frontend decide which dashboard to load first

## 3. Request Creation DTOs

### Create request DTO

File:
[CreateRequestDto.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/dto/request/CreateRequestDto.java:1)

This is what a requester sends when creating a draft.

Example:

```json
{
  "title": "Laptop replacements for legal officers",
  "description": "Replacement of aging laptops used for case preparation and court work.",
  "lineItems": [
    {
      "itemDescription": "Dell Latitude laptop",
      "quantity": 3,
      "unitCost": 1200.00,
      "unit": "PCS",
      "notes": "For officers handling litigation and mobile casework"
    }
  ],
  "departmentId": 3,
  "justification": "Existing devices are failing and delaying document preparation.",
  "priority": "HIGH",
  "requiredByDate": "2026-05-15"
}
```

Validation rules in plain English:

- title is required
- description is required
- at least one line item is required
- department is required
- justification is required
- priority is required
- required-by date must be in the future

### Line item request

File:
[LineItemRequest.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/dto/LineItemRequest.java:1)

This lets one procurement request contain several concrete cost items instead of one vague blob.

That is good for:

- clearer budgeting
- better comments during review
- more believable procurement records

## 4. Request Response DTOs

### Request detail response

File:
[RequestDetailResponseDto.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/dto/response/RequestDetailResponseDto.java:1)

This is the “full story” payload for a single request.

It includes:

- core request data
- line items
- requester info
- approval history
- comments
- attachments
- overdue markers

Example response idea:

```json
{
  "id": 12,
  "title": "Stationery packs for district legal clinics",
  "description": "Bulk stationery replenishment for field teams.",
  "estimatedCost": 1200.00,
  "departmentName": "Community Outreach",
  "priority": "HIGH",
  "status": "PENDING_ADMIN_RECOMMENDATION",
  "currentStage": "ADMIN_RECOMMENDATION",
  "requesterName": "Farai Mhlanga",
  "requesterEmail": "requester2@ranaka.org",
  "approvalHistory": [],
  "comments": [],
  "attachments": []
}
```

The key idea:
this DTO is shaped for a detail page, not just for a database save.

## 5. Approval Action DTOs

### Approval action request

File:
[ApprovalActionRequest.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/dto/request/ApprovalActionRequest.java:1)

Typical example:

```json
{
  "comment": "Budget line confirmed. Forwarding to GM."
}
```

The same request shape is reused for:

- recommend
- approve
- authorize
- reject
- return

The meaning changes based on the endpoint and current stage.

## 6. Dashboard DTOs

Example file:
[DashboardSummaryDto.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/dashboard/dto/DashboardSummaryDto.java:1)

This is the compact KPI payload for dashboard cards.

Example:

```json
{
  "totalRequests": 26,
  "pendingRequests": 9,
  "overdueRequests": 2,
  "completedRequests": 11,
  "rejectedRequests": 2,
  "returnedRequests": 2,
  "avgApprovalTimeHours": 37.5,
  "generatedDate": "2026-04-22"
}
```

This is intentionally lightweight because dashboards usually need fast summary data first.

## 7. Report DTOs

Example file:
[ReportFilterDto.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/report/dto/ReportFilterDto.java:1)

This DTO helps report endpoints accept filter criteria such as:

- date range
- department
- priority
- status
- overdue-only flag

Example query idea in business terms:

"Show me overdue high-priority requests for ICT and Systems for this month."

The response DTOs in the report package then shape the answer into report-friendly aggregates.

## 8. Settings DTOs

Example file:
[SystemSettingResponse.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/settings/dto/SystemSettingResponse.java:1)

This is used when the backend returns system configuration entries such as SLA settings.

Example:

```json
{
  "id": 3,
  "settingKey": "sla.gm.hours",
  "settingValue": "48",
  "description": "GM approval SLA in hours",
  "settingType": "NUMBER",
  "systemSetting": true
}
```

## 9. Why DTOs Matter For MVP Readiness

Clean DTOs help the MVP in three ways:

- the frontend gets predictable contracts
- validation rules are explicit at the edge
- internal entity changes do not automatically break the API

When a project reaches demo stage, this clarity becomes a huge advantage because frontend work, testing, and bug fixing all move faster.
