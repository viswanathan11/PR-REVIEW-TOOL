# Backend Errors Log

This document lists the resolved errors and issues encountered in the Spring Boot backend application.

---

## 1. Maven Plugin Prefix Resolution Error

### Symptom
When starting the Spring Boot server, Maven build failed with:
```
[ERROR] No plugin found for prefix 'sping-boot' in the current project and in the plugin groups
```

### Cause
A typographical error in the Maven command: typing `sping-boot:run` (missing the letter `r` in `spring`).

### Solution
Corrected the command to:
```bash
./mvnw spring-boot:run
```

---

## 2. GitHub OAuth 404 Error Page

### Symptom
Clicking **Sign in with GitHub** immediately redirected the browser to a GitHub **404 Not Found** page.

### Cause
The backend `.env` was using the default placeholder Client ID (`Ov231ifbX086RrV1kSKc`) and a mismatched Client Secret. Because GitHub did not recognize the client ID, it failed the authorization request and displayed a 404.

### Solution
1. Registered a new OAuth Application on [GitHub Developer Settings](https://github.com/settings/developers).
2. Set the Authorization Callback URL to: `http://localhost:8081/login/oauth2/code/github`.
3. Set the Homepage URL to: `http://localhost:5173`.
4. Copied the generated Client ID and Client Secret into the backend's `.env` file:
   ```env
   GITHUB_CLIENT_ID=Ov23liRUEEt3DYQljBZd
   GITHUB_CLIENT_SECRET=your_newly_generated_client_secret
   ```

---

## 3. JWT Signing Key Security Failure (WeakKeyException)

### Symptom
Upon successful GitHub authorization, the backend crashed with a 500 error page showing:
```
io.jsonwebtoken.security.WeakKeyException: The specified key byte array is 104 bits which is not secure enough for any JWT HMAC-SHA algorithm.
```

### Cause
The environment variable `JWT_SECRET` was missing from the backend `.env` file. As a result, Spring Boot resolved the property placeholder literal `${JWT_SECRET}` as the exact string `"${JWT_SECRET}"`. Since this string contains exactly 13 characters (13 bytes = 104 bits), the JSON Web Token library (`jjwt`) rejected it as too weak for HMAC-SHA256 (which requires at least 256 bits / 32 bytes).

### Solution
Added `JWT_SECRET` to the backend `.env` file with a secure value of at least 32 characters:
```env
JWT_SECRET=super_secret_key_that_is_at_least_32_characters_long
```

---

## 4. Syntax Typo in application.yml properties

### Symptom
Spring Boot config parsing or application properties failed.

### Cause
Accidentally added extraneous brackets `]]}` to the end of the `base-url` property:
```yaml
base-url: ${APP_BASE_URL:http://localhost:8081]]}
```

### Solution
Cleaned up the syntax back to:
```yaml
base-url: ${APP_BASE_URL:http://localhost:8081}
```
