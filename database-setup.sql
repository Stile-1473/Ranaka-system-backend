-- Ranaka Procurement System - Database Setup Script
-- This script is aligned with the current Spring Boot/JPA entity model.
-- Update the database name below if your local .env / application config uses a different value.
--
-- Important for existing local databases:
-- if your schema was created from an older version of the project, run
-- `database-schema-fix.sql` before starting the app. Hibernate update mode
-- does not remove stale columns automatically.

CREATE DATABASE IF NOT EXISTS zero_trust
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE zero_trust;

-- ==================== MASTER TABLES ====================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('REQUESTER', 'ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value VARCHAR(1000),
    description VARCHAR(500),
    setting_type VARCHAR(255) NOT NULL,
    is_system_setting BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ==================== PROCUREMENT WORKFLOW ====================

CREATE TABLE IF NOT EXISTS procurement_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    estimated_cost DECIMAL(15,2),
    department_id BIGINT NOT NULL,
    justification VARCHAR(1000) NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    required_by_date DATE NOT NULL,
    requester_id BIGINT NOT NULL,
    status ENUM(
        'DRAFT',
        'SUBMITTED',
        'PENDING_ADMIN_RECOMMENDATION',
        'RECOMMENDED',
        'PENDING_GM_APPROVAL',
        'APPROVED_BY_GM',
        'PENDING_CEO_AUTHORIZATION',
        'AUTHORIZED',
        'RETURNED_FOR_CORRECTION',
        'REJECTED',
        'OVERDUE',
        'COMPLETED'
    ) NOT NULL,
    current_stage ENUM(
        'DRAFT',
        'ADMIN_RECOMMENDATION',
        'GM_APPROVAL',
        'CEO_AUTHORIZATION',
        'COMPLETED',
        'CANCELLED'
    ),
    submitted_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    return_count INT DEFAULT 0,
    is_overdue BOOLEAN DEFAULT FALSE,
    overdue_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_procurement_request_department
        FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_procurement_request_requester
        FOREIGN KEY (requester_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS request_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    item_description VARCHAR(500) NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(15,2) NOT NULL,
    total_cost DECIMAL(15,2) NOT NULL,
    unit VARCHAR(50) DEFAULT 'PCS',
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_line_item_request
        FOREIGN KEY (request_id) REFERENCES procurement_requests(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS request_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    stage ENUM('ADMIN_RECOMMENDATION', 'GM_APPROVAL', 'CEO_AUTHORIZATION') NOT NULL,
    action ENUM('RECOMMEND', 'APPROVE', 'AUTHORIZE', 'REJECT', 'RETURN_FOR_CORRECTION') NOT NULL,
    comment VARCHAR(1000),
    action_date TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_approval_request
        FOREIGN KEY (request_id) REFERENCES procurement_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_approval_approver
        FOREIGN KEY (approver_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS request_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    commenter_id BIGINT NOT NULL,
    comment VARCHAR(1000) NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_comment_request
        FOREIGN KEY (request_id) REFERENCES procurement_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_comment_commenter
        FOREIGN KEY (commenter_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS request_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    uploaded_by_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_attachment_request
        FOREIGN KEY (request_id) REFERENCES procurement_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_attachment_user
        FOREIGN KEY (uploaded_by_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    type ENUM(
        'REQUEST_SUBMITTED',
        'REQUEST_MOVED_TO_NEXT_STAGE',
        'REQUEST_RETURNED',
        'REQUEST_REJECTED',
        'REQUEST_APPROVED',
        'REQUEST_AUTHORIZED',
        'REQUEST_COMPLETED',
        'REQUEST_OVERDUE',
        'REMINDER_TRIGGERED'
    ) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    reference_id BIGINT NULL,
    reference_type VARCHAR(255) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action ENUM(
        'LOGIN',
        'LOGOUT',
        'CREATE_DRAFT',
        'UPDATE_DRAFT',
        'SUBMIT_REQUEST',
        'RECOMMEND_REQUEST',
        'APPROVE_REQUEST',
        'AUTHORIZE_REQUEST',
        'REJECT_REQUEST',
        'RETURN_REQUEST',
        'CREATE_COMMENT',
        'UPLOAD_ATTACHMENT',
        'CREATE_USER',
        'UPDATE_USER',
        'DEACTIVATE_USER',
        'SETTINGS_CHANGED'
    ) NOT NULL,
    description VARCHAR(500),
    entity_id BIGINT NULL,
    entity_type VARCHAR(255) NULL,
    old_value VARCHAR(1000) NULL,
    new_value VARCHAR(1000) NULL,
    ip_address VARCHAR(255) NULL,
    user_agent VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ==================== INDEXES ====================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_departments_code ON departments(code);
CREATE INDEX idx_procurement_requests_status ON procurement_requests(status);
CREATE INDEX idx_procurement_requests_stage ON procurement_requests(current_stage);
CREATE INDEX idx_procurement_requests_requester ON procurement_requests(requester_id);
CREATE INDEX idx_procurement_requests_department ON procurement_requests(department_id);
CREATE INDEX idx_request_approvals_request ON request_approvals(request_id);
CREATE INDEX idx_request_approvals_stage ON request_approvals(stage);
CREATE INDEX idx_request_comments_request ON request_comments(request_id);
CREATE INDEX idx_request_attachments_request ON request_attachments(request_id);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_unread ON notifications(recipient_id, is_read);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_id, entity_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- ==================== DEFAULT SETTINGS ====================

INSERT IGNORE INTO system_settings (setting_key, setting_value, description, setting_type, is_system_setting) VALUES
('SLA_ADMIN_HOURS', '24', 'SLA time limit for admin recommendation in hours', 'INTEGER', TRUE),
('SLA_GM_HOURS', '48', 'SLA time limit for GM approval in hours', 'INTEGER', TRUE),
('SLA_CEO_HOURS', '72', 'SLA time limit for CEO authorization in hours', 'INTEGER', TRUE),
('SLA_REMINDER_HOURS', '2', 'Hours before SLA breach to send reminder', 'INTEGER', TRUE),
('PRIORITY_CRITICAL_SLA_HOURS', '4', 'SLA hours for critical priority requests', 'INTEGER', TRUE),
('PRIORITY_HIGH_SLA_HOURS', '8', 'SLA hours for high priority requests', 'INTEGER', TRUE),
('PRIORITY_MEDIUM_SLA_HOURS', '24', 'SLA hours for medium priority requests', 'INTEGER', TRUE),
('PRIORITY_LOW_SLA_HOURS', '48', 'SLA hours for low priority requests', 'INTEGER', TRUE);

-- ==================== OPTIONAL REFERENCE DATA ====================

INSERT IGNORE INTO departments (name, code, description, is_active) VALUES
('Legal Services', 'LEGAL_SERVICES', 'Supports legal aid casework, advocacy, and direct client service operations.', TRUE),
('Finance and Administration', 'FINANCE_ADMIN', 'Handles budgeting, procurement administration, and operational support.', TRUE),
('ICT and Systems', 'ICT_SYSTEMS', 'Maintains infrastructure, devices, software subscriptions, and internal platforms.', TRUE),
('Community Outreach', 'COMMUNITY_OUTREACH', 'Coordinates mobile clinics, awareness campaigns, and stakeholder engagement.', TRUE);
