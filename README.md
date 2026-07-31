# Request & Approval Workflow System

A role-based backend application that manages approval processes within an organization. Employees can submit requests, and authorized managers can approve or reject them.

## Architecture

This project follows a clean, organized **Layered Architecture** (Frontend → Controller → Service → Repository → Database):

* **Frontend (`src/main/resources/static/index.html`)**: A simple, vanilla JavaScript/HTML user interface.
* **Controller (`controller/`)**: Exposes RESTful APIs and handles HTTP requests.
* **Service (`service/`)**: Contains the core business logic (validating roles, changing statuses).
* **Repository (`repository/`)**: Interfaces with the MySQL database using Spring Data JPA.
* **Entity (`entity/`)**: Defines the database schemas (User, ApprovalRequest).
* **DTO (`dto/`)**: Data Transfer Objects used to securely receive incoming request data.
* **Enums (`enums/`)**: Constants for User Roles and Request Statuses.

## Prerequisites

- Java 17 or higher
- MySQL Server running on localhost (port 3306)
- Maven (optional, wrapper is included)

## Database Setup

1. Open your MySQL client and run:
   ```sql
   CREATE DATABASE requestdb;
   ```
2. The application is configured to connect with the username `root` and password `8359`. If your credentials differ, update them in `src/main/resources/application.properties`.

## Running the Application

1. Open the project in your IDE (IntelliJ IDEA, Eclipse, VS Code).
2. Run `RequestApprovalWorkflowSystemApplication.java` as a Java application.
3. The server will start on `http://localhost:8080`.
4. *Note: Upon the first run, the system automatically creates two test users in the database (`employee1` and `manager1`).*

## API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/requests` | Create a new request (Requires Employee ID) |
| `POST` | `/api/requests/{id}/action` | Approve/Reject a request (Requires Manager ID) |
| `GET` | `/api/requests` | Fetch all requests |
| `GET` | `/api/requests/employee/{id}` | Fetch requests for a specific employee |

## Testing the UI

Once the application is running, open your web browser and navigate to:
`http://localhost:8080/index.html`

You will see a dashboard where you can simulate logging in as an employee to submit requests, or as a manager to approve/reject them.
