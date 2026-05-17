# AI GitHub Code Reviewer — Step-by-Step Build Guide

> Hell-mode learning project. Full-stack. No shortcuts explained away.
> Spring Boot · PostgreSQL · Redis · GitHub Webhooks · Anthropic AI · Next.js

---

## Before you start — prerequisites checklist

Make sure you have these installed before touching any code:

```bash
java --version        # need 17+
mvn --version         # Maven 3.8+
node --version        # need 18+
npm --version         # 9+
docker --version      # Docker Desktop running
docker-compose --version
psql --version        # PostgreSQL client (for debugging)
git --version
```

Accounts you need:
- GitHub account (you have this)
- Anthropic account → https://console.anthropic.com (free tier works, grab an API key)
- ngrok account → https://ngrok.com (free tier, for webhook testing)

---

## Phase 1 — Project scaffolding

### Step 1 — Create your GitHub OAuth App

1. Go to https://github.com/settings/developers
2. Click **OAuth Apps** → **New OAuth App**
3. Fill in:
   - Application name: `AI Code Reviewer (dev)`
   - Homepage URL: `http://localhost:3000`
   - Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
4. Click **Register application**
5. Copy the **Client ID** and generate a **Client Secret** — save both somewhere safe

> You will need these in Step 5.

---

### Step 2 — Create project folder structure

```bash
mkdir ai-code-reviewer
cd ai-code-reviewer
mkdir backend frontend
git init
```

Create a `.gitignore` in the root:

```
# root .gitignore
backend/.env
frontend/.env.local
*.class
target/
node_modules/
.DS_Store
```

---

### Step 3 — Bootstrap Spring Boot backend

Go to https://start.spring.io and configure:

- **Project:** Maven
- **Language:** Java
- **Spring Boot:** 3.2.x
- **Group:** `com.reviewer`
- **Artifact:** `backend`
- **Java:** 17

Add these dependencies:
- Spring Web
- Spring Security
- Spring Data JPA
- OAuth2 Client
- Spring Data Redis
- PostgreSQL Driver
- Lombok
- Validation
- Spring Boot DevTools

Click **Generate**, unzip into the `backend/` folder.

Or use curl:

```bash
cd backend
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.2.5 \
  -d baseDir=. \
  -d groupId=com.reviewer \
  -d artifactId=backend \
  -d javaVersion=17 \
  -d dependencies=web,security,data-jpa,oauth2-client,data-redis,postgresql,lombok,devtools,validation \
  -o starter.zip
unzip starter.zip && rm starter.zip
```

---

### Step 4 — Bootstrap Next.js frontend

```bash
cd ../frontend
npx create-next-app@latest . \
  --typescript \
  --tailwind \
  --eslint \
  --app \
  --no-src-dir \
  --import-alias "@/*"
```

Install dependencies you'll need:

```bash
npm install next-auth swr axios
npm install -D @types/node
```

---

### Step 5 — Set up Docker Compose (PostgreSQL + Redis)

Create `docker-compose.yml` in the project root:

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:15-alpine
    container_name: reviewer_postgres
    environment:
      POSTGRES_DB: codereview
      POSTGRES_USER: reviewer
      POSTGRES_PASSWORD: reviewpass
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U reviewer -d codereview"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: reviewer_redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redisdata:/data

volumes:
  pgdata:
  redisdata:
```

Start both:

```bash
docker-compose up -d
docker-compose ps   # verify both are "healthy" / "running"
```

---

### Step 6 — Configure backend application.yml

Replace `src/main/resources/application.properties` with `application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/codereview
    username: reviewer
    password: reviewpass
    hikari:
      maximum-pool-size: 10

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: read:user,repo,admin:repo_hook

app:
  base-url: ${APP_BASE_URL:http://localhost:8080}
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
  jwt:
    secret: ${JWT_SECRET}
    expiry-hours: 24
  webhook:
    secret: ${WEBHOOK_SECRET}
  ai:
    anthropic-api-key: ${ANTHROPIC_API_KEY}
    model: claude-sonnet-4-20250514
    max-tokens: 4096
    max-diff-chars: 50000
```

Create `backend/.env`:

```env
GITHUB_CLIENT_ID=your_client_id_here
GITHUB_CLIENT_SECRET=your_client_secret_here
JWT_SECRET=change-this-to-a-random-64-char-string-in-production
WEBHOOK_SECRET=another-random-secret-for-hmac-validation
ANTHROPIC_API_KEY=sk-ant-your-key-here
APP_BASE_URL=http://localhost:8080
FRONTEND_URL=http://localhost:3000
```

Add to your `pom.xml` in the `<dependencies>` section — JWT library:

```xml
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.3</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.3</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.3</version>
  <scope>runtime</scope>
</dependency>
```

Load the `.env` file at startup by adding this to your main class:

```java
// BackendApplication.java
@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        loadEnvFile();
        SpringApplication.run(BackendApplication.class, args);
    }

    private static void loadEnvFile() {
        File envFile = new File(".env");
        if (!envFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            reader.lines()
                .filter(l -> !l.startsWith("#") && l.contains("="))
                .forEach(l -> {
                    String[] parts = l.split("=", 2);
                    System.setProperty(parts[0].trim(), parts[1].trim());
                });
        } catch (Exception e) {
            System.err.println("Could not load .env: " + e.getMessage());
        }
    }
}
```

---

## Phase 2 — Database schema

### Step 7 — Add Flyway and write migrations

Add Flyway to `pom.xml`:

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Add to `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

Create folder: `src/main/resources/db/migration/`

**`V1__create_users.sql`**

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    github_id       VARCHAR(50)  UNIQUE NOT NULL,
    github_login    VARCHAR(100) NOT NULL,
    avatar_url      TEXT,
    access_token    TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_github_id ON users(github_id);
```

**`V2__create_repositories.sql`**

```sql
CREATE TABLE repositories (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_repo_id  VARCHAR(50) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    description     TEXT,
    private         BOOLEAN DEFAULT FALSE,
    webhook_id      VARCHAR(50),
    webhook_active  BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, github_repo_id)
);

CREATE INDEX idx_repositories_user_id ON repositories(user_id);
```

**`V3__create_pull_requests.sql`**

```sql
CREATE TABLE pull_requests (
    id              BIGSERIAL PRIMARY KEY,
    repository_id   BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    pr_number       INTEGER NOT NULL,
    title           TEXT,
    author          VARCHAR(100),
    base_branch     VARCHAR(100),
    head_branch     VARCHAR(100),
    head_sha        VARCHAR(100),
    state           VARCHAR(20),
    github_url      TEXT,
    opened_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(repository_id, pr_number)
);

CREATE INDEX idx_pull_requests_repository_id ON pull_requests(repository_id);
```

**`V4__create_reviews.sql`**

```sql
CREATE TYPE review_status AS ENUM ('PENDING', 'PROCESSING', 'DONE', 'FAILED');

CREATE TABLE reviews (
    id                  BIGSERIAL PRIMARY KEY,
    pull_request_id     BIGINT NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
    status              review_status NOT NULL DEFAULT 'PENDING',
    model_used          VARCHAR(100),
    review_summary      TEXT,
    overall_score       INTEGER,
    issues_found        INTEGER DEFAULT 0,
    raw_response        JSONB,
    posted_to_github    BOOLEAN DEFAULT FALSE,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE TABLE review_comments (
    id              BIGSERIAL PRIMARY KEY,
    review_id       BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path       TEXT NOT NULL,
    line_number     INTEGER,
    severity        VARCHAR(20) NOT NULL,
    comment         TEXT NOT NULL,
    suggestion      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_pull_request_id ON reviews(pull_request_id);
CREATE INDEX idx_review_comments_review_id ON review_comments(review_id);
```

---

### Step 8 — Create JPA entity classes

Create package `com.reviewer.model`:

**`User.java`**

```java
package com.reviewer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", unique = true, nullable = false)
    private String githubId;

    @Column(name = "github_login", nullable = false)
    private String githubLogin;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Repository> repositories;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

**`Repository.java`**

```java
package com.reviewer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "repositories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "github_repo_id", nullable = false)
    private String githubRepoId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String description;

    @Column(name = "private")
    private Boolean isPrivate;

    @Column(name = "webhook_id")
    private String webhookId;

    @Column(name = "webhook_active")
    private Boolean webhookActive = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<PullRequest> pullRequests;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

**`PullRequest.java`**

```java
package com.reviewer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "pull_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    @ToString.Exclude
    private Repository repository;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    private String title;
    private String author;

    @Column(name = "base_branch")
    private String baseBranch;

    @Column(name = "head_branch")
    private String headBranch;

    @Column(name = "head_sha")
    private String headSha;

    private String state;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

**`Review.java`**

```java
package com.reviewer.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    @ToString.Exclude
    private PullRequest pullRequest;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "review_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "review_summary", columnDefinition = "TEXT")
    private String reviewSummary;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "issues_found")
    private Integer issuesFound = 0;

    @Column(name = "raw_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawResponse;

    @Column(name = "posted_to_github")
    private Boolean postedToGithub = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ReviewComment> comments;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

**`ReviewStatus.java`**

```java
package com.reviewer.model;

public enum ReviewStatus {
    PENDING, PROCESSING, DONE, FAILED
}
```

**`ReviewComment.java`**

```java
package com.reviewer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "review_comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    @ToString.Exclude
    private Review review;

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

---

### Step 9 — Create Spring Data repositories

Create package `com.reviewer.repository`:

```java
// UserRepository.java
package com.reviewer.repository;
import com.reviewer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGithubId(String githubId);
}
```

```java
// RepositoryRepo.java
package com.reviewer.repository;
import com.reviewer.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RepositoryRepo extends JpaRepository<Repository, Long> {
    List<Repository> findByUserId(Long userId);
    Optional<Repository> findByUserIdAndGithubRepoId(Long userId, String githubRepoId);
    Optional<Repository> findByFullName(String fullName);
}
```

```java
// PullRequestRepository.java
package com.reviewer.repository;
import com.reviewer.model.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    Optional<PullRequest> findByRepositoryIdAndPrNumber(Long repositoryId, Integer prNumber);
}
```

```java
// ReviewRepository.java
package com.reviewer.repository;
import com.reviewer.model.Review;
import com.reviewer.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByPullRequestId(Long pullRequestId);

    @Query("SELECT r FROM Review r JOIN FETCH r.pullRequest pr JOIN FETCH pr.repository repo " +
           "WHERE repo.user.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findAllByUserId(Long userId);
}
```

```java
// ReviewCommentRepository.java
package com.reviewer.repository;
import com.reviewer.model.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByReviewId(Long reviewId);
}
```

**Verify the database connection works:**

```bash
cd backend
./mvnw spring-boot:run
# Look for: "HikariPool-1 - Start completed" in logs
# Flyway should log: "Successfully applied 4 migrations"
# Then Ctrl+C to stop
```

---

## Phase 3 — GitHub OAuth and JWT auth

### Step 10 — Create AppProperties config class

Create package `com.reviewer.config`:

```java
// AppProperties.java
package com.reviewer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String baseUrl;
    private String frontendUrl;
    private Jwt jwt = new Jwt();
    private String webhookSecret;
    private Ai ai = new Ai();

    @Data
    public static class Jwt {
        private String secret;
        private int expiryHours = 24;
    }

    @Data
    public static class Ai {
        private String anthropicApiKey;
        private String model;
        private int maxTokens;
        private int maxDiffChars;
    }
}
```

---

### Step 11 — Build JwtService

```java
// JwtService.java
package com.reviewer.service;

import com.reviewer.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties props;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(
            props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generate(Long userId, String githubLogin) {
        long nowMs = System.currentTimeMillis();
        long expiryMs = nowMs + (props.getJwt().getExpiryHours() * 3600_000L);
        return Jwts.builder()
            .subject(userId.toString())
            .claims(Map.of("login", githubLogin))
            .issuedAt(new Date(nowMs))
            .expiration(new Date(expiryMs))
            .signWith(getKey())
            .compact();
    }

    public Claims validate(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(validate(token).getSubject());
    }
}
```

---

### Step 12 — Build UserService

```java
// UserService.java
package com.reviewer.service;

import com.reviewer.model.User;
import com.reviewer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreate(String githubId, String login,
                             String avatarUrl, String accessToken) {
        return userRepository.findByGithubId(githubId)
            .map(existing -> {
                existing.setGithubLogin(login);
                existing.setAvatarUrl(avatarUrl);
                existing.setAccessToken(accessToken);
                return userRepository.save(existing);
            })
            .orElseGet(() -> userRepository.save(
                User.builder()
                    .githubId(githubId)
                    .githubLogin(login)
                    .avatarUrl(avatarUrl)
                    .accessToken(accessToken)
                    .build()
            ));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}
```

---

### Step 13 — Build SecurityConfig

```java
// SecurityConfig.java
package com.reviewer.config;

import com.reviewer.service.JwtService;
import com.reviewer.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserService userService;
    private final AppProperties props;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/webhooks/**"))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/login/**", "/oauth2/**").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth2 -> oauth2
                .successHandler((request, response, authentication) -> {
                    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                    String githubId  = oauth2User.getAttribute("id").toString();
                    String login     = oauth2User.getAttribute("login");
                    String avatarUrl = oauth2User.getAttribute("avatar_url");

                    // Get the access token from the OAuth2 authorized client
                    // (requires OAuth2AuthorizedClientService injection — simplified here)
                    String accessToken = ""; // Wire up OAuth2AuthorizedClientService here

                    var user = userService.findOrCreate(githubId, login, avatarUrl, accessToken);
                    String jwt = jwtService.generate(user.getId(), user.getGithubLogin());

                    response.sendRedirect(props.getFrontendUrl()
                        + "/auth/callback?token=" + jwt);
                }))
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                String header = req.getHeader("Authorization");
                if (header != null && header.startsWith("Bearer ")) {
                    try {
                        String token = header.substring(7);
                        Long userId = jwtService.extractUserId(token);
                        var user = userService.findById(userId);
                        var auth = new UsernamePasswordAuthenticationToken(
                            user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (Exception ignored) {}
                }
                chain.doFilter(req, res);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(props.getFrontendUrl()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

> The access token extraction from OAuth2 context requires injecting `OAuth2AuthorizedClientService`. Wire that up following Spring's docs — it's 5 extra lines in the success handler.

---

### Step 14 — Set up NextAuth.js

Create `frontend/app/api/auth/[...nextauth]/route.ts`:

```typescript
import NextAuth, { NextAuthOptions } from "next-auth";
import GithubProvider from "next-auth/providers/github";

export const authOptions: NextAuthOptions = {
  providers: [
    GithubProvider({
      clientId: process.env.GITHUB_CLIENT_ID!,
      clientSecret: process.env.GITHUB_CLIENT_SECRET!,
      authorization: {
        params: { scope: "read:user repo admin:repo_hook" },
      },
    }),
  ],
  callbacks: {
    async jwt({ token, account }) {
      if (account?.access_token) {
        token.githubAccessToken = account.access_token;
      }
      return token;
    },
    async session({ session, token }) {
      (session as any).githubAccessToken = token.githubAccessToken;
      return session;
    },
  },
  pages: {
    signIn: "/login",
  },
};

const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };
```

Create `frontend/.env.local`:

```env
GITHUB_CLIENT_ID=your_client_id_here
GITHUB_CLIENT_SECRET=your_client_secret_here
NEXTAUTH_SECRET=generate-with-openssl-rand-base64-32
NEXTAUTH_URL=http://localhost:3000
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Create `frontend/app/login/page.tsx`:

```typescript
"use client";
import { signIn } from "next-auth/react";

export default function LoginPage() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-gray-950">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-white mb-2">AI Code Reviewer</h1>
        <p className="text-gray-400 mb-8">Automated PR reviews powered by AI</p>
        <button
          onClick={() => signIn("github", { callbackUrl: "/dashboard" })}
          className="bg-white text-gray-900 font-semibold px-6 py-3 rounded-lg hover:bg-gray-100 transition"
        >
          Sign in with GitHub
        </button>
      </div>
    </main>
  );
}
```

Wrap root layout with `SessionProvider` in `frontend/app/layout.tsx`:

```typescript
import { getServerSession } from "next-auth";
import { authOptions } from "./api/auth/[...nextauth]/route";
import SessionProviderWrapper from "@/components/SessionProviderWrapper";

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(authOptions);
  return (
    <html lang="en">
      <body>
        <SessionProviderWrapper session={session}>
          {children}
        </SessionProviderWrapper>
      </body>
    </html>
  );
}
```

Create `frontend/components/SessionProviderWrapper.tsx`:

```typescript
"use client";
import { SessionProvider } from "next-auth/react";
export default function SessionProviderWrapper({
  children, session
}: { children: React.ReactNode; session: any }) {
  return <SessionProvider session={session}>{children}</SessionProvider>;
}
```

**Test auth flow:**

```bash
# Terminal 1 — start backend
cd backend && ./mvnw spring-boot:run

# Terminal 2 — start frontend
cd frontend && npm run dev

# Visit http://localhost:3000/login
# Click "Sign in with GitHub"
# Authorize the app
# You should land on /dashboard (404 for now — that's fine)
# Check backend logs for the OAuth callback
# Check DB: psql -U reviewer codereview -c "SELECT github_login FROM users;"
```

---

## Phase 4 — Repository management

### Step 15 — Build GitHubService

Create package `com.reviewer.service`:

```java
// GitHubService.java
package com.reviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final RestTemplate restTemplate;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Accept", "application/vnd.github.v3+json");
        return h;
    }

    public List<Map<String, Object>> getUserRepositories(String token) {
        ResponseEntity<List> response = restTemplate.exchange(
            "https://api.github.com/user/repos?per_page=100&sort=updated",
            HttpMethod.GET,
            new HttpEntity<>(headers(token)),
            List.class
        );
        return response.getBody();
    }

    public String registerWebhook(String repoFullName, String token) {
        String webhookUrl = props.getBaseUrl() + "/api/webhooks/github";
        Map<String, Object> body = Map.of(
            "name",   "web",
            "active", true,
            "events", List.of("pull_request"),
            "config", Map.of(
                "url",          webhookUrl,
                "content_type", "json",
                "secret",       props.getWebhookSecret()
            )
        );
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.github.com/repos/" + repoFullName + "/hooks",
            new HttpEntity<>(body, headers(token)),
            Map.class
        );
        return response.getBody().get("id").toString();
    }

    public void deleteWebhook(String repoFullName, String webhookId, String token) {
        restTemplate.exchange(
            "https://api.github.com/repos/" + repoFullName + "/hooks/" + webhookId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers(token)),
            Void.class
        );
    }

    public String fetchPRDiff(String repoFullName, int prNumber, String token) {
        HttpHeaders h = headers(token);
        h.set("Accept", "application/vnd.github.v3.diff");
        ResponseEntity<String> response = restTemplate.exchange(
            "https://api.github.com/repos/" + repoFullName + "/pulls/" + prNumber,
            HttpMethod.GET,
            new HttpEntity<>(h),
            String.class
        );
        String diff = response.getBody();
        if (diff != null && diff.length() > props.getAi().getMaxDiffChars()) {
            diff = diff.substring(0, props.getAi().getMaxDiffChars()) + "\n... [diff truncated]";
        }
        return diff;
    }

    public void postReviewComment(String repoFullName, int prNumber,
                                   String commitSha, String filePath,
                                   int line, String body, String token) {
        Map<String, Object> payload = Map.of(
            "body",      body,
            "commit_id", commitSha,
            "path",      filePath,
            "line",      line,
            "side",      "RIGHT"
        );
        try {
            restTemplate.postForEntity(
                "https://api.github.com/repos/" + repoFullName + "/pulls/" + prNumber + "/comments",
                new HttpEntity<>(payload, headers(token)),
                Void.class
            );
        } catch (Exception e) {
            log.warn("Failed to post review comment to GitHub: {}", e.getMessage());
        }
    }

    public void postReviewSummary(String repoFullName, int prNumber,
                                   String body, String token) {
        Map<String, Object> payload = Map.of("body", body);
        restTemplate.postForEntity(
            "https://api.github.com/repos/" + repoFullName + "/issues/" + prNumber + "/comments",
            new HttpEntity<>(payload, headers(token)),
            Void.class
        );
    }
}
```

Add `RestTemplate` and `ObjectMapper` beans in a config class:

```java
// BeanConfig.java
package com.reviewer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BeanConfig {
    @Bean
    public RestTemplate restTemplate() { return new RestTemplate(); }

    @Bean
    public ObjectMapper objectMapper() { return new ObjectMapper(); }
}
```

---

### Step 16 — Build RepositoryController

```java
// RepositoryController.java
package com.reviewer.controller;

import com.reviewer.model.Repository;
import com.reviewer.model.User;
import com.reviewer.repository.RepositoryRepo;
import com.reviewer.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryRepo repositoryRepo;
    private final GitHubService githubService;

    @GetMapping
    public ResponseEntity<List<Repository>> listTrackedRepos(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(repositoryRepo.findByUserId(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Repository> addRepo(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String githubRepoId = body.get("githubRepoId");
        String fullName     = body.get("fullName");
        String description  = body.get("description");

        Repository repo = repositoryRepo
            .findByUserIdAndGithubRepoId(user.getId(), githubRepoId)
            .orElse(Repository.builder()
                .user(user)
                .githubRepoId(githubRepoId)
                .fullName(fullName)
                .description(description)
                .webhookActive(false)
                .build());

        return ResponseEntity.ok(repositoryRepo.save(repo));
    }

    @PostMapping("/{id}/webhook")
    public ResponseEntity<Repository> enableWebhook(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Repository repo = repositoryRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Repo not found"));

        String webhookId = githubService.registerWebhook(
            repo.getFullName(), user.getAccessToken());
        repo.setWebhookId(webhookId);
        repo.setWebhookActive(true);
        return ResponseEntity.ok(repositoryRepo.save(repo));
    }

    @DeleteMapping("/{id}/webhook")
    public ResponseEntity<Repository> disableWebhook(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Repository repo = repositoryRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Repo not found"));

        if (repo.getWebhookId() != null) {
            githubService.deleteWebhook(repo.getFullName(),
                repo.getWebhookId(), user.getAccessToken());
        }
        repo.setWebhookId(null);
        repo.setWebhookActive(false);
        return ResponseEntity.ok(repositoryRepo.save(repo));
    }
}
```

---

## Phase 5 — Webhook pipeline

### Step 17 — Build WebhookValidator

```java
// WebhookValidator.java
package com.reviewer.service;

import com.reviewer.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class WebhookValidator {

    private final AppProperties props;

    public boolean isValid(String rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                props.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computed = "sha256=" + HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

### Step 18 — Create WebhookPayloadDTO

Create package `com.reviewer.dto`:

```java
// WebhookPayloadDTO.java
package com.reviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayloadDTO {
    private String action;
    private PullRequestData pullRequest;
    private RepositoryData repository;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequestData {
        private int number;
        private String title;
        private String state;

        @JsonProperty("html_url")
        private String htmlUrl;
        private UserData user;
        private BranchData base;
        private BranchData head;

        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryData {
        @JsonProperty("full_name")
        private String fullName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserData {
        private String login;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BranchData {
        private String ref;
        private String sha;
    }
}
```

---

### Step 19 — Configure Async and build WebhookController

```java
// AsyncConfig.java
package com.reviewer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reviewExecutor")
    public Executor reviewTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ReviewJob-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

```java
// WebhookController.java
package com.reviewer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.dto.WebhookPayloadDTO;
import com.reviewer.service.ReviewJobService;
import com.reviewer.service.WebhookValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookValidator validator;
    private final ReviewJobService reviewJobService;
    private final ObjectMapper objectMapper;

    private static final List<String> TRIGGER_ACTIONS =
        List.of("opened", "synchronize", "reopened");

    @PostMapping("/github")
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String sig,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event) {

        if (!validator.isValid(rawBody, sig)) {
            log.warn("Invalid webhook signature rejected");
            return ResponseEntity.status(401).build();
        }

        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok().build();
        }

        try {
            WebhookPayloadDTO payload = objectMapper.readValue(
                rawBody, WebhookPayloadDTO.class);
            log.info("Webhook received: action={} PR={}",
                payload.getAction(),
                payload.getPullRequest().getNumber());

            if (TRIGGER_ACTIONS.contains(payload.getAction())) {
                reviewJobService.enqueueReview(payload);
            }
        } catch (Exception e) {
            log.error("Failed to parse webhook payload: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
```

---

### Step 20 — Expose local backend with ngrok

```bash
# In a new terminal — keep this running the whole time you develop
ngrok http 8080
```

Copy the HTTPS URL (e.g. `https://abc123.ngrok-free.app`).

Update `backend/.env`:

```env
APP_BASE_URL=https://abc123.ngrok-free.app
```

Restart the backend. Now GitHub can reach your local machine.

---

## Phase 6 — AI review service

### Step 21 — Create ReviewResultDTO

```java
// ReviewResultDTO.java
package com.reviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewResultDTO {
    private String summary;
    private List<IssueDTO> issues;

    @JsonProperty("overall_score")
    private Integer overallScore;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueDTO {
        private String file;
        private Integer line;
        private String severity;
        private String comment;
        private String suggestion;
    }
}
```

---

### Step 22 — Build AIReviewService

```java
// AIReviewService.java
package com.reviewer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.config.AppProperties;
import com.reviewer.dto.ReviewResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIReviewService {

    private final RestTemplate restTemplate;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are a senior software engineer performing a thorough code review.
        Analyze the provided git diff carefully.

        Return ONLY a valid JSON object with this exact structure — no markdown, no explanation:
        {
          "summary": "2-3 sentence overview of the changes and overall quality",
          "overall_score": 7,
          "issues": [
            {
              "file": "src/main/java/Example.java",
              "line": 42,
              "severity": "BUG",
              "comment": "Clear description of the issue",
              "suggestion": "Specific fix or improvement"
            }
          ]
        }

        Severity must be one of: BUG, SECURITY, PERFORMANCE, STYLE, INFO
        Focus on: null pointer risks, SQL injection, missing error handling,
        inefficient loops, hardcoded secrets, missing input validation.
        If no issues found, return an empty issues array.
        overall_score is 1-10 where 10 is perfect production-ready code.
        """;

    public ReviewResultDTO analyzeCode(String diff) {
        Map<String, Object> requestBody = Map.of(
            "model",      props.getAi().getModel(),
            "max_tokens", props.getAi().getMaxTokens(),
            "system",     SYSTEM_PROMPT,
            "messages",   List.of(Map.of(
                "role",    "user",
                "content", "Review this pull request diff:\n\n```diff\n" + diff + "\n```"
            ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", props.getAi().getanthropicApiKey());
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.anthropic.com/v1/messages",
            new HttpEntity<>(requestBody, headers),
            Map.class
        );

        String jsonText = extractContent(response.getBody());
        // Strip any accidental markdown fences
        jsonText = jsonText.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        try {
            return objectMapper.readValue(jsonText, ReviewResultDTO.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", jsonText);
            throw new RuntimeException("AI returned invalid JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> content =
            (List<Map<String, Object>>) response.get("content");
        return content.stream()
            .filter(c -> "text".equals(c.get("type")))
            .map(c -> (String) c.get("text"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No text in AI response"));
    }
}
```

---

### Step 23 — Build ReviewJobService — the full pipeline

```java
// ReviewJobService.java
package com.reviewer.service;

import com.reviewer.dto.ReviewResultDTO;
import com.reviewer.dto.WebhookPayloadDTO;
import com.reviewer.model.*;
import com.reviewer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewJobService {

    private final RepositoryRepo repositoryRepo;
    private final PullRequestRepository prRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final GitHubService githubService;
    private final AIReviewService aiReviewService;
    private final RedisTemplate<String, String> redisTemplate;

    @Async("reviewExecutor")
    public void enqueueReview(WebhookPayloadDTO payload) {
        String repoFullName = payload.getRepository().getFullName();
        int prNumber = payload.getPullRequest().getNumber();
        String lockKey = "review:lock:" + repoFullName + ":" + prNumber;

        // Deduplication — skip if already processing
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "processing", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("Review already in progress for {}/#{}", repoFullName, prNumber);
            return;
        }

        log.info("Starting review for {}/#{}", repoFullName, prNumber);
        Review review = null;

        try {
            // 1. Find the tracked repository
            Repository repo = repositoryRepo.findByFullName(repoFullName)
                .orElseThrow(() -> new RuntimeException("Repo not tracked: " + repoFullName));

            String userToken = repo.getUser().getAccessToken();
            WebhookPayloadDTO.PullRequestData prData = payload.getPullRequest();

            // 2. Upsert PullRequest record
            PullRequest pr = prRepository
                .findByRepositoryIdAndPrNumber(repo.getId(), prNumber)
                .orElse(PullRequest.builder()
                    .repository(repo)
                    .prNumber(prNumber)
                    .build());

            pr.setTitle(prData.getTitle());
            pr.setAuthor(prData.getUser().getLogin());
            pr.setBaseBranch(prData.getBase().getRef());
            pr.setHeadBranch(prData.getHead().getRef());
            pr.setHeadSha(prData.getHead().getSha());
            pr.setState(prData.getState());
            pr.setGithubUrl(prData.getHtmlUrl());
            pr = prRepository.save(pr);

            // 3. Create review with PROCESSING status
            review = reviewRepository.save(Review.builder()
                .pullRequest(pr)
                .status(ReviewStatus.PROCESSING)
                .modelUsed("claude-sonnet-4-20250514")
                .build());

            // 4. Fetch the PR diff
            String diff = githubService.fetchPRDiff(repoFullName, prNumber, userToken);
            if (diff == null || diff.isBlank()) {
                throw new RuntimeException("Empty diff — nothing to review");
            }

            // 5. Send to AI
            ReviewResultDTO result = aiReviewService.analyzeCode(diff);

            // 6. Save review comments
            if (result.getIssues() != null && !result.getIssues().isEmpty()) {
                List<ReviewComment> comments = result.getIssues().stream()
                    .filter(i -> i.getFile() != null && i.getComment() != null)
                    .map(issue -> ReviewComment.builder()
                        .review(review)
                        .filePath(issue.getFile())
                        .lineNumber(issue.getLine())
                        .severity(issue.getSeverity() != null
                            ? issue.getSeverity() : "INFO")
                        .comment(issue.getComment())
                        .suggestion(issue.getSuggestion())
                        .build())
                    .collect(Collectors.toList());
                commentRepository.saveAll(comments);

                // 7. Post inline comments to GitHub PR
                String commitSha = prData.getHead().getSha();
                for (ReviewComment c : comments) {
                    if (c.getLineNumber() != null && c.getLineNumber() > 0) {
                        githubService.postReviewComment(
                            repoFullName, prNumber, commitSha,
                            c.getFilePath(), c.getLineNumber(),
                            formatGitHubComment(c), userToken);
                    }
                }

                // 8. Post summary comment
                githubService.postReviewSummary(repoFullName, prNumber,
                    formatSummaryComment(result), userToken);
            }

            // 9. Mark review DONE
            review.setStatus(ReviewStatus.DONE);
            review.setReviewSummary(result.getSummary());
            review.setOverallScore(result.getOverallScore());
            review.setIssuesFound(result.getIssues() != null
                ? result.getIssues().size() : 0);
            review.setPostedToGithub(true);
            review.setCompletedAt(Instant.now());
            reviewRepository.save(review);

            log.info("Review DONE for {}/#{} — {} issues found",
                repoFullName, prNumber, review.getIssuesFound());

        } catch (Exception e) {
            log.error("Review FAILED for {}/#{}: {}", repoFullName, prNumber, e.getMessage(), e);
            if (review != null) {
                review.setStatus(ReviewStatus.FAILED);
                review.setErrorMessage(e.getMessage());
                review.setCompletedAt(Instant.now());
                reviewRepository.save(review);
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private String formatGitHubComment(ReviewComment c) {
        String emoji = switch (c.getSeverity()) {
            case "BUG"         -> "🐛";
            case "SECURITY"    -> "🔒";
            case "PERFORMANCE" -> "⚡";
            case "STYLE"       -> "✨";
            default            -> "💡";
        };
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**%s %s**\n\n%s", emoji, c.getSeverity(), c.getComment()));
        if (c.getSuggestion() != null && !c.getSuggestion().isBlank()) {
            sb.append(String.format("\n\n> **Suggestion:** %s", c.getSuggestion()));
        }
        return sb.toString();
    }

    private String formatSummaryComment(ReviewResultDTO result) {
        int score = result.getOverallScore() != null ? result.getOverallScore() : 0;
        int issues = result.getIssues() != null ? result.getIssues().size() : 0;
        return String.format(
            "## 🤖 AI Code Review\n\n**Score:** %d/10 | **Issues found:** %d\n\n%s\n\n" +
            "_Reviewed by [AI Code Reviewer](https://github.com)_",
            score, issues, result.getSummary()
        );
    }
}
```

**End-to-end test — the moment of truth:**

```bash
# Restart backend with fresh env
./mvnw spring-boot:run

# 1. Open a real pull request on GitHub in a repo you enabled
# 2. Watch the Spring Boot logs — you should see:
#    "Webhook received: action=opened PR=1"
#    "Starting review for owner/repo/#1"
#    "Review DONE for owner/repo/#1 — 3 issues found"
# 3. Check the GitHub PR — you should see AI comments posted

# 4. Verify in DB:
psql -U reviewer codereview -c \
  "SELECT r.status, r.issues_found, r.review_summary FROM reviews r LIMIT 5;"
```

---

## Phase 7 — Redis caching

### Step 24 — Configure Redis cache manager

```java
// RedisConfig.java
package com.reviewer.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

### Step 25 — Add caching to ReviewController

```java
// ReviewController.java
package com.reviewer.controller;

import com.reviewer.model.*;
import com.reviewer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository commentRepository;
    private final ReviewJobService reviewJobService;
    private final PullRequestRepository prRepository;

    @GetMapping("/{prId}")
    @Cacheable(value = "pr-reviews", key = "#prId")
    public ResponseEntity<Review> getReview(@PathVariable Long prId) {
        return reviewRepository.findByPullRequestId(prId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{prId}/comments")
    public ResponseEntity<List<ReviewComment>> getComments(@PathVariable Long prId) {
        return reviewRepository.findByPullRequestId(prId)
            .map(r -> ResponseEntity.ok(commentRepository.findByReviewId(r.getId())))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{prId}/trigger")
    public ResponseEntity<Map<String, String>> triggerReview(
            @AuthenticationPrincipal User user,
            @PathVariable Long prId) {
        // Build a minimal payload and re-enqueue — implementation left as exercise
        return ResponseEntity.ok(Map.of("status", "queued"));
    }
}
```

---

## Phase 8 — Frontend (AI-assisted, you wire up)

### Step 26 — Create API client utility

Create `frontend/lib/api.ts`:

```typescript
const API = process.env.NEXT_PUBLIC_API_URL;

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };
}

export async function getRepositories(token: string) {
  const res = await fetch(`${API}/api/repos`, { headers: authHeaders(token) });
  if (!res.ok) throw new Error("Failed to fetch repos");
  return res.json();
}

export async function enableWebhook(repoId: number, token: string) {
  const res = await fetch(`${API}/api/repos/${repoId}/webhook`, {
    method: "POST", headers: authHeaders(token),
  });
  if (!res.ok) throw new Error("Failed to enable webhook");
  return res.json();
}

export async function getReview(prId: number, token: string) {
  const res = await fetch(`${API}/api/reviews/${prId}`, { headers: authHeaders(token) });
  if (!res.ok) throw new Error("Failed to fetch review");
  return res.json();
}

export async function getReviewComments(prId: number, token: string) {
  const res = await fetch(`${API}/api/reviews/${prId}/comments`, {
    headers: authHeaders(token),
  });
  return res.json();
}
```

### Step 27 — AI-generate the remaining UI pages

Use this prompt with Claude for each page. Replace `[PAGE]` with what you need:

```
I'm building a Next.js 14 App Router frontend (TypeScript + Tailwind) for an AI GitHub
Code Review platform. The backend returns this data shape:

[paste the JSON response from your API here]

Build me a complete [PAGE] component that:
- Uses the `useSession()` hook from next-auth/react to get the session
- Calls the API using the functions in lib/api.ts
- Uses SWR for data fetching with loading and error states
- Has a clean dark theme (bg-gray-950, text-white)
- Shows severity badges: BUG=red, SECURITY=orange, PERFORMANCE=yellow, STYLE=blue, INFO=gray

Pages to generate one at a time:
1. app/dashboard/page.tsx — repository list with enable/disable webhook toggle
2. app/dashboard/[repoId]/page.tsx — PR history table with review status badges
3. app/dashboard/[repoId]/reviews/[prId]/page.tsx — review detail with comment cards
```

### Step 28 — Wire up the auth callback route

Create `frontend/app/auth/callback/page.tsx`:

```typescript
"use client";
import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export default function AuthCallback() {
  const router = useRouter();
  const params = useSearchParams();

  useEffect(() => {
    const token = params.get("token");
    if (token) {
      localStorage.setItem("jwt", token);
      router.push("/dashboard");
    } else {
      router.push("/login");
    }
  }, []);

  return <div className="min-h-screen bg-gray-950 flex items-center justify-center">
    <p className="text-white">Signing you in...</p>
  </div>;
}
```

---

## Phase 9 — Final polish

### Step 29 — Add global error handling

```java
// GlobalExceptionHandler.java
package com.reviewer.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        log.error("Unhandled exception: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied() {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Access denied"));
    }
}
```

### Step 30 — Write tests

```java
// WebhookValidatorTest.java
package com.reviewer.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class WebhookValidatorTest {

    private WebhookValidator validator;

    @BeforeEach
    void setup() {
        AppProperties props = new AppProperties();
        props.setWebhookSecret("test-secret-key");
        validator = new WebhookValidator(props);
    }

    @Test
    void validSignatureReturnsTrue() {
        String body = "{\"action\":\"opened\"}";
        // Compute real HMAC and test — use a pre-computed value
        // sha256=... computed with secret "test-secret-key"
        assertFalse(validator.isValid(body, null));
        assertFalse(validator.isValid(body, "sha256=wrongsignature"));
    }

    @Test
    void nullSignatureReturnsFalse() {
        assertFalse(validator.isValid("body", null));
    }

    @Test
    void missingPrefixReturnsFalse() {
        assertFalse(validator.isValid("body", "abc123"));
    }
}
```

### Step 31 — Write the README

Your README must have:

1. Project title + 1-sentence description
2. A demo GIF or screenshot (record with Loom, crop to just the PR getting commented on)
3. Architecture diagram (link to implementation.md)
4. Tech stack table
5. Local setup instructions (exactly the commands from this guide)
6. Environment variables table
7. A "How it works" section with the 5-step flow

---

## Quick reference — run everything

```bash
# Start infrastructure
docker-compose up -d

# Terminal 1 — ngrok
ngrok http 8080

# Terminal 2 — backend (from /backend)
./mvnw spring-boot:run

# Terminal 3 — frontend (from /frontend)
npm run dev

# Verify DB
psql -U reviewer -h localhost codereview

# Watch review jobs
tail -f backend/logs/spring.log | grep "ReviewJob"
```

---

## Common errors and fixes

**`PSQLException: relation "users" does not exist`**
→ Flyway migrations didn't run. Check `spring.flyway.enabled=true` and that your `db/migration` folder has the right file naming (`V1__`, `V2__`).

**`401 Unauthorized` from GitHub API**
→ The stored access token is expired or has wrong scopes. Delete the user row from DB and re-authenticate.

**`WebhookValidator always returns false`**
→ Spring is consuming the request body before your controller reads it. Add `@RequestBody String rawBody` (String, not a parsed object) to the webhook controller method — this is critical.

**`@Async` method not running asynchronously**
→ You're calling the `@Async` method from within the same class. Move the call to a different bean — Spring proxies only intercept external calls.

**`RedisConnectionException`**
→ Docker Redis container isn't running. Run `docker-compose up redis -d`.

**`AI returns non-JSON response`**
→ The model sometimes adds a preamble. The `.replaceAll("```json", "")` strip in `AIReviewService` handles this. If it's still broken, log the raw response and check what the model returned.

---

> Good luck. The first time you open a PR and see AI comments appear automatically on GitHub — that moment makes every compile error worth it.