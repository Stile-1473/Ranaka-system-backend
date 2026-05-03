# Frontend Integration Guide

This guide shows how a frontend can integrate with the Ranaka backend in a practical way.

It focuses on:

- exact API base URL ideas
- how to authenticate
- how to send authorized requests
- how to call the main feature endpoints
- how to connect to WebSocket/STOMP for live notifications
- sample request and response snippets you can build UI around

## 1. Backend Base URL

By default, the backend runs on:

```text
http://localhost:5005
```

So your base API URL is usually:

```text
http://localhost:5005/api/v1
```

And the WebSocket endpoint is:

```text
http://localhost:5005/ws
```

## 2. Recommended Frontend Setup

If your frontend is running on Vite locally, it will likely be on:

- `http://localhost:5173`
- or `http://127.0.0.1:5173`

Those origins are already allowed by backend CORS and WebSocket config.

## 3. Suggested API Client Wrapper

You can use `fetch`, `axios`, or any HTTP client. Here is a simple `fetch`-style pattern.

### `src/lib/api.ts`

```ts
const API_BASE_URL = "http://localhost:5005/api/v1";

export function getAuthToken() {
  return localStorage.getItem("ranaka_token");
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getAuthToken();

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  });

  if (!response.ok) {
    let errorBody: unknown = null;

    try {
      errorBody = await response.json();
    } catch {
      errorBody = null;
    }

    throw {
      status: response.status,
      body: errorBody,
    };
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
```

## 4. Authentication Flow

### 4.1 Login request

Endpoint:

```http
POST /api/v1/auth/login
```

Example request:

```json
{
  "email": "requester1@ranaka.org",
  "password": "Password@123"
}
```

Typical response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "requester1@ranaka.org",
  "message": "User logged in",
  "role": "REQUESTER"
}
```

### Frontend example

```ts
type AuthResponse = {
  token: string;
  email: string;
  message: string;
  role: "REQUESTER" | "ADMIN" | "GM" | "CEO" | "SYSTEM_ADMIN";
};

export async function login(email: string, password: string) {
  const result = await apiFetch<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

  localStorage.setItem("ranaka_token", result.token);
  localStorage.setItem("ranaka_role", result.role);

  return result;
}
```

## 5. Load Current User After Login

Endpoint:

```http
GET /api/v1/auth/me
Authorization: Bearer <token>
```

Example response:

```json
{
  "id": 5,
  "firstName": "Lindiwe",
  "lastName": "Sibanda",
  "email": "requester1@ranaka.org",
  "phoneNumber": "+263771000005",
  "role": "REQUESTER",
  "active": true
}
```

### Frontend example

```ts
type CurrentUser = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  role: "REQUESTER" | "ADMIN" | "GM" | "CEO" | "SYSTEM_ADMIN";
  active: boolean;
};

export function getCurrentUser() {
  return apiFetch<CurrentUser>("/auth/me");
}
```

## 6. Route Users By Role

After loading `/auth/me`, the frontend can redirect like this:

```ts
export function getDefaultRouteForRole(role: CurrentUser["role"]) {
  switch (role) {
    case "REQUESTER":
      return "/dashboard";
    case "ADMIN":
      return "/admin/dashboard";
    case "GM":
      return "/gm/dashboard";
    case "CEO":
      return "/ceo/dashboard";
    case "SYSTEM_ADMIN":
      return "/system/dashboard";
  }
}
```

## 7. Create A Draft Request

Endpoint:

```http
POST /api/v1/requests
Authorization: Bearer <token>
Content-Type: application/json
```

Example request body:

```json
{
  "title": "Laptop replacements for legal officers",
  "description": "Replacement of aging laptops used for case preparation and court work.",
  "lineItems": [
    {
      "itemDescription": "Dell Latitude laptop",
      "quantity": 3,
      "unitCost": 1200.0,
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

### Frontend example

```ts
export function createRequest(payload: {
  title: string;
  description: string;
  lineItems: Array<{
    itemDescription: string;
    quantity: number;
    unitCost: number;
    unit?: string;
    notes?: string;
  }>;
  departmentId: number;
  justification: string;
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  requiredByDate: string;
}) {
  return apiFetch("/requests", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
```

## 8. Update A Draft Request

Endpoint:

```http
PUT /api/v1/requests/{requestId}
```

Use the same shape as create, but only the fields being updated need to be included if your UI supports partial editing through the DTO you send.

Example:

```ts
export function updateRequest(requestId: number, payload: unknown) {
  return apiFetch(`/requests/${requestId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
```

## 9. Submit A Request

Endpoint:

```http
POST /api/v1/requests/{requestId}/submit
```

Example:

```ts
export function submitRequest(requestId: number) {
  return apiFetch(`/requests/${requestId}/submit`, {
    method: "POST",
  });
}
```

## 10. Load My Requests

Endpoint:

```http
GET /api/v1/requests/my-requests?page=0&size=10&sort=createdAt&direction=desc
```

Typical response shape:

```json
{
  "content": [
    {
      "id": 12,
      "title": "Stationery packs for district legal clinics",
      "status": "PENDING_ADMIN_RECOMMENDATION",
      "currentStage": "ADMIN_RECOMMENDATION",
      "priority": "HIGH",
      "estimatedCost": 1200.0,
      "requiredByDate": "2026-05-10",
      "requesterName": "Farai Mhlanga",
      "departmentName": "Community Outreach",
      "submittedAt": "2026-04-22T09:00:00",
      "createdAt": "2026-04-22T08:30:00",
      "overdue": false,
      "returnCount": 0
    }
  ],
  "pageable": {},
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": {},
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

### Frontend example

```ts
export function getMyRequests(page = 0, size = 10) {
  return apiFetch(`/requests/my-requests?page=${page}&size=${size}&sort=createdAt&direction=desc`);
}
```

## 11. Load Request Details

Endpoint:

```http
GET /api/v1/requests/{requestId}
```

This is the best endpoint for the request detail page because it includes:

- line items
- approval history
- comments
- attachments
- requester info
- status and stage

### Frontend example

```ts
export function getRequestDetails(requestId: number) {
  return apiFetch(`/requests/${requestId}`);
}
```

## 12. Check If Current User Can Perform An Action

Endpoint:

```http
GET /api/v1/requests/{requestId}/can-act?action=approve
```

Useful for enabling or disabling UI buttons.

Example:

```ts
export function canActOnRequest(requestId: number, action: string) {
  return apiFetch<boolean>(`/requests/${requestId}/can-act?action=${encodeURIComponent(action)}`);
}
```

## 13. Approval Actions From The UI

These are the main workflow action endpoints:

- `POST /api/v1/requests/{id}/recommend`
- `POST /api/v1/requests/{id}/approve`
- `POST /api/v1/requests/{id}/authorize`
- `POST /api/v1/requests/{id}/reject`
- `POST /api/v1/requests/{id}/return`

The backend also exposes approval-centric aliases:

- `POST /api/v1/approvals/{id}/recommend`
- `POST /api/v1/approvals/{id}/approve`
- `POST /api/v1/approvals/{id}/authorize`
- `POST /api/v1/approvals/{id}/reject`
- `POST /api/v1/approvals/{id}/return`

Recommendation:
pick one style in the frontend and stay consistent.
For most UI code, the request-centric paths are easier to read because the request detail page usually owns these actions.

Common request body:

```json
{
  "comment": "Budget line confirmed. Forwarding to GM."
}
```

### Frontend helper

```ts
export function performApprovalAction(
  requestId: number,
  action: "recommend" | "approve" | "authorize" | "reject" | "return",
  comment: string
) {
  return apiFetch(`/requests/${requestId}/${action}`, {
    method: "POST",
    body: JSON.stringify({ comment }),
  });
}
```

## 14. Comments From The UI

Endpoint:

```http
POST /api/v1/requests/{requestId}/comments?comment=Please%20attach%20a%20quote&isInternal=false
```

This endpoint currently expects query parameters instead of a JSON body.

### Frontend example

```ts
export function addComment(
  requestId: number,
  comment: string,
  isInternal = false
) {
  const params = new URLSearchParams({
    comment,
    isInternal: String(isInternal),
  });

  return apiFetch<void>(`/requests/${requestId}/comments?${params.toString()}`, {
    method: "POST",
  });
}
```

To load comments:

```ts
export function getComments(requestId: number) {
  return apiFetch(`/requests/${requestId}/comments`);
}
```

## 15. Attachment Upload From The UI

Endpoint:

```http
POST /api/v1/requests/{requestId}/attachments
Content-Type: multipart/form-data
```

Important:
do not send `"Content-Type": "application/json"` for file upload.

### Frontend example

```ts
export async function uploadAttachment(requestId: number, file: File) {
  const token = localStorage.getItem("ranaka_token");
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(
    `http://localhost:5005/api/v1/requests/${requestId}/attachments`,
    {
      method: "POST",
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: formData,
    }
  );

  if (!response.ok) {
    throw new Error("Attachment upload failed");
  }
}
```

To load attachments:

```ts
export function getAttachments(requestId: number) {
  return apiFetch(`/requests/${requestId}/attachments`);
}
```

## 16. Role-Specific Pending Queues

These endpoints are useful for dashboard queue tables:

- Admin: `GET /api/v1/requests/pending/admin`
- GM: `GET /api/v1/requests/pending/gm`
- CEO: `GET /api/v1/requests/pending/ceo`

Approval-centric aliases also exist:

- `GET /api/v1/approvals/pending/admin`
- `GET /api/v1/approvals/pending/gm`
- `GET /api/v1/approvals/pending/ceo`

### Frontend example

```ts
export function getPendingQueue(role: "ADMIN" | "GM" | "CEO") {
  const path =
    role === "ADMIN"
      ? "/requests/pending/admin"
      : role === "GM"
      ? "/requests/pending/gm"
      : "/requests/pending/ceo";

  return apiFetch(path);
}
```

## 17. Dashboard Integration

### Summary endpoint

Endpoint:

```http
GET /api/v1/dashboard/summary
```

Important detail:
this endpoint is role-aware. The backend adapts the summary to the signed-in user.

Typical shape:

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

### Frontend example

```ts
export function getDashboardSummary() {
  return apiFetch("/dashboard/summary");
}
```

### Additional dashboard endpoints

- `GET /api/v1/dashboard/request-trends?startDate=2026-04-01&endDate=2026-04-30`
- `GET /api/v1/dashboard/priority-distribution`
- `GET /api/v1/dashboard/department-stats`
- `GET /api/v1/dashboard/stage-performance`
- `GET /api/v1/dashboard/overdue-summary`

These are especially useful for Admin, GM, CEO, and System Admin dashboards.

## 18. Notifications API Integration

### Get notifications

Endpoint:

```http
GET /api/v1/notifications/my?page=0&size=10
```

Notification item example:

```json
{
  "id": 15,
  "type": "REQUEST_SUBMITTED",
  "title": "New request submitted",
  "message": "Procurement request 'Stationery packs for district legal clinics' needs your recommendation.",
  "referenceId": 12,
  "referenceType": "ProcurementRequest",
  "read": false,
  "readAt": null,
  "emailSent": true,
  "emailSentAt": "2026-04-22T10:15:30",
  "createdAt": "2026-04-22T10:15:00"
}
```

### Mark one as read

```ts
export function markNotificationAsRead(notificationId: number) {
  return apiFetch(`/notifications/${notificationId}/read`, {
    method: "PATCH",
  });
}
```

### Mark all as read

```ts
export function markAllNotificationsAsRead() {
  return apiFetch<{ updatedCount: number }>("/notifications/read-all", {
    method: "PATCH",
  });
}
```

### Unread count

```ts
export function getUnreadCount() {
  return apiFetch<{ unreadCount: number }>("/notifications/unread-count");
}
```

## 19. WebSocket/STOMP Integration For Real-Time Notifications

The backend uses:

- SockJS endpoint: `/ws`
- personal destination: `/user/queue/notifications`
- unread-count destination: `/user/queue/notifications/unread-count`

### Install client packages

```bash
npm install @stomp/stompjs sockjs-client
```

### Example notification socket client

```ts
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

let stompClient: Client | null = null;

export function connectNotificationsSocket({
  token,
  onNotification,
  onUnreadCount,
}: {
  token: string;
  onNotification: (payload: unknown) => void;
  onUnreadCount: (count: number) => void;
}) {
  stompClient = new Client({
    webSocketFactory: () => new SockJS("http://localhost:5005/ws"),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 5000,
    debug: () => {},
    onConnect: () => {
      stompClient?.subscribe("/user/queue/notifications", (message) => {
        onNotification(JSON.parse(message.body));
      });

      stompClient?.subscribe(
        "/user/queue/notifications/unread-count",
        (message) => {
          onUnreadCount(JSON.parse(message.body));
        }
      );
    },
  });

  stompClient.activate();
}

export function disconnectNotificationsSocket() {
  stompClient?.deactivate();
  stompClient = null;
}
```

### How it works

1. user logs in
2. frontend stores JWT token
3. frontend opens SockJS connection to `/ws`
4. STOMP `CONNECT` frame includes `Authorization: Bearer <token>`
5. backend authenticates the socket
6. frontend subscribes to:
   `/user/queue/notifications`
   `/user/queue/notifications/unread-count`

## 20. React Example For Notifications

```tsx
import { useEffect, useState } from "react";
import {
  connectNotificationsSocket,
  disconnectNotificationsSocket,
} from "./notificationsSocket";

export function NotificationsBootstrap() {
  const [unreadCount, setUnreadCount] = useState(0);
  const token = localStorage.getItem("ranaka_token");

  useEffect(() => {
    if (!token) return;

    connectNotificationsSocket({
      token,
      onNotification: (notification) => {
        console.log("Live notification", notification);
      },
      onUnreadCount: (count) => {
        setUnreadCount(count);
      },
    });

    return () => {
      disconnectNotificationsSocket();
    };
  }, [token]);

  return <span>{unreadCount}</span>;
}
```

## 21. Reports Integration

Main report endpoints:

- `GET /api/v1/reports/approval-times`
- `GET /api/v1/reports/bottlenecks`
- `GET /api/v1/reports/department-usage`
- `GET /api/v1/reports/returns-rejections`
- `GET /api/v1/reports/overdue-requests`
- `GET /api/v1/reports/export?type=approval-times`

Example with filters:

```ts
export function getApprovalTimesReport(params: {
  startDate?: string;
  endDate?: string;
  departmentId?: number;
  priority?: string;
  status?: string;
  overdueOnly?: boolean;
}) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });

  return apiFetch(`/reports/approval-times?${search.toString()}`);
}
```

### CSV export example

```ts
export async function exportReport(type: string) {
  const token = localStorage.getItem("ranaka_token");

  const response = await fetch(
    `http://localhost:5005/api/v1/reports/export?type=${encodeURIComponent(type)}`,
    {
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }
  );

  if (!response.ok) {
    throw new Error("Report export failed");
  }

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${type}-report.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
```

## 22. Settings Integration

System Admin endpoints:

- `GET /api/v1/settings/sla`
- `PUT /api/v1/settings/sla`
- `GET /api/v1/settings/priorities`
- `PUT /api/v1/settings/priorities`
- `GET /api/v1/settings/workflow`

Example SLA response:

```json
{
  "adminSlaHours": 24,
  "gmSlaHours": 48,
  "ceoSlaHours": 72,
  "reminderHoursBeforeBreach": 2
}
```

Example update:

```ts
export function updateSlaConfig(payload: {
  adminSlaHours: number;
  gmSlaHours: number;
  ceoSlaHours: number;
  reminderHoursBeforeBreach: number;
}) {
  return apiFetch("/settings/sla", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
```

## 23. User Management Integration

System Admin endpoints:

- `GET /api/v1/users`
- `POST /api/v1/users`
- `GET /api/v1/users/{id}`
- `PUT /api/v1/users/{id}`
- `PATCH /api/v1/users/{id}/activate`
- `PATCH /api/v1/users/{id}/deactivate`
- `GET /api/v1/users/role/{role}`

Example create user request:

```json
{
  "firstName": "Tariro",
  "lastName": "Ncube",
  "email": "admin2@ranaka.org",
  "phoneNumber": "+263771222222",
  "password": "Password@123",
  "role": "ADMIN"
}
```

## 24. Common Error Handling In The Frontend

The backend returns a structured error shape.

Typical example:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "requiredByDate": "Required by date must be in the future"
  },
  "timestamp": "2026-04-22T10:15:30",
  "path": "/api/v1/requests"
}
```

### Frontend suggestion

- show `message` as the top-level toast or alert
- if `details` is an object, map it to form field errors
- use `code` for conditional handling when needed

Example:

```ts
export function extractFieldErrors(error: any): Record<string, string> {
  if (error?.body?.details && typeof error.body.details === "object") {
    return error.body.details;
  }

  return {};
}
```

## 25. Suggested Frontend Flow On App Start

When the app loads:

1. read token from storage
2. if no token, show login page
3. if token exists, call `/api/v1/auth/me`
4. store current user in app state
5. load role-appropriate dashboard
6. connect WebSocket/STOMP notification client
7. load unread notification count

This creates a smooth “resume session” experience for the user.

## 26. Practical UI Mapping

Here is a simple mental map from screens to endpoints:

- Login page
  `POST /api/v1/auth/login`

- Current session bootstrap
  `GET /api/v1/auth/me`

- Requester dashboard
  `GET /api/v1/dashboard/summary`
  `GET /api/v1/requests/my-requests`

- Request detail page
  `GET /api/v1/requests/{id}`
  `GET /api/v1/requests/{id}/comments`
  `GET /api/v1/requests/{id}/attachments`

- Approval queue page
  `GET /api/v1/requests/pending/admin`
  or GM/CEO equivalent

- Notification center
  `GET /api/v1/notifications/my`
  `GET /api/v1/notifications/unread-count`
  plus WebSocket subscriptions

- Reports page
  `GET /api/v1/reports/...`

- Settings page
  `GET /api/v1/settings/sla`
  `PUT /api/v1/settings/sla`

## 27. Final Frontend Tips

- Keep the JWT token in a central auth store.
- Always attach the token to protected API calls.
- Connect the STOMP socket only after login.
- Disconnect the socket on logout.
- Use `/auth/me` as the trusted source of the current role.
- Use `/requests/{id}/can-act` to simplify action-button rules in the UI.
- For file uploads, send `FormData`, not JSON.
- Treat notification unread count as both an API-driven and socket-driven value.

This should give your frontend enough structure to integrate cleanly with the current backend without guessing endpoint behavior.
