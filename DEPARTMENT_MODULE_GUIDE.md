# Department Module Guide

This guide explains the `department` module in a teammate-friendly way.

## What This Module Does

Departments help the system answer:

- "Which team owns this request?"
- "Which department is creating the most procurement workload?"
- "How should requests be grouped in reports and dashboards?"

This module is simple, but it matters because requests depend on it.

## Main Files

- [DepartmentController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/department/controller/DepartmentController.java:1)
  Endpoints for creating, updating, listing, and deleting departments.

- [DepartmentServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/department/serviceImpl/DepartmentServiceImpl.java:1)
  Handles validation and department-code generation.

## Human Example

Story:

- The organisation adds a new operational team called "Community Outreach".
- The System Admin creates the department.
- The service generates a stable department code.
- Requesters can now attach requests to that department.
- Reports and dashboards can group requests under that department later.

## Example Create Department

```http
POST /api/v1/departments
Authorization: Bearer <system-admin-token>
Content-Type: application/json

{
  "name": "Community Outreach",
  "description": "Coordinates mobile clinics, awareness campaigns, and outreach work."
}
```

## Example Get Active Departments

```http
GET /api/v1/departments/active
Authorization: Bearer <token>
```

This is useful for frontend dropdowns like:

- "Select department"
- "Which team is requesting this item?"

## Why This Matters

If departments are inconsistent:

- requests get misclassified
- reports become misleading
- dashboards lose business meaning

This module keeps that reference data clean and predictable.
