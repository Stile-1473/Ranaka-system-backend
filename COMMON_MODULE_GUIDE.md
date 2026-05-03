# Common Module Guide

This guide explains the shared enums and exception classes that the rest of the backend leans on.

## 1. What This Layer Is For

Think of `common` as the shared language of the application.

- Enums define the official business vocabulary.
- Exceptions define the main failure types.
- The global exception handler turns failures into predictable API responses.

If the request workflow is the story, `common` gives us the words and grammar.

## 2. Shared Enums In Plain English

### `RequestStatus`

This tells us the current business state of a request.

Example:

- `DRAFT`: Lindiwe is still preparing her printer request.
- `PENDING_ADMIN_RECOMMENDATION`: it has been submitted and is waiting for Tariro.
- `PENDING_GM_APPROVAL`: the Admin stage is done, now Nyasha must act.
- `PENDING_CEO_AUTHORIZATION`: it is on the CEO desk.
- `RETURNED_FOR_CORRECTION`: the requester needs to fix something.
- `REJECTED`: the workflow has stopped.
- `AUTHORIZED` or `COMPLETED`: the request successfully reached the end.

### `WorkflowStage`

This tells us who is supposed to act next.

That matters because a request can be “pending” in a broad sense, but we still need to know whether it is waiting for Admin, GM, or CEO.

### `RequestPriority`

This is the urgency signal:

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

Example:
A case-management software renewal might be `CRITICAL`, while replacing decorative office items might be `LOW`.

### `ApprovalAction`

This records what someone actually did at a stage.

Examples:

- `RECOMMEND`
- `APPROVE`
- `AUTHORIZE`
- `REJECT`
- `RETURN_FOR_CORRECTION`

This becomes very important in approval history and audit views.

## 3. How Errors Become API Responses

The most important file here is [GlobalExceptionHandler.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/common/exception/GlobalExceptionHandler.java:1).

Its job is simple:

1. catch known exceptions
2. convert them into the right HTTP status
3. return a consistent JSON error payload

That means frontend code does not have to guess what an error response looks like.

## 4. Error Response Shape

The shared shape lives in [ApiErrorResponse.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/common/exception/ApiErrorResponse.java:1).

Typical response idea:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "requiredByDate": "Required by date must be in the future",
    "title": "Title is required"
  },
  "timestamp": "2026-04-22T10:15:30",
  "path": "/api/v1/requests"
}
```

Why this is useful:

- `code` is stable for frontend logic.
- `message` is readable for users.
- `details` carries field-level clues.
- `path` makes debugging easier.

## 5. Real Failure Examples

### Example: duplicate user email

If a System Admin tries to create another user with `admin@ranaka.org`, the backend can throw `EmailAlreadyExistsException`.

Expected result:

- HTTP `409`
- code like `EMAIL_ALREADY_EXISTS`

### Example: wrong stage action

If the GM tries to approve a request that is still waiting for Admin recommendation, the backend can raise an `IllegalStateException`.

Expected result:

- HTTP `409`
- code like `STATE_CONFLICT`

### Example: role violation

If a Requester calls `/api/v1/approvals/12/approve`, Spring Security can raise `AccessDeniedException`.

Expected result:

- HTTP `403`
- code like `ACCESS_DENIED`

## 6. Why This Matters For MVP Readiness

For an MVP demo, polished error handling matters more than people first think.

It helps with:

- predictable frontend integration
- clearer user feedback
- easier debugging during demos
- cleaner audit and support conversations

When the workflow blocks a user, the system should explain why in a stable, understandable way.
