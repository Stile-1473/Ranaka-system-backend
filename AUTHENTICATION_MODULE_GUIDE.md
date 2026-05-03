# Authentication Module Guide

This guide explains the `authentication` part of the backend in everyday language.

## What This Module Does

Authentication answers:

- "Who are you?"
- "Can we trust this token?"
- "What role do you have?"

In this project, that matters because the same backend is used by:

- Requesters
- Admins
- GMs
- CEOs
- System Admins

Each of those roles should see different things and do different actions.

## Main Files

- [AuthController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/authentication/controller/AuthController.java:1)
  REST endpoints for login, register, current user, and password change.

- [AuthServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/authentication/serviceImp/AuthServiceImpl.java:1)
  Business logic for login and current-user lookup.

- [SecurityConfig.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/security/config/SecurityConfig.java:1)
  Defines what is public and what requires authentication.

## Human Example

Story:

- Farai opens the system in the morning.
- He enters his email and password.
- The backend checks whether the user exists, whether the password is correct, and whether the account is active.
- If all checks pass, the backend returns a JWT token.
- Farai’s frontend includes that token in later requests.

## Example Login Request

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "requester2@ranaka.org",
  "password": "Password@123"
}
```

Example idea of the response:

```json
{
  "token": "eyJhbGciOi...",
  "email": "requester2@ranaka.org",
  "message": "User logged in",
  "role": "REQUESTER"
}
```

## Example "Who Am I?" Request

Useful when the frontend has a token and wants to know who is currently signed in.

```http
GET /api/v1/auth/me
Authorization: Bearer <token>
```

## Example Password Change

```http
POST /api/v1/auth/change-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "currentPassword": "Password@123",
  "newPassword": "BetterPassword@456",
  "confirmPassword": "BetterPassword@456"
}
```

## Why This Matters

If auth is weak or confusing:

- users get access they should not have
- inactive accounts keep working
- the frontend cannot reliably know the current user
- auditing becomes less trustworthy

This module is the front door of the whole system, so simple and predictable behavior matters a lot.
