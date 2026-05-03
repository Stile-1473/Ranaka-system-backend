# Ranaka Backend Implementation Guide

## Overview
This document provides the complete setup and implementation guide for the Ranaka Procurement Workflow Management System backend.

---

## ✅ Completed Implementations

### 1. **Database Configuration Fix**
- **Issue**: Tables were not being created on application startup
- **Solution**: Changed `spring.jpa.hibernate.ddl-auto` from `validate` to `update` in `application.properties`
- **Result**: Hibernate now automatically creates/updates database schema on startup

### 2. **Email Notification System**
- **Created**: `EmailService` - Comprehensive email notification service
- **Features**:
  - Non-blocking asynchronous email sending
  - Multiple email templates for different workflow events
  - Graceful error handling with logging
  - Email delivery tracking in notifications table
  - Support for:
    - Approval request notifications
    - Request completion notifications
    - Request rejection notifications
    - Request return notifications
    - SLA overdue/warning notifications

### 3. **Approval Workflow Enhancement**
- **Updated**: `RequestServiceImpl` to integrate EmailService
- **Improvements**:
  - Comments are now optional for approve/authorize/recommend actions
  - Comments are mandatory only for reject/return actions (enforced via validation)
  - Email notifications sent automatically when requests move through workflow
  - Improved notification tracking with email delivery status

### 4. **Maven Dependencies**
- **Added**: `spring-boot-starter-mail` dependency
- **Enables**: Full email sending capabilities via SMTP

---

## 📋 Required Environment Variables

Add the following environment variables to your deployment configuration:

```bash
# Database Configuration
DB_HOST=your_mysql_host
DB_NAME=zero_trust
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Server Configuration
SERVER_PORT=5005

# JWT Configuration
JWT_SECRET=your_secret_key_here

# Email Configuration (Gmail example)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_specific_password

# For other email providers, adjust MAIL_HOST and MAIL_PORT accordingly
```

### Email Provider Configuration Examples:

#### Gmail:
```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
```

#### Office 365:
```properties
MAIL_HOST=smtp.office365.com
MAIL_PORT=587
```

#### Custom SMTP Server:
```properties
MAIL_HOST=your.smtp.server
MAIL_PORT=587
```

---

## 🚀 Workflow - Approval Journey

The system follows this workflow:

1. **DRAFT** - User creates request in draft state
   - ✏️ Can be edited before submission

2. **SUBMIT** - User submits request
   - 📧 Email sent to all ADMINs
   - Status: PENDING_ADMIN_RECOMMENDATION

3. **ADMIN RECOMMENDS** - Admin reviews and recommends
   - 📧 Email sent to all GMs
   - Status: PENDING_GM_APPROVAL
   - Comment: Optional

4. **GM APPROVES** - General Manager approves
   - 📧 Email sent to all CEOs
   - Status: PENDING_CEO_AUTHORIZATION
   - Comment: Optional

5. **CEO AUTHORIZES** - CEO provides final authorization
   - 📧 Email sent to requester
   - Status: AUTHORIZED (Completed)
   - Comment: Optional

**Alternative Paths:**

- **REJECT** - Can be done at any stage
  - 📧 Email sent to requester with reason
  - Status: REJECTED
  - Comment: **REQUIRED**

- **RETURN** - Send back for corrections
  - 📧 Email sent to requester with reason
  - Status: RETURNED
  - Comment: **REQUIRED**

---

## 📧 Email Notifications Sent

### System Notifications:

1. **New Request Submitted**
   - Recipients: All ADMINs
   - Trigger: User submits request

2. **Request Ready for Approval**
   - Recipients: All GMs
   - Trigger: Admin recommends request

3. **Request Ready for Authorization**
   - Recipients: All CEOs
   - Trigger: GM approves request

4. **Request Completed**
   - Recipients: Requester
   - Trigger: CEO authorizes request

5. **Request Rejected**
   - Recipients: Requester
   - Trigger: Any approver rejects request
   - Includes: Rejection reason

6. **Request Returned for Correction**
   - Recipients: Requester
   - Trigger: Any approver returns request
   - Includes: Return reason

7. **SLA Overdue Warning**
   - Recipients: Current approver
   - Trigger: Request exceeds SLA timeframe

---

## 🔧 Configuration Details

### Application Properties
Located at: `src/main/resources/application.properties`

```properties
# Database
spring.datasource.url=jdbc:mysql://${DB_HOST}:3306/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

# Hibernate (Auto-creates tables on startup)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Email
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

---

## 📝 API Endpoints - Approval Actions

All endpoints are in the RequestController and require authentication.

### Submit Request
```
POST /api/requests/{requestId}/submit
Headers: Authorization: Bearer {token}
```

### Recommend Request (Admin)
```
POST /api/requests/{requestId}/recommend
Headers: Authorization: Bearer {token}
Body: {
  "comment": "Looks good, forwarding to GM" (optional)
}
```

### Approve Request (GM)
```
POST /api/requests/{requestId}/approve
Headers: Authorization: Bearer {token}
Body: {
  "comment": "Approved for CEO authorization" (optional)
}
```

### Authorize Request (CEO)
```
POST /api/requests/{requestId}/authorize
Headers: Authorization: Bearer {token}
Body: {
  "comment": "Authorized and complete" (optional)
}
```

### Reject Request
```
POST /api/requests/{requestId}/reject
Headers: Authorization: Bearer {token}
Body: {
  "comment": "Does not meet requirements" (REQUIRED)
}
```

### Return Request for Correction
```
POST /api/requests/{requestId}/return
Headers: Authorization: Bearer {token}
Body: {
  "comment": "Please provide additional information" (REQUIRED)
}
```

---

## 🗄️ Database Tables

The following tables are automatically created/updated:

- `procurement_requests` - Main request data
- `request_approvals` - Approval history
- `request_comments` - Comments on requests
- `request_attachments` - File attachments
- `notifications` - System notifications
- `audit_logs` - Comprehensive audit trail
- `users` - User information
- `departments` - Department data
- `system_settings` - System configuration

---

## 🔐 Security Features

1. **JWT Authentication**
   - Token-based access control
   - Configurable token expiration (default: 24 hours)

2. **Role-Based Access Control**
   - REQUESTER: Can create and submit requests
   - ADMIN: Can recommend requests
   - GM: Can approve requests
   - CEO: Can authorize requests
   - SYSTEM_ADMIN: Full access

3. **Audit Logging**
   - All actions logged with timestamp and user info
   - IP address and user agent tracking
   - Old/new value comparison for sensitive changes

4. **Email Validation**
   - Recipient validation before sending
   - Error handling for invalid email addresses

---

## 🧪 Testing the System

### Test Case 1: Basic Workflow
1. Create a request (Draft)
2. Submit request (notification email to admin)
3. Admin recommends (notification email to GM)
4. GM approves (notification email to CEO)
5. CEO authorizes (notification email to requester)

### Test Case 2: Rejection
1. Create and submit request
2. Admin rejects with comment (notification to requester)
3. Verify rejection email includes reason

### Test Case 3: Return for Correction
1. Create and submit request
2. Admin returns with comment (notification to requester)
3. Requester updates and resubmits

---

## 📊 Monitoring & Troubleshooting

### Check Email Sending Status
```sql
SELECT * FROM notifications WHERE email_sent = false;
```

### View Audit Trail
```sql
SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 20;
```

### Check Request Status
```sql
SELECT id, title, status, current_stage, created_at FROM procurement_requests;
```

---

## ⚠️ Important Notes

1. **Email Sending**: Requires valid SMTP credentials. If email fails, the system continues without throwing errors (graceful degradation).

2. **Database**: First startup will create all required tables automatically via Hibernate.

3. **SMTP Port**: Usually 587 for TLS or 465 for SSL. Default in config is 587.

4. **Gmail Users**: Use "App Passwords" feature rather than account password for security.

5. **Error Logging**: All errors are logged to application logs. Check logs if notifications aren't being sent.

---

## 📞 Support

For issues or questions:
1. Check application logs for errors
2. Verify email configuration with test credentials
3. Ensure database connectivity
4. Check user roles and permissions

---

**Last Updated**: April 22, 2026
**Version**: 1.0 - Initial Implementation

