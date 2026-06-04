# 🏢 Ranaka Procurement Workflow & Approval Management System

## Backend Implementation - Complete Setup Guide

Welcome to the Ranaka Backend! This is a comprehensive Spring Boot application for managing procurement requests with a multi-level approval workflow.

---

## 📑 Quick Navigation

- [✨ Features](#-features)
- [🛠️ Tech Stack](#️-tech-stack)
- [📋 Requirements](#-requirements)
- [🚀 Quick Start](#-quick-start)
- [⚙️ Configuration](#️-configuration)
- [📧 Email Setup](#-email-setup)
- [🔄 Workflow](#-workflow)
- [📚 API Documentation](#-api-documentation)
- [🧠 Humanized Guide](./HUMANIZED_BACKEND_GUIDE.md)
- [🐛 Troubleshooting](#-troubleshooting)

## 🧠 Humanized Guide

If you want the backend explained in plain, teammate-friendly language with realistic request examples,
open [HUMANIZED_BACKEND_GUIDE.md](./HUMANIZED_BACKEND_GUIDE.md).

Module guides:

- [AUTHENTICATION_MODULE_GUIDE.md](./AUTHENTICATION_MODULE_GUIDE.md)
- [COMMON_MODULE_GUIDE.md](./COMMON_MODULE_GUIDE.md)
- [BOOTSTRAP_MODULE_GUIDE.md](./BOOTSTRAP_MODULE_GUIDE.md)
- [DTO_GUIDE.md](./DTO_GUIDE.md)
- [SECURITY_MODULE_GUIDE.md](./SECURITY_MODULE_GUIDE.md)
- [ENTITY_MODEL_GUIDE.md](./ENTITY_MODEL_GUIDE.md)
- [REPOSITORY_GUIDE.md](./REPOSITORY_GUIDE.md)
- [SERVICE_LAYER_GUIDE.md](./SERVICE_LAYER_GUIDE.md)
- [SLA_GUIDE.md](./SLA_GUIDE.md)
- [USER_FLOW_GUIDE.md](./USER_FLOW_GUIDE.md)
- [FRONTEND_INTEGRATION_GUIDE.md](./FRONTEND_INTEGRATION_GUIDE.md)
- [NOTIFICATION_MODULE_GUIDE.md](./NOTIFICATION_MODULE_GUIDE.md)
- [SETTINGS_MODULE_GUIDE.md](./SETTINGS_MODULE_GUIDE.md)
- [USER_MODULE_GUIDE.md](./USER_MODULE_GUIDE.md)
- [DEPARTMENT_MODULE_GUIDE.md](./DEPARTMENT_MODULE_GUIDE.md)
- [DASHBOARD_MODULE_GUIDE.md](./DASHBOARD_MODULE_GUIDE.md)
- [REQUEST_MODULE_GUIDE.md](./REQUEST_MODULE_GUIDE.md)
- [REPORT_MODULE_GUIDE.md](./REPORT_MODULE_GUIDE.md)
- [AUDIT_MODULE_GUIDE.md](./AUDIT_MODULE_GUIDE.md)

---

## ✨ Features

### Core Functionality
- ✅ **Multi-level Approval Workflow**: REQUESTER → ADMIN → GM → CEO
- ✅ **Request Management**: Create, update, submit, and track procurement requests
- ✅ **Role-Based Access Control**: Different capabilities for each user role
- ✅ **Comprehensive Audit Trail**: Track all actions and changes
- ✅ **Real-time Notifications**: Email notifications for all workflow events
- ✅ **SLA Tracking**: Monitor request processing times
- ✅ **Request Attachments**: Upload and manage supporting documents
- ✅ **Comments & Notes**: Internal and external commenting system
- ✅ **Advanced Filtering**: Search and filter requests by various criteria
- ✅ **Dashboard Analytics**: View request statistics and pending items

### Security Features
- 🔐 JWT Authentication
- 🔐 Role-Based Authorization
- 🔐 Password Hashing (Bcrypt)
- 🔐 Audit Logging
- 🔐 IP Tracking

---

## 🛠️ Tech Stack

**Backend Framework**
- Spring Boot 4.0.5
- Spring Data JPA
- Spring Security with JWT
- Maven

**Database**
- MySQL 8.0+

**Email Service**
- Spring Mail (SMTP)

**Languages & Tools**
- Java 21
- Lombok (for boilerplate reduction)
- JPA Validation

---

## 📋 Requirements

### System Requirements
- Java 21 or higher
- Maven 3.8.1 or higher
- MySQL 8.0 or higher
- 2GB RAM minimum
- Internet connection (for dependencies)

### For Email Notifications
- Valid SMTP server credentials
- Gmail: App Password (2FA enabled account)
- Or custom SMTP server

### Development Tools (Optional)
- IntelliJ IDEA / VSCode / Eclipse
- MySQL Workbench / CLI
- Postman (for API testing)

---

## 🚀 Quick Start

### Option 1: Using the Start Script (Recommended)

```bash
cd /home/ghost1473/Desktop/Projects/RANAKA_SYSTEM/BACKEND/ranaka

# Copy environment template
cp .env.template .env

# Edit .env with your configuration
nano .env

# Run the start script
./start.sh
```

### Option 2: Manual Setup

**Step 1: Set Environment Variables**
```bash
export DB_HOST=localhost
export DB_NAME=zero_trust
export DB_USERNAME=ranaka_user
export DB_PASSWORD=your_password
export SERVER_PORT=5005
export JWT_SECRET=your_jwt_secret_key
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

**Step 2: Create Database**
```bash
mysql -h localhost -u root -p < database-setup.sql
```

**Step 3: Build Project**
```bash
mvn clean package
```

**Step 4: Run Application**
```bash
java -jar target/ranaka-0.0.1-SNAPSHOT.jar
```

### Option 3: Using Docker

**Create Docker Container**
```bash
docker run -d --name ranaka-db \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=zero_trust \
  -p 3306:3306 \
  mysql:8.0

# Wait for MySQL to start, then run database setup
mysql -h 127.0.0.1 -u root -proot < database-setup.sql
```

**Run Backend**
```bash
docker run -d --name ranaka-backend \
  -e DB_HOST=host.docker.internal \
  -e DB_NAME=zero_trust \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=root \
  -e SERVER_PORT=5005 \
  -e JWT_SECRET=your_secret \
  -e MAIL_HOST=smtp.gmail.com \
  -e MAIL_PORT=587 \
  -e MAIL_USERNAME=your-email@gmail.com \
  -e MAIL_PASSWORD=your-app-password \
  -p 5005:5005 \
  ranaka-backend:latest
```

---

## ⚙️ Configuration

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Application
spring.application.name=ranaka
server.port=${SERVER_PORT}

# Database
spring.datasource.url=jdbc:mysql://${DB_HOST}:3306/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

# Hibernate (Auto-creates tables)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000

# Email
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

### Environment Variables Priority

1. **System Environment Variables** (highest priority)
2. **Docker Environment Variables**
3. **.env file** (lowest priority)

```bash
# Check current values
echo $DB_HOST
echo $MAIL_USERNAME
```

---

## 📧 Email Setup

### Gmail Configuration (Recommended for Testing)

1. **Enable 2-Factor Authentication**
   - Go to myaccount.google.com/security
   - Enable 2-Step Verification

2. **Generate App Password**
   - Go to myaccount.google.com/apppasswords
   - Select "Mail" and "Other (custom name)"
   - Google generates a 16-character password
   - Copy the password (without spaces)

3. **Configure in .env**
   ```
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=xxxx xxxx xxxx xxxx  (remove spaces)
   ```

### Office 365 Configuration

```
MAIL_HOST=smtp.office365.com
MAIL_PORT=587
MAIL_USERNAME=your-email@company.com
MAIL_PASSWORD=your-office-password
```

### Custom SMTP Server

```
MAIL_HOST=mail.yourdomain.com
MAIL_PORT=587
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=your-password
```

### Test Email Configuration

```bash
# Send a test email
curl -X POST http://localhost:5005/api/test/send-email \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "test@example.com",
    "subject": "Test Email",
    "body": "This is a test email"
  }'
```

---

## 🔄 Workflow

### Request Lifecycle

```
┌─────────────┐
│    DRAFT    │  User creates request
└──────┬──────┘
       │ submit()
┌──────▼──────────────────────┐
│ PENDING_ADMIN_RECOMMENDATION│  Admin reviews
└──────┬───────────────────────┘
       │ recommend()
┌──────▼──────────────────┐
│ PENDING_GM_APPROVAL     │  GM approves
└──────┬──────────────────┘
       │ approve()
┌──────▼──────────────────┐
│PENDING_CEO_AUTHORIZATION│  CEO authorizes
└──────┬──────────────────┘
       │ authorize()
┌──────▼──────────────────┐
│     AUTHORIZED          │  ✅ Complete
└─────────────────────────┘

Alternate paths:
- reject() → REJECTED (any stage)
- return() → RETURNED (any stage)
```

### Email Notifications Sent

| Event | Recipients | Email Subject |
|-------|-----------|---------------|
| Submit | All ADMINs | New Request Submitted |
| Recommend | All GMs | Request Ready for Approval |
| Approve | All CEOs | Request Ready for Authorization |
| Authorize | Requester | Request Completed |
| Reject | Requester | Request Rejected |
| Return | Requester | Request Returned for Correction |

---

## 📚 API Documentation

### Base URL
```
http://localhost:5005/api
```

### Authentication
All endpoints require JWT token in header:
```
Authorization: Bearer {your_jwt_token}
```

### Endpoints

#### Requests
```
POST   /requests                    - Create new request
GET    /requests/{id}               - Get request details
PUT    /requests/{id}               - Update draft request
GET    /requests                    - List all requests (with pagination)
GET    /my-requests                 - Get user's requests

POST   /requests/{id}/submit        - Submit request
POST   /requests/{id}/recommend     - Admin recommends
POST   /requests/{id}/approve       - GM approves
POST   /requests/{id}/authorize     - CEO authorizes
POST   /requests/{id}/reject        - Reject request
POST   /requests/{id}/return        - Return for correction
```

#### Attachments
```
POST   /requests/{id}/attachments   - Upload file
GET    /requests/{id}/attachments   - List attachments
DELETE /requests/{id}/attachments/{fileId} - Delete attachment
```

#### Comments
```
POST   /requests/{id}/comments      - Add comment
GET    /requests/{id}/comments      - Get all comments
PUT    /comments/{id}               - Edit comment
DELETE /comments/{id}               - Delete comment
```

#### Notifications
```
GET    /notifications               - Get user's notifications
GET    /notifications/unread        - Get unread notifications
PUT    /notifications/{id}/read     - Mark as read
```

#### Audit Logs
```
GET    /audit-logs                  - Get audit trail
GET    /audit-logs/{entityId}       - Get entity audit history
```

### Example API Call

```bash
# Get pending requests
curl -X GET http://localhost:5005/api/requests \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{"status": "PENDING_ADMIN_RECOMMENDATION"}'

# Submit a request
curl -X POST http://localhost:5005/api/requests/1/submit \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Recommend a request
curl -X POST http://localhost:5005/api/requests/1/recommend \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"comment": "Looks good, forwarding to GM"}'
```

---

## 🐛 Troubleshooting

### Application Won't Start

**Error: "Could not connect to database"**
```
Solution:
1. Verify MySQL is running: mysql -u root -p -e "SELECT 1"
2. Check DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD
3. Ensure database exists: CREATE DATABASE zero_trust;
4. Run database setup: mysql -u root -p < database-setup.sql
```

**Error: "Hibernate validation failed"**
```
Solution:
1. Check if spring.jpa.hibernate.ddl-auto=update
2. Verify database tables exist
3. Run database-setup.sql again
```

### Emails Not Sending

**Error: "Authentication failed for SMTP"**
```
Solution:
1. Verify MAIL_USERNAME and MAIL_PASSWORD
2. For Gmail: Use App Password, not account password
3. For Gmail: Ensure 2FA is enabled
4. Test credentials: telnet MAIL_HOST MAIL_PORT
```

**Emails going to spam**
```
Solution:
1. Configure SPF/DKIM records for your domain
2. Use verified sender email address
3. Check spam filter settings
4. Whitelist sender email in receiving email client
```

### Performance Issues

**Requests are slow**
```
Solution:
1. Check database indexes: SHOW INDEX FROM procurement_requests;
2. Enable query caching in MySQL
3. Check application logs: tail -f logs/spring.log
4. Monitor resource usage: top, free, df -h
```

**High CPU Usage**
```
Solution:
1. Check for infinite loops: grep -r "while(true)" src/
2. Reduce log level: logging.level.root=WARN
3. Limit thread pool size in application.properties
4. Use database connection pooling
```

### Database Issues

**Tables not created**
```
Solution:
1. Ensure spring.jpa.hibernate.ddl-auto=update (not validate)
2. Restart application
3. Check database permissions
4. Run database-setup.sql manually
```

**Error: "Field 'quantity' doesn't have a default value" when creating a request**
```
Cause:
Your local MySQL schema still has an old `quantity` column on `procurement_requests`,
but the current code stores quantities in `request_line_items`.

Solution:
1. Run the compatibility fix: mysql -u root -p zero_trust < database-schema-fix.sql
2. Restart the backend
3. Retry POST /api/v1/requests
```

**Foreign key constraint error**
```
Solution:
1. Disable foreign key check temporarily:
   SET FOREIGN_KEY_CHECKS=0;
2. Delete and recreate tables
3. Re-enable: SET FOREIGN_KEY_CHECKS=1;
```

### Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| 401 Unauthorized | Invalid JWT token | Request new token via login |
| 403 Forbidden | Insufficient permissions | Check user role |
| 404 Not Found | Resource doesn't exist | Verify request ID |
| 500 Internal Server Error | Server error | Check application logs |
| 422 Unprocessable Entity | Invalid request data | Check validation rules |

---

## 📊 Database Queries

### View Pending Requests
```sql
SELECT * FROM v_pending_requests
ORDER BY priority DESC, created_at ASC;
```

### View Overdue Requests
```sql
SELECT * FROM v_overdue_requests;
```

### View User Statistics
```sql
SELECT * FROM v_user_request_stats
WHERE total_requests > 0
ORDER BY total_requests DESC;
```

### Check Unread Notifications
```sql
SELECT * FROM notifications
WHERE is_read = FALSE
ORDER BY created_at DESC;
```

### Audit Trail for Specific Request
```sql
SELECT * FROM audit_logs
WHERE entity_id = 1 AND entity_type = 'ProcurementRequest'
ORDER BY created_at DESC;
```

---

## 📞 Support & Documentation

### Resources
- [Implementation Guide](./IMPLEMENTATION_GUIDE.md)
- [Database Schema](./database-setup.sql)
- [Environment Template](./.env.template)
- [API Documentation](./HELP.md)

### Getting Help
1. Check the **Troubleshooting** section above
2. Review **application logs**: `tail -f logs/spring.log`
3. Check **database status**: `SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 10;`
4. Review **configuration**: Verify all environment variables are set correctly

---

## 🔒 Security Best Practices

1. **Never commit secrets to git**
   - Use environment variables or .env file (add to .gitignore)
   - Use secrets management in production

2. **Strong Passwords**
   - Minimum 12 characters
   - Mix of uppercase, lowercase, numbers, symbols
   - Different for each environment

3. **JWT Token Security**
   - Change JWT_SECRET in production
   - Token expiration: 24 hours (configurable)
   - Implement token refresh mechanism

4. **Database Security**
   - Use strong database passwords
   - Limit database user permissions
   - Use SSL/TLS for database connections

5. **Email Security**
   - Use app-specific passwords for email accounts
   - Enable 2FA for email accounts
   - Verify SMTP server certificates

---

## 📈 Performance Optimization

### Caching
```properties
spring.cache.type=redis  # Enable Redis caching
```

### Database Optimization
```sql
-- Create additional indexes for frequently queried fields
CREATE INDEX idx_request_date_range ON procurement_requests(created_at, submitted_at);
CREATE INDEX idx_notification_recipient_date ON notifications(recipient_id, created_at DESC);
```

### Connection Pooling
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

---

## 📝 Changelog

### Version 1.0 (April 22, 2026)
- ✅ Initial implementation
- ✅ Multi-level approval workflow
- ✅ Email notification system
- ✅ Comprehensive audit logging
- ✅ Role-based access control
- ✅ Database auto-initialization

---

## 📄 License

This project is part of the Ranaka Procurement Management System. All rights reserved.

---

## 👥 Development Team

**Backend Development**: Ranaka Development Team
**System Architecture**: Enterprise Architecture Team
**Database Design**: Database Administration Team

---

**Last Updated**: April 22, 2026  
**Version**: 1.0  
**Status**: 🟢 Production Ready

---

## Quick Links

- 🚀 [Quick Start Guide](#-quick-start)
- ⚙️ [Configuration Guide](#️-configuration)
- 📧 [Email Setup](#-email-setup)
- 📚 [API Documentation](#-api-documentation)
- 🐛 [Troubleshooting](#-troubleshooting)

---

**Enjoy using Ranaka! Happy Procuring! 🎉**
