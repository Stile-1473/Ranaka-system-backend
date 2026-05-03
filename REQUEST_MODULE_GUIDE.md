# Request Module Guide

This guide explains the `request` module like you are onboarding a teammate who needs to change the workflow safely.

## What This Module Is Responsible For

This module handles the real life journey of a procurement request:

- create a draft
- edit the draft
- submit it into the workflow
- move it through Admin, GM, and CEO stages
- allow returns, rejections, comments, and attachments
- decide who can see or act on a request

If the system were a conversation, this module is the part that says:

- "Who is allowed to touch this request right now?"
- "What status should it move to next?"
- "Who needs to be notified?"
- "What should be written into the audit trail?"

## Files You’ll Usually Need

- [RequestServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/serviceImpl/RequestServiceImpl.java:1)
  The main business workflow brain.

- [RequestController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/controller/RequestController.java:1)
  Exposes the request endpoints.

- [ApprovalController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/controller/ApprovalController.java:1)
  Exposes the stage-action endpoints in a cleaner, approval-focused way.

- [ProcurementRequest.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/request/entity/ProcurementRequest.java:1)
  The main request entity with status, stage, requester, and totals.

## Human Story Example

Example:

- Lindiwe needs two printers for Legal Services.
- She creates a draft because she is still checking supplier pricing.
- Once the numbers are ready, she submits.
- Tariro recommends it.
- Nyasha approves it.
- Rumbidzai authorizes it.

That story becomes these state changes:

1. `DRAFT`
2. `PENDING_ADMIN_RECOMMENDATION`
3. `PENDING_GM_APPROVAL`
4. `PENDING_CEO_AUTHORIZATION`
5. `COMPLETED`

## Example Requests

### Create Draft

```http
POST /api/v1/requests
Authorization: Bearer <requester-token>
Content-Type: application/json

{
  "title": "Desktop printers for legal aid intake office",
  "description": "Procurement of two network printers for faster client intake processing.",
  "departmentId": 1,
  "justification": "Current printers fail often and slow intake work.",
  "priority": "MEDIUM",
  "requiredByDate": "2026-05-20",
  "lineItems": [
    {
      "itemDescription": "Network printer",
      "quantity": 2,
      "unitCost": 425.00,
      "unit": "PCS",
      "notes": "One for front desk, one for records desk"
    }
  ]
}
```

### Submit Draft

```http
POST /api/v1/requests/15/submit
Authorization: Bearer <requester-token>
```

What the backend is protecting here:

- only the requester can submit their own draft
- the draft must be complete enough to enter the approval chain
- after submission, the request is no longer freely editable

### Return For Correction

```http
POST /api/v1/approvals/15/return
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "comment": "Please attach a vendor quote and separate the printer cost from installation cost."
}
```

What that means in human terms:

- the request is not bad enough to reject forever
- the approver wants better information
- the requester gets another chance without starting over

## Things To Be Careful About

When changing this module, be especially careful about:

- access checks
- stage transitions
- comment requirements for reject/return
- resetting overdue flags when a request moves correctly
- keeping requester history visible after the request finishes
- writing audit logs for every important action

## Safe Mental Model For Changes

Before you change anything in the request flow, ask:

1. Who is allowed to do this action?
2. What status/stage should the request have before the action?
3. What status/stage should it have after the action?
4. Who should be notified?
5. What should be audited?

If all five answers are clear, your change is usually on solid ground.
