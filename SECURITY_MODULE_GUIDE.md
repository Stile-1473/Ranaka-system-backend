# Security Module Guide

This guide explains how authentication and authorization work across both the REST API and real-time notifications.

## 1. The Big Picture

The backend uses JWT-based stateless security.

That means:

- the user logs in once
- the backend returns a token
- the frontend sends that token on future requests
- Spring Security rebuilds the user context from the token each time

There is no server-side login session to keep track of.

## 2. The Main Security Files

- [SecurityConfig.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/security/config/SecurityConfig.java:1)
  Controls endpoint protection, stateless mode, CORS, and JWT filter registration.

- [JwtAuthenticationFilter.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/security/jwt/JwtAuthenticationFilter.java:1)
  Reads the `Authorization` header and turns a valid token into an authenticated Spring Security user.

- [JwtService.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/security/jwt/JwtService.java:1)
  Generates and validates tokens.

- [StompAuthChannelInterceptor.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/security/websocket/StompAuthChannelInterceptor.java:1)
  Applies the same JWT idea to STOMP WebSocket connections.

- [WebSocketConfig.java](/home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka/src/main/java/Ranaka/ranaka/config/WebSocketConfig.java:1)
  Defines the `/ws` endpoint and broker prefixes used for live notifications.

## 3. REST Login Flow

Here is the normal REST authentication story:

1. the frontend posts email and password to `/api/v1/auth/login`
2. the backend validates credentials
3. `JwtService` creates a token
4. the frontend stores the token
5. future API calls send `Authorization: Bearer <token>`

Example:

```http
GET /api/v1/requests/my-requests
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## 4. What The JWT Filter Does

`JwtAuthenticationFilter` runs once per request.

In plain English, it does this:

1. read the `Authorization` header
2. ignore the request if there is no Bearer token
3. extract the email from the token
4. load the user details
5. validate the token
6. place an authenticated principal into the Spring Security context

Once that happens, the rest of the application can treat the request as belonging to a real user.

That is why controller methods and service logic can ask for the “current user”.

## 5. Public Vs Protected Endpoints

`SecurityConfig` currently allows these without authentication:

- `/api/auth/**`
- `/api/v1/auth/**`
- `/ws/**`
- `/error`

Everything else requires authentication.

That is a sensible MVP setup because login and WebSocket handshake must be reachable before a user is fully connected.

## 6. Role-Based Access

Authentication answers:

"Who is this user?"

Authorization answers:

"Is this user allowed to do this?"

Example:

- a Requester may create and submit requests
- an Admin may recommend, reject, or return
- a GM may approve
- a CEO may authorize

Even if two users are both authenticated, they should not be able to perform the same business actions.

## 7. WebSocket And STOMP Authentication

This backend also uses the JWT token for live notifications.

That matters because real-time updates should still be private.

The flow is:

1. frontend opens a SockJS/STOMP connection to `/ws`
2. frontend sends the Bearer token in the STOMP `CONNECT` headers
3. `StompAuthChannelInterceptor` validates that token
4. Spring attaches the authenticated principal to the socket session
5. the backend can push user-specific notifications safely

Example frontend idea:

```javascript
client.connect(
  {
    Authorization: `Bearer ${token}`,
  },
  () => {
    client.subscribe("/user/queue/notifications", (message) => {
      console.log("Live notification:", JSON.parse(message.body));
    });
  }
);
```

## 8. Broker Prefixes In Human Terms

Configured in `WebSocketConfig`:

- `/ws`
  This is the connection endpoint the browser opens first.

- `/app`
  This is for messages sent from client to server.

- `/user`
  This is for user-specific destinations.

- `/topic`
  This is more appropriate for shared broadcast-style streams.

For notifications, `/user/...` is the important one because each person should only receive their own updates.

## 9. CORS Notes

`SecurityConfig` currently allows local frontend origins like:

- `http://localhost:5173`
- `http://127.0.0.1:5173`
- `http://localhost:4173`
- `http://127.0.0.1:4173`

That is enough for local development, but production should be tightened to real deployed frontend domains.

## 10. MVP Readiness Suggestions

This security setup is solid for an MVP, but a few follow-up checks are still worth keeping in mind:

- use a strong production JWT secret
- confirm the secret is base64 encoded correctly
- restrict production CORS origins
- add tests for forbidden role actions
- confirm WebSocket clients always send the token on connect

If those checks are in place, the security layer is in a good position for a demo-ready internal system.
