# Request & Approval Workflow System

A production-ready Enterprise Request and Approval Workflow platform built with **Spring Boot 3**, **Spring Security 6**, **JWT Authentication**, and **MySQL**.

This system implements Role-Based Access Control (RBAC), multi-stage approval workflows, complete state machine transition validations, and immutable audit logging.

---

## Architecture Overview

```
                      +---------------------------------------+
                      |       Client / Swagger UI / Web       |
                      +-------------------+-------------------+
                                          |
                                    [Bearer JWT]
                                          v
+---------------------------------------------------------------------------------+
|                               Spring Filter Chain                               |
|   +-------------------------------------------------------------------------+   |
|   |                        JwtAuthenticationFilter                          |   |
|   +-------------------------------------------------------------------------+   |
+-----------------------------------------+---------------------------------------+
                                          |
                                          v
+---------------------------------------------------------------------------------+
|                                 REST API Layer                                  |
|   AuthController (/api/v1/auth)       |   RequestController (/api/v1/requests)  |
|   AdminController (/api/v1/admin)     |   GlobalExceptionHandler                |
+-----------------------------------------+---------------------------------------+
                                          |
                                          v
+---------------------------------------------------------------------------------+
|                                  Service Layer                                  |
|   AuthService (BCrypt + JWT)          |   RequestService (Workflow Engine)      |
|   AuditLogService (Immutable Trail)   |   UserService                           |
+-----------------------------------------+---------------------------------------+
                                          |
                                          v
+---------------------------------------------------------------------------------+
|                            Data Access Layer (JPA)                              |
|   UserRepository  |  RequestRepository (Filters)  |  RequestAuditLogRepository  |
+-----------------------------------------+---------------------------------------+
                                          |
                                          v
+---------------------------------------------------------------------------------+
|                                  MySQL 8.0                                      |
|   users  |  user_roles  |  requests  |  request_audit_logs                      |
+---------------------------------------------------------------------------------+
```

---

## Key Features

1. **Enterprise Security & Authentication**
   - Stateless JWT authentication via `io.jsonwebtoken` (JJWT 0.11.5).
   - Password hashing using `BCryptPasswordEncoder`.
   - Granular RBAC (`ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_ADMIN`).
   - Secured endpoint access using `@PreAuthorize` method-level security.

2. **Request Management & Workflow Lifecycle**
   - **Statuses**: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`.
   - **Priorities**: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
   - **Categories**: `EQUIPMENT`, `SOFTWARE_ACCESS`, `LEAVE`, `EXPENSE_REIMBURSEMENT`, `GENERAL`.
   - Dynamic filtering, sorting, and pagination via Spring Data `Pageable`.

3. **Approval Engine & Safeguards**
   - Strict state machine transitions (cannot approve/reject requests that are already finalized).
   - **Self-Approval Prevention**: Employees cannot approve their own requests.
   - **Manager Scope Enforcement**: Requests assigned to specific managers can only be evaluated by that manager or an administrator.
   - **Mandatory Rejection Comment**: Rejections require an explicit, audited rationale.
   - Complete immutable audit logging capturing actor, role, old status, new status, timestamp, and remarks.

4. **Production Configuration & Cloud Readiness**
   - Zero hardcoded passwords (credentials injected via environment variables).
   - Multi-stage `Dockerfile` and `docker-compose.yml` for containerized orchestration.
   - Interactive OpenAPI 3 / Swagger documentation (`/swagger-ui.html`).
   - In-memory H2 test suite that executes tests independently of MySQL.

---

## Role-Based Access Control Matrix

| Feature / Endpoint | Employee | Manager | Admin |
| :--- | :---: | :---: | :---: |
| Register / Login (`/api/v1/auth/**`) | Yes | Yes | Yes |
| Submit Request (`POST /api/v1/requests`) | Yes | Yes | Yes |
| View Own Requests (`GET /api/v1/requests`) | Yes | Yes | Yes |
| View Department Approval Queue | - | Yes | Yes |
| View All Requests System-wide | - | - | Yes |
| Update Pending Request (`PUT /api/v1/requests/{id}`) | Requester only | Requester only | Requester only |
| Cancel Request (`PUT /api/v1/requests/{id}/cancel`) | Requester only | Requester only | Requester only |
| Approve Request (`PUT /api/v1/requests/{id}/approve`) | - | Assigned Manager | Yes |
| Reject Request (`PUT /api/v1/requests/{id}/reject`) | - | Assigned Manager | Yes |
| View Audit Trail (`GET /api/v1/requests/{id}/history`) | Participants | Participants | Yes |
| Workflow Statistics (`GET /api/v1/admin/stats`) | - | - | Yes |
| User Directory (`GET /api/v1/admin/users`) | - | - | Yes |

---

## Pre-seeded Credentials (Development Mode)

Upon startup, `DataInitializer` seeds accounts for immediate testing:

| Username | Password | Role | Description |
| :--- | :--- | :--- | :--- |
| `admin` | `Admin@123` | `ROLE_ADMIN` | System administrator |
| `manager1` | `Manager@123` | `ROLE_MANAGER` | Engineering Manager |
| `employee1` | `Employee@123` | `ROLE_EMPLOYEE` | Engineering Team |
| `employee2` | `Employee@123` | `ROLE_EMPLOYEE` | Product Design Team |

---

## API Reference

### 1. Authentication (`/api/v1/auth`)

- **Register**: `POST /api/v1/auth/register`
  ```json
  {
    "username": "john_doe",
    "email": "john@example.com",
    "password": "Password@123",
    "fullName": "John Doe",
    "department": "Engineering",
    "roles": ["ROLE_EMPLOYEE"]
  }
  ```
- **Login**: `POST /api/v1/auth/login`
  ```json
  {
    "username": "employee1",
    "password": "Employee@123"
  }
  ```

### 2. Request Management (`/api/v1/requests`)

*Requires header: `Authorization: Bearer <TOKEN>`*

- **Create Request**: `POST /api/v1/requests`
  ```json
  {
    "title": "Dual 4K Monitor Setup",
    "description": "Required for multi-window development",
    "category": "EQUIPMENT",
    "priority": "HIGH"
  }
  ```
- **List / Filter Requests**: `GET /api/v1/requests?status=PENDING&page=0&size=10&sort=createdAt,desc`
- **Get Request Details**: `GET /api/v1/requests/{id}`
- **Update Request**: `PUT /api/v1/requests/{id}`
- **Approve Request**: `PUT /api/v1/requests/{id}/approve`
  ```json
  {
    "comment": "Approved within Q4 departmental hardware budget"
  }
  ```
- **Reject Request**: `PUT /api/v1/requests/{id}/reject`
  ```json
  {
    "comment": "Hardware budget exceeded for the current quarter"
  }
  ```
- **Cancel Request**: `PUT /api/v1/requests/{id}/cancel`
- **View Audit Trail**: `GET /api/v1/requests/{id}/history`

### 3. Admin Operations (`/api/v1/admin`)

*Requires `ROLE_ADMIN`*

- **Workflow Metrics**: `GET /api/v1/admin/stats`
- **All Users**: `GET /api/v1/admin/users`
- **All Managers**: `GET /api/v1/admin/managers`

---

## Local Development Setup

### Prerequisites
- Java 17+
- Maven 3.8+ (or use included `mvnw`)
- MySQL 8.0+ (or Docker)

### Environment Variables
Configure your database credentials in your shell or IDE run configuration:
```bash
export DB_URL="jdbc:mysql://localhost:3306/requestdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME="root"
export DB_PASSWORD="your_actual_password"
```

### Running Locally
```bash
# Compile and run
./mvnw spring-boot:run
```

Once started:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Interactive Web Client**: [http://localhost:8080/index.html](http://localhost:8080/index.html)

---

## Running with Docker Compose

Launch the entire stack (MySQL 8 + Spring Boot API) in one command:
```bash
docker-compose up --build
```

---

## Running Tests

Execute the full suite of unit and integration tests:
```bash
./mvnw test
```
The test suite utilizes in-memory H2 and MockMvc to test authentication, RBAC authorization, and workflow state transitions.
