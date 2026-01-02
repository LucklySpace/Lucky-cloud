# Lucky Cloud Scheduler

Distributed Task Scheduler Module based on Spring Boot 3, Quartz, and JPA.

## Features

- **Distributed Scheduling**: Uses Quartz JDBC JobStore with clustering support.
- **Task Management**: Add, Edit, Delete, Start, Stop, Trigger tasks.
- **Schedule Types**: Cron, Fixed Rate, Fixed Delay.
- **Concurrency Control**: Serial (DisallowConcurrentExecution) or Parallel execution per task.
- **Monitoring**: Execution logs, success/failure status, duration.
- **Security**: Role-based access control (ADMIN/OPS).
- **UI**: Thymeleaf + Tailwind CSS for easy management.

## Architecture

- **Controller**: `TaskController` (API), `PageController` (UI).
- **Service**: `TaskService` manages Quartz Scheduler and Metadata.
- **Domain**: `TaskInfo` stores task definitions; `TaskLog` stores execution history.
- **Job**: `ParallelJob` and `SerialJob` wrapper classes that execute the target logic.

## Prerequisites

- Java 21
- PostgreSQL (Default) or MySQL
- Maven

## Setup

1. **Database Initialization**:
    - Create a database `im_scheduler`.
    - Run `src/main/resources/sql/init_postgres.sql` (for Postgres) or `init_mysql.sql`.
    - The application will automatically create `scheduler_task_info` and `scheduler_task_log` tables via JPA.

2. **Configuration**:
    - Edit `src/main/resources/application.yml` to set your database credentials.

3. **Run**:
   ```bash
   mvn spring-boot:run
   ```

4. **Access**:
    - UI: `http://localhost:9090/`
    - Login: `admin/admin` or `ops/ops`
    - Swagger API: `http://localhost:9090/doc.html` or `/swagger-ui.html`

## Creating a Job

1. Go to **New Task**.
2. **Job Class**: Enter the fully qualified class name of a Bean or Class that has a `execute(String)` method or
   implements `Runnable`.
    - Example: Create a Bean in the project and use its class name.
3. **Schedule**: Choose Cron or Fixed Rate.
4. **Concurrency**: Select Serial if you don't want the same job running multiple times simultaneously.

## Deployment

- This module is part of the Lucky-cloud project.
- Can be deployed as a standalone JAR or containerized.
- For clustering, ensure all nodes point to the same database and have time synchronized.

