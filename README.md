# Salary Processor Backend

A Spring Boot application that processes salary data from Excel files based on attendance records. This backend provides APIs for uploading Excel files, processing salary data, and generating PDF salary slips.

## Features

- Excel file parsing for attendance data
- Integration with Contentful CMS for employee data
- Salary computation based on business rules
- PDF generation for individual salary slips
- JWT-based authentication to secure endpoints
- Deployable to Render (free tier)

## Business Logic

- Ignore rows where Status = WO (Week Off)
- Expected Hours = (WOP + P + A - paidLeaves) × 8
- Actual Hours = Sum of worked hours
- Late Mark Rule:
  - If InTime > 10:00 AM, count as a late mark
  - First 3 late marks = 0.5 day salary cut
  - From 4th onward: 0.5 / 3 day cut per late mark
- Coefficient = (actualHours / expectedHours) - lateMarkPenalty
- Final Salary = coefficient × fixedMonthlySalary

## Prerequisites

- Java 11+
- Maven 3.6+
- Contentful account with employee data

## Environment Variables

- `CONTENTFUL_SPACE_ID`: Your Contentful space ID
- `CONTENTFUL_ACCESS_TOKEN`: Your Contentful access token
- `CONTENTFUL_ENVIRONMENT`: Contentful environment (default: master)
- `JWT_SECRET`: Secret key for JWT token generation

## Project Structure

- `src/main/java/com/salaryprocessor/controller`: API endpoints
- `src/main/java/com/salaryprocessor/model`: Data models
- `src/main/java/com/salaryprocessor/service`: Business logic
- `src/main/java/com/salaryprocessor/security`: JWT authentication
- `src/main/java/com/salaryprocessor/config`: Application configuration
- `src/main/resources`: Configuration files

## API Endpoints

- `POST /api/auth/login`: Authenticate user and get JWT token
- `POST /api/salary/process`: Process Excel file and return salary data
- `POST /api/salary/generate-pdf`: Generate PDF salary slips as ZIP file
- `POST /api/salary/process-with-pdf`: Process salary data and return both JSON results and PDF data

## Setup and Installation

1. Clone the repository
2. Configure environment variables
3. Build the project:
   ```
   mvn clean package
   ```
4. Run the application:
   ```
   java -jar target/backend-0.0.1-SNAPSHOT.jar
   ```

## Contentful Setup

Create a Content Model in Contentful with the following fields:
- Content Type: `employee`
- Fields:
  - `employeeId` (Text)
  - `name` (Text)
  - `monthlySalary` (Number)

## Deployment to Render

1. Push the code to a Git repository
2. In Render, create a new Web Service
3. Connect to your Git repository
4. Select the "Docker" environment
5. Configure environment variables
6. Deploy

## Authentication

The application uses JWT tokens for authentication. Default credentials:
- Username: `admin`
- Password: `admin123`
