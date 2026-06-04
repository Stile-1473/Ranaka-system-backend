#!/bin/bash

# Ranaka Backend - Quick Start Script
# This script helps you set up and run the Ranaka backend system

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Ranaka Procurement System - Backend Setup & Run            ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found!"
    echo "Creating .env from template..."
    if [ -f .env.template ]; then
        cp .env.template .env
        echo "✅ .env file created. Please update it with your configuration:"
        echo ""
        echo "   Database Configuration:"
        echo "   - DB_HOST: MySQL server host"
        echo "   - DB_NAME: Database name (default: zero_trust)"
        echo "   - DB_USERNAME: Database user"
        echo "   - DB_PASSWORD: Database password"
        echo ""
        echo "   Email Configuration:"
        echo "   - MAIL_HOST: SMTP server (e.g., smtp.gmail.com)"
        echo "   - MAIL_PORT: SMTP port (usually 587)"
        echo "   - MAIL_USERNAME: Email account"
        echo "   - MAIL_PASSWORD: Email password or app-specific password"
        echo ""
        echo "   Security:"
        echo "   - JWT_SECRET: Generate a strong random key"
        echo ""
        echo "✏️  Edit .env file and run this script again."
        exit 1
    else
        echo "❌ .env.template not found either!"
        exit 1
    fi
fi

echo "✅ .env file found"
echo ""

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

echo "Configuration loaded:"
echo "  Database: $DB_HOST:3306/$DB_NAME"
echo "  Server Port: $SERVER_PORT"
echo "  Mail Server: $MAIL_HOST:$MAIL_PORT"
echo ""

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found! Please install Maven first."
    exit 1
fi
echo "✅ Maven found"

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found! Please install Java 21 or later."
    exit 1
fi
echo "✅ Java found: $(java -version 2>&1 | head -n 1)"

echo ""
echo "Building project..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Clean and build
mvn clean package -DskipTests=true

if [ $? -eq 0 ]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "✅ Build successful!"
else
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "Database Setup"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "ℹ️  Make sure MySQL is running and accessible at: $DB_HOST"
echo ""
echo "To create the database manually, run:"
echo "  mysql -h $DB_HOST -u root -p < database-setup.sql"
echo ""
echo "Or create it with Docker:"
echo "  docker run -d --name ranaka-db \\"
echo "    -e MYSQL_ROOT_PASSWORD=root \\"
echo "    -e MYSQL_DATABASE=$DB_NAME \\"
echo "    -p 3306:3306 \\"
echo "    mysql:8.0"
echo ""
echo "Press Enter to continue..."
read

echo ""
echo "Starting Ranaka Backend"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Run the application
java -jar target/*.jar


