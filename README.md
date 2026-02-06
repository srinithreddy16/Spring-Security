# Spring Security REST API

A REST API built with **Spring Boot 4** and **Spring Security 7**, featuring JWT authentication, PostgreSQL persistence, and role-based access. Public endpoints allow registration and login; protected endpoints require a Bearer token or HTTP Basic credentials.

---

## Tech Stack

| Layer        | Technology                    |
| ------------ | ----------------------------- |
| Framework    | Spring Boot 4.0.2             |
| Security     | Spring Security 7, JWT (JJWT 0.12) |
| Data         | Spring Data JPA, Hibernate 7  |
| Database     | PostgreSQL                    |
| Build        | Maven                         |
| Java         | 21                            |

---

## Prerequisites

- **Java 21**
- **Maven 3.6+**
- **PostgreSQL** (e.g. 14+) running locally or remotely

---

## Configuration

1. **Database**  
   Create a database (e.g. `srinith1`) and set connection details in `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/srinith1
   spring.datasource.username=postgres
   spring.datasource.password=<your-password>
   ```

2. **Server port**  
   Default is `8082`. Override with `server.port` in `application.properties` or via environment variable.

3. **Optional**  
   For production, externalize secrets (DB password, etc.) via environment variables or a secret manager instead of storing them in `application.properties`.

---

## Running the Application

```bash
# Clone the repository (if applicable)
git clone <your-repo-url>
cd SpringSecurity

# Build
./mvnw clean install

# Run
./mvnw spring-boot:run
```

Or run the main class `com.srinith.SpringSecurity.SpringSecurityApplication` from your IDE.

The API base URL is: **`http://localhost:8082`** (unless you changed the port).

---

## API Overview

| Method | Endpoint      | Auth        | Description                    |
| ------ | ------------- | ----------- | ------------------------------ |
| GET    | `/`           | Optional    | Welcome message                |
| POST   | `/register`   | None        | Register a new user            |
| POST   | `/login`      | None        | Login; returns JWT token       |
| GET    | `/students`   | Required    | List all students              |
| POST   | `/students`   | Required    | Add a student                  |
| GET    | `/csrf-token` | Required    | Get CSRF token (if enabled)    |

- **Public:** `/register`, `/login` — no `Authorization` header.
- **Protected:** All other endpoints require either **Bearer JWT** or **HTTP Basic** (username/password).

---

## Postman Instructions

### 1. Register a user

- **Method:** `POST`
- **URL:** `http://localhost:8082/register`
- **Authorization:** `No Auth`
- **Headers:**  
  - `Content-Type: application/json`
- **Body (raw, JSON):**

  ```json
  {
    "id": 1,
    "username": "srinith",
    "password": "srinith123"
  }
  ```

- **Note:** `id` can be omitted if the DB generates it. Passwords are stored hashed (BCrypt).

---

### 2. Login and get JWT token

- **Method:** `POST`
- **URL:** `http://localhost:8082/login`
- **Authorization:** `No Auth`
- **Headers:**  
  - `Content-Type: application/json`
- **Body (raw, JSON):**

  ```json
  {
    "username": "srinith",
    "password": "srinith123"
  }
  ```

- **Response:** Plain string JWT in the response body. Copy this token for the next step.

---

### 3. Call a protected endpoint with JWT (Bearer token)

- **Method:** `GET` (example)
- **URL:** `http://localhost:8082/students`
- **Authorization:**
  - Type: **Bearer Token**
  - Token: paste the token from the login response
- **Headers:**  
  - No extra headers required if using the Authorization tab for Bearer Token.

Send the request; you should get `200 OK` and the list of students.

**Other protected examples:**

- `GET http://localhost:8082/` — welcome message  
- `POST http://localhost:8082/students` with body, e.g.:

  ```json
  { "id": 3, "name": "John", "marks": 75 }
  ```

  Use the same **Bearer Token** as above.

---

### 4. Optional: Use HTTP Basic instead of JWT

For the same protected endpoints you can use **Basic Auth**:

- **Authorization:** Type **Basic Auth**
- **Username:** same as registered (e.g. `srinith`)
- **Password:** same as registered (e.g. `srinith123`)

No need to use the token; Spring Security will accept Basic credentials for protected URLs.

---

### Quick checklist

1. **Register** → POST `/register` with JSON body, **No Auth**.  
2. **Login** → POST `/login` with JSON body, **No Auth** → copy token.  
3. **Protected** → Use **Bearer Token** (paste token) or **Basic Auth** for `/students`, `/`, etc.  
4. Ensure **Collection / Folder** auth is set to **No Auth** or **Inherit from parent** so that **No Auth** on the request is not overridden by a saved Basic Auth.

---

## Project Structure

```
src/main/java/com/srinith/SpringSecurity/
├── config/
│   ├── JwtFilter.java          # JWT validation filter
│   └── SecurityConfig.java      # Security rules, filter chain
├── controller/
│   ├── HelloController.java    # GET /
│   ├── StudentController.java  # /students, /csrf-token
│   └── UserController.java     # /register, /login
├── model/
│   ├── Student.java
│   ├── UserPrincipal.java      # UserDetails adapter
│   └── Users.java              # JPA user entity
├── repo/
│   └── UserRepo.java           # User repository
├── service/
│   ├── JWTService.java         # JWT create/validate
│   ├── MyUserDetailsService.java
│   └── UserService.java        # Register, login
└── SpringSecurityApplication.java
```

---

## License

This project is provided as-is for demonstration and learning purposes.
