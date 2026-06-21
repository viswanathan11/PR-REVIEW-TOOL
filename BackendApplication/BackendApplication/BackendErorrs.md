# Backend Errors Log

This document lists the resolved logical, conceptual, and architectural issues encountered during the development of the Spring Boot backend application.

---

## 1. GitHub OAuth 404 Error Page

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

## 2. JWT Signing Key Security Failure (WeakKeyException)

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

## 3. Unsatisfied Dependency: GitHubService Bean Not Found

> [!CAUTION]
> **Error Symptom:**
> Spring Boot application failed to start with a crash log:
> ```
> Parameter 1 of constructor in com.example.BackendApplication.controller.RepositoryController required a bean of type 'com.example.BackendApplication.service.GitHubService' that could not be found.
> ```

> [!IMPORTANT]
> **Root Cause:**
> The `GitHubService` class was defined without any Spring stereotype annotation (like `@Service` or `@Component`). Therefore, Spring's component scanner ignored the class, meaning no bean of `GitHubService` was registered in the Application Context.

> [!TIP]
> **Resolution:**
> Added `@Service` annotation directly above the `GitHubService` class declaration and imported `org.springframework.stereotype.Service` to ensure Spring registers the bean.

---

## 4. GitHub API Return empty/null and 500 errors

> [!CAUTION]
> **Error Symptom:**
> 1. Initially, the frontend threw `SyntaxError: Unexpected end of JSON input` when calling `/api/repos/github`.
> 2. After debugging, it was found that the call succeeded with `200 OK` but returned `null` (an empty response body).
> 3. After adding error checking, the server returned `500 Internal Server Error`.

> [!IMPORTANT]
> **Root Cause:**
> There were two issues preventing GitHub communication:
> 1. **Credential Loss on Redirect:** The API URL requested was `http://api.github.com/user/repos` (unsecured `http`). GitHub returned a 301 redirect to `https://api.github.com/user/repos`. During this redirect, Spring's `RestTemplate` stripped the `Authorization` header to prevent credentials leakage across protocols, making the request unauthenticated.
> 2. **Missing User-Agent:** The GitHub API requires all requests to include a `User-Agent` header. Without it, requests are rejected with a `403 Forbidden`.

> [!TIP]
> **Resolution:**
> 1. Updated the GitHub API URL to use secure `https://api.github.com/user/repos` directly so that headers are preserved without redirects.
> 2. Added the `User-Agent: AI-PR-Reviewer-App` header to the HTTP request headers inside `GitHubService.java`.

---

## 5. CSRF Redirect and CORS Policy Block on Webhook Toggle

> [!CAUTION]
> **Error Symptom:**
> When clicking the "Enable Review" or "Disable Review" buttons, the frontend dashboard console logged a CORS error:
> `Access to fetch at 'https://github.com/login/oauth/authorize?response_type=code...' from origin 'http://localhost:5173' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.`
> And the network tab showed that `POST /api/repos/1/webhook` returned a 302 redirect.

> [!IMPORTANT]
> **Root Cause:**
> Spring Security enforces CSRF protection by default for state-changing HTTP requests (POST, PUT, DELETE) unless explicitly configured otherwise.
> 1. Because our React frontend interacts with the backend using HttpOnly cookie credentials without passing a CSRF token, Spring's CSRF filter intercepted the POST and DELETE requests to toggle the webhook and threw a CSRF mismatch error.
> 2. By default, Spring Security handles this exception by redirecting the client to the `/error` endpoint.
> 3. However, since the client was unauthenticated at `/error` (or because of the filter chain structure), it redirected the browser to the GitHub OAuth2 authorization URL (`https://github.com/login/oauth/authorize...`).
> 4. Since the browser made an asynchronous AJAX `fetch()` call, it followed the redirect and attempted to request the GitHub authorization URL directly. GitHub's OAuth endpoint does not allow CORS requests from local origins, causing the browser to block the request.

> [!TIP]
> **Resolution:**
> Configured Spring Security to ignore CSRF checks for all API endpoints `/api/**` in `SecurityConfig.java`. Since our API endpoints use HttpOnly cookies for custom session verification or are designed as API endpoints, we bypass CSRF checks for these paths:
> ```java
> .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
> ```

---

## 6. ClassCastException on Request Body Parsing due to Type Erasure

> [!CAUTION]
> **Error Symptom:**
> When adding a repository to track (clicking the repository card on the frontend), the request failed with a `500 Internal Server Error`, and the backend logs printed a `ClassCastException`:
> `java.lang.ClassCastException: class java.lang.Boolean cannot be cast to class java.lang.CharSequence`

> [!IMPORTANT]
> **Root Cause:**
> Inside `RepositoryController.java`, the request handler mapped the incoming JSON request body to `Map<String, String> body`.
> 1. In JSON, the `private` field is a boolean (`"private": true`).
> 2. Under the hood, Jackson (Spring's JSON deserializer) parses JSON values into native Java types (e.g. `Boolean` for JSON booleans, `String` for strings).
> 3. Because of Java type erasure, the compiler does not enforce the generic type constraints of `Map<String, String>` at runtime. The JVM allowed Jackson to put a `Boolean` value inside the map.
> 4. When we attempted to extract the value using `body.get("private")` and cast it (or implicitly convert it) to a `String` (e.g., `String.valueOf(body.get("private"))`), the JVM threw a `ClassCastException` because a `Boolean` object cannot be cast to a `String` or `CharSequence`.

> [!TIP]
> **Resolution:**
> Changed the map type to `Map<String, Object> body` in the controller's signature, and implemented a safe dynamic type-check/cast block:
> ```java
> final Boolean isPrivate;
> Object privateObj = body.get("private");
> if (privateObj instanceof Boolean) {
>     isPrivate = (Boolean) privateObj;
> } else if (privateObj instanceof String) {
>     isPrivate = Boolean.parseBoolean((String) privateObj);
> } else {
>     isPrivate = false; // Fallback
> }
> ```

---

## 7. Lambda Variable Constraints: Effectively Final Constraint

> [!CAUTION]
> **Error Symptom:**
> Compiler error when compiling `RepositoryController.java`:
> `Local variable isPrivate defined in an enclosing scope must be final or effectively final`

> [!IMPORTANT]
> **Root Cause:**
> Java's lambda expressions (such as the one passed to `.orElseGet()` when setting up the repository entity) can only access variables from their outer scope if those variables are `final` or "effectively final" (i.e. declared and never reassigned after declaration).
> In the initial implementation, the variable `Boolean isPrivate = false;` was declared and then re-assigned inside an `if-else` block depending on the type of the incoming payload. Because `isPrivate` was reassigned, the compiler rejected its usage inside the `.orElseGet(() -> { ... newRepo.setIsPrivate(isPrivate); ... })` lambda.

> [!TIP]
> **Resolution:**
> Modified the control flow to ensure that `isPrivate` is assigned exactly once. We declared the variable as `final Boolean isPrivate` and structured the `if-else` blocks so that every path assigns to it exactly once. Since it is never reassigned, the compiler recognized it as final and permitted its usage inside the lambda block.

---

## 8. Localhost Webhook Rejection and Tunneling

> [!CAUTION]
> **Error Symptom:**
> Toggling the review webhook failed with:
> `Failed to enable webhook on GitHub. Ensure your OAuth Application has 'admin:repo_hook' permissions.`
> Checking the backend logs showed that GitHub API returned a `422 Unprocessable Entity` with details that the webhook URL was invalid.

> [!IMPORTANT]
> **Root Cause:**
> The backend application's default base URL was configured to `http://localhost:8081` in the `.env` file (`APP_BASE_URL`). When registering a webhook, our application requested GitHub to send pull request events to `http://localhost:8081/api/webhooks/github`.
> GitHub's webhook dispatcher runs on the public internet. It cannot resolve or deliver HTTP requests to `localhost` or local network IP addresses (`127.0.0.1`). Thus, GitHub's API validation rejects local webhook URLs with a `422` status code.

> [!TIP]
> **Resolution:**
> 1. Set up a secure, public HTTP tunnel using a tool like `localhost.run` by running:
>    `ssh -R 80:localhost:8081 nokey@localhost.run`
>    This allocates a public HTTPS URL (e.g. `https://2c000fbb0fa22c.lhr.life`) that proxies traffic directly to the local port `8081`.
> 2. Updated the `APP_BASE_URL` inside the `.env` file to point to this public URL:
>    ```env
>    APP_BASE_URL=https://2c000fbb0fa22c.lhr.life
>    ```
>    *Note: The tunnel must remain running during developer review sessions to receive live events from GitHub.*

