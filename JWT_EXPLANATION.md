# JWT Concept and Flow Explanation

## 📚 What is JWT?

**JWT (JSON Web Token)** is like a **digital ID card** that proves who you are without storing your password on the server.

### Key Concepts:

1. **Stateless**: The server doesn't need to store sessions. The token itself contains all the info needed.
2. **Self-contained**: The token has 3 parts separated by dots (`.`):
   ```
   eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzcmluaXRoIiwiZXhwIjoxNzA3MDAwMDAwfQ.signature
   ```
   - **Header** (first part): Algorithm used (HMAC SHA256)
   - **Payload** (middle part): Data like username, expiration time
   - **Signature** (last part): Ensures token wasn't tampered with

3. **Signed**: The server signs the token with a secret key. If someone changes the token, the signature won't match and it's rejected.

--- 

## 🔄 Complete Flow in Your Project

### **Phase 1: Registration** (One-time setup)

``` 
User → POST /register → UserController.register()
                      → UserService.register()
                      → Password hashed with BCrypt
                      → Saved to PostgreSQL database
```

**Code Flow:**
1. **UserController.register()** receives `{username, password}`
2. **UserService.register()**:
   - Hashes password: `encoder.encode(user.getPassword())`
   - Saves to DB: `repo.save(user)`
3. User is now in the database with a hashed password

---

### **Phase 2: Login** (Get JWT Token)

```
User → POST /login → UserController.login()
                   → UserService.verify()
                   → AuthenticationManager checks credentials
                   → JWTService.generateToken()
                   → Returns JWT token string
```

**Step-by-Step Code Walkthrough:**

#### Step 1: UserController.login()
```java
POST /login with {username: "srinith", password: "srinith123"}
↓
UserController.login() receives the request
↓
Calls service.verify(user)
```

#### Step 2: UserService.verify()
```java
public String verify(Users user) {
    // Create authentication token with username & password
    Authentication authentication = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            user.getUsername(), 
            user.getPassword()
        )
    );
    
    // If authentication succeeds (password matches DB)
    if(authentication.isAuthenticated()) {
        // Generate JWT token with username
        return jwtservice.generateToken(user.getUsername());
    }
}
```

**What happens here:**
- `AuthenticationManager` compares the provided password with the hashed password in DB
- If they match → `isAuthenticated()` returns `true`
- Then we call `generateToken()` to create a JWT

#### Step 3: JWTService.generateToken()
```java
public String generateToken(String username) {
    Map<String, Object> claims = new HashMap<>();
    return Jwts.builder()
        .claims(claims)                    // Empty claims map (can add roles, etc.)
        .subject(username)                 // Set username as "subject"
        .issuedAt(new Date(...))           // When token was created
        .expiration(new Date(... + 30hrs)) // Token expires in 30 hours
        .signWith(getKey())                // Sign with secret key
        .compact();                        // Convert to string
}
```

**What getKey() does:**
```java
private SecretKey getKey() {
    // Decode the Base64 secret key stored in memory
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    // Create HMAC SHA256 key for signing
    return Keys.hmacShaKeyFor(keyBytes);
}
```

**The secret key:**
- Generated once when `JWTService` is created (in constructor)
- Stored in memory as Base64 string
- Used to sign AND verify tokens
- **Important**: Same key must be used for both signing and verification

**Result:** A JWT string like:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzcmluaXRoIiwiZXhwIjoxNzA3MDAwMDAwfQ.abc123...
```

#### Step 4: Token returned to user
```java
return ResponseEntity.ok(token);  // Returns plain string token
```

**User receives:** `"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."`

---

### **Phase 3: Using JWT Token** (For Protected Endpoints)

```
User → GET /students
     → Header: Authorization: Bearer <token>
     → JwtFilter intercepts request
     → Extracts token, validates it
     → Sets SecurityContext
     → Request proceeds to StudentController
```

**Step-by-Step Code Walkthrough:**

#### Step 1: Request arrives with Bearer token
```
GET /students
Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Step 2: JwtFilter.doFilterInternal() runs (BEFORE the controller)

**Why JwtFilter runs first?**
- In `SecurityConfig`, we have:
  ```java
  http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
  ```
- This means `JwtFilter` runs **before** Spring Security's default authentication

**JwtFilter code:**
```java
protected void doFilterInternal(...) {
    // Step 2a: Extract token from header
    String authHeader = request.getHeader("Authorization");
    String token = null;
    String username = null;
    
    if(authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);  // Remove "Bearer " prefix
        username = jwtService.extractUserName(token);  // Extract username from token
    }
    
    // Step 2b: If we have username AND no authentication is set yet
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        // Load user from database
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        // Step 2c: Validate token
        if(jwtService.validateToken(token, userDetails)) {
            // Step 2d: Create authentication object
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null,  // No credentials needed (already verified)
                    userDetails.getAuthorities()  // User's roles/permissions
                );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Step 2e: Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }
    
    // Step 2f: Continue to next filter/controller
    filterChain.doFilter(request, response);
}
```

#### Step 3: JWTService.extractUserName()
```java
public String extractUserName(String token) {
    return extractClaim(token, Claims::getSubject);
}
```

**What extractAllClaims() does:**
```java
private Claims extractAllClaims(String token) {
    return Jwts.parser()                    // Create parser
        .verifyWith(getKey())               // Use same secret key to verify signature
        .build()                            // Build parser
        .parseSignedClaims(token)           // Parse and verify token
        .getPayload();                      // Extract the payload (claims)
}
```

**This does 3 things:**
1. **Parses** the token (splits header.payload.signature)
2. **Verifies signature** (checks if token was tampered with)
3. **Extracts payload** (gets the data inside: username, expiration, etc.)

**If token is invalid:**
- Wrong signature → throws exception
- Expired → `isTokenExpired()` returns `true`
- Malformed → throws exception

#### Step 4: JWTService.validateToken()
```java
public boolean validateToken(String token, UserDetails userDetails) {
    final String userName = extractUserName(token);  // Get username from token
    return userName.equals(userDetails.getUsername())  // Match with DB username
        && !isTokenExpired(token);                    // Check expiration
}
```

**Checks:**
1. Username in token matches username from database
2. Token hasn't expired (`expiration` date is in the future)

#### Step 5: SecurityContext is set
```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

**What this does:**
- Spring Security now knows: "This request is authenticated as user 'srinith'"
- Other parts of the app can check: `SecurityContextHolder.getContext().getAuthentication()`
- Protected endpoints will allow the request

#### Step 6: Request continues to controller
```java
filterChain.doFilter(request, response);
```

Now `StudentController.getStudents()` runs, and Spring Security sees the user is authenticated, so it allows access.

---

## 🔐 Security Flow Diagram

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ 1. POST /register {username, password}
       ▼
┌──────────────────┐
│ UserController   │
└──────┬───────────┘
       │
       │ 2. UserService.register()
       ▼
┌──────────────────┐      ┌──────────────┐
│ BCrypt Hash      │─────▶│ PostgreSQL   │
│ Password         │      │ (Save user)  │
└──────────────────┘      └──────────────┘

       │
       │ 3. POST /login {username, password}
       ▼
┌──────────────────┐
│ UserController   │
└──────┬───────────┘
       │
       │ 4. UserService.verify()
       ▼
┌──────────────────────────┐
│ AuthenticationManager    │
│ (Compare password)       │
└──────┬───────────────────┘
       │
       │ 5. If valid → JWTService.generateToken()
       ▼
┌──────────────────┐
│ JWT Token        │
│ (signed string)  │
└──────┬───────────┘
       │
       │ 6. Return token to client
       ▼
┌─────────────┐
│   Client    │
│ (stores token)
└──────┬──────┘
       │
       │ 7. GET /students
       │    Header: Authorization: Bearer <token>
       ▼
┌──────────────────┐
│   JwtFilter      │ ◀─── Runs BEFORE controller
└──────┬───────────┘
       │
       │ 8. Extract & validate token
       ▼
┌──────────────────┐
│ JWTService       │
│ - extractUserName│
│ - validateToken  │
└──────┬───────────┘
       │
       │ 9. If valid → Set SecurityContext
       ▼
┌──────────────────────────┐
│ SecurityContextHolder    │
│ (User authenticated)     │
└──────┬───────────────────┘
       │
       │ 10. Continue to controller
       ▼
┌──────────────────┐
│StudentController │
│ (Access granted) │
└──────────────────┘
```

---

## 🎯 Key Points to Remember

### 1. **Secret Key**
- Generated once when app starts (`JWTService` constructor)
- Used to **sign** tokens (when creating) and **verify** tokens (when validating)
- Must be the same key for both operations
- Stored in memory (lost when app restarts - this is OK for demo, but in production you'd store it securely)

### 2. **Token Structure**
```
Header.Payload.Signature
```
- **Header**: Algorithm info (HMAC SHA256)
- **Payload**: Username, expiration, issued date
- **Signature**: Ensures token wasn't modified

### 3. **Why JWT?**
- **Stateless**: Server doesn't store sessions
- **Scalable**: Works across multiple servers
- **Self-contained**: Token has all info needed
- **Secure**: Signed so it can't be tampered with

### 4. **Token Lifecycle**
1. **Created** during login (valid for 30 hours)
2. **Sent** by client in `Authorization: Bearer <token>` header
3. **Validated** by `JwtFilter` on every protected request
4. **Expires** after 30 hours (client must login again)

### 5. **Filter Chain Order**
```
Request → JwtFilter → UsernamePasswordAuthenticationFilter → Controller
```
- `JwtFilter` runs first
- If JWT is valid, sets `SecurityContext`
- If no JWT or invalid, falls back to Basic Auth (UsernamePasswordAuthenticationFilter)

---

## 🧪 Testing the Flow

### Test 1: Register
```bash
POST http://localhost:8082/register
Body: {"username": "srinith", "password": "srinith123"}
```
**Result:** User saved to DB with hashed password

### Test 2: Login
```bash
POST http://localhost:8082/login
Body: {"username": "srinith", "password": "srinith123"}
```
**Result:** Returns JWT token string

### Test 3: Use Token
```bash
GET http://localhost:8082/students
Header: Authorization: Bearer <paste-token-here>
```
**Result:** Returns list of students (if token is valid)

### Test 4: Expired Token
```bash
# Wait 30+ hours, then try same request
GET http://localhost:8082/students
Header: Authorization: Bearer <old-token>
```
**Result:** 401 Unauthorized (token expired)

---

## 💡 Common Questions

**Q: What if someone steals my token?**  
A: Tokens expire (30 hours). Also, use HTTPS in production so tokens aren't intercepted.

**Q: Can I change the expiration time?**  
A: Yes! In `JWTService.generateToken()`, change:
```java
.expiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000 * 30))
```
The `30` is hours. Change to `24` for 24 hours, `1` for 1 hour, etc.

**Q: Why do we need both JWT and Basic Auth?**  
A: Your config has both:
- **JWT** (via `JwtFilter`) - for API clients
- **Basic Auth** (via `httpBasic()`) - fallback or for different clients

**Q: What happens if token signature is wrong?**  
A: `parseSignedClaims()` throws an exception, `validateToken()` returns false, and request is rejected.

**Q: Where is the secret key stored?**  
A: Currently in memory (generated at startup). In production, store it in:
- Environment variables
- Secret management service (AWS Secrets Manager, HashiCorp Vault)
- Encrypted config file

---

## 📝 Summary

**JWT Flow in 3 Steps:**
1. **Login** → Server validates credentials → Returns JWT token
2. **Request** → Client sends token in `Authorization: Bearer <token>` header
3. **Filter** → Server validates token → Sets SecurityContext → Allows access

**Key Classes:**
- `JWTService`: Creates and validates tokens
- `JwtFilter`: Intercepts requests, validates tokens, sets SecurityContext
- `UserService`: Handles registration and login
- `SecurityConfig`: Configures filter chain

This is a **stateless authentication** system - the server doesn't remember who logged in. The token itself proves identity!
