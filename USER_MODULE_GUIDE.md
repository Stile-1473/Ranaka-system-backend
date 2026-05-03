# User Module Guide

This guide explains the `user` module in plain, practical language.

## What This Module Does

The user module is where system-admin-level account management happens.

It answers questions like:

- "How do we create a new requester, admin, GM, or CEO account?"
- "How do we deactivate someone who should no longer log in?"
- "How do we change or validate user details safely?"

This is different from login/authentication.
Authentication proves who a person is right now.
The user module manages the account records themselves.

## Main Files

- [UserController.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/user/controller/UserController.java:1)
  Endpoints for creating, updating, activating, deactivating, and listing users.

- [UserServiceImpl.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/user/serviceImpl/UserServiceImpl.java:1)
  Validation and business logic for user management.

## Human Example

Story:

- A new requester joins the organisation.
- The System Admin creates their account with name, email, phone, role, and password.
- Later, that person moves to a different responsibility.
- The System Admin updates the role.
- If they leave the organisation, the System Admin deactivates the account instead of deleting history.

## Example Create User

```http
POST /api/v1/users
Authorization: Bearer <system-admin-token>
Content-Type: application/json

{
  "firstName": "Lindiwe",
  "lastName": "Sibanda",
  "email": "lindiwe.sibanda@ranaka.org",
  "phoneNumber": "+263771123456",
  "password": "SecurePassword@123",
  "role": "REQUESTER"
}
```

## Example Deactivate User

```http
PATCH /api/v1/users/8/deactivate
Authorization: Bearer <system-admin-token>
```

Why deactivate instead of delete:

- old request history still matters
- audit logs still matter
- the account should stop working without erasing business records

## Why This Matters

If user management is sloppy:

- wrong people get access
- old accounts remain active
- role-based workflow breaks down

This module protects the quality of the role model the whole system depends on.
