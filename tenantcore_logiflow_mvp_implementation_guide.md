# TenantCore Platform + LogiFlow Service — Tài liệu triển khai MVP

> Mục tiêu của tài liệu này là làm checklist bám sát code thực tế cho IDE Agent khi tiếp tục triển khai dự án.
>
> Trạng thái hiện tại:
>
> - Đã tạo monorepo Maven: `tenant-platform`
> - Đã có các module: `common-lib`, `core-service`, `gateway-service`, `logiflow-service`
> - Đã thống nhất dùng Maven multi-module
> - Đã thay `devhub-service` bằng `logiflow-service`
> - Tạm thời dùng Neon PostgreSQL để dev
> - Flyway migration sẽ chạy từ `core-service`
> - `logiflow-service` dùng DB nhưng không chạy Flyway

---

## 1. Kiến trúc MVP

### 1.1 Service chính

```text
tenant-platform/
├── common-lib
├── core-service
├── gateway-service
├── logiflow-service
├── pom.xml
└── .gitignore
```

| Module | Loại | Có chạy riêng không? | Vai trò |
|---|---|---:|---|
| `common-lib` | Maven module thường | Không | Chứa DTO, exception, context, constants dùng chung |
| `core-service` | Spring Boot app | Có | Auth, tenant, user, role, permission, file, audit, notification |
| `gateway-service` | Spring Boot Gateway app | Có | Cổng vào API, route request, JWT filter, tenant filter, CORS |
| `logiflow-service` | Spring Boot app | Có | Nghiệp vụ logistics: customer, driver, vehicle, order, delivery, COD |

### 1.2 Flow request chuẩn

```text
Frontend / Postman
    ↓
gateway-service :8080
    ↓
core-service :8081
logiflow-service :8082
    ↓
Neon PostgreSQL
```

Nguyên tắc:

```text
Frontend không gọi trực tiếp core-service hoặc logiflow-service khi test flow đầy đủ.
Tất cả API nên đi qua gateway-service.
```

---

## 2. Tech stack chốt

```text
Java: 21 LTS
Build tool: Maven
Spring Boot: 3.5.x
Spring Cloud: 2025.0.x
Database: PostgreSQL / Neon PostgreSQL
Migration: Flyway
ORM: Spring Data JPA
Gateway: Spring Cloud Gateway Reactive
Validation: Jakarta Validation
```

---

## 3. Maven multi-module

### 3.1 Parent `pom.xml`

File:

```text
tenant-platform/pom.xml
```

Yêu cầu quan trọng:

```xml
<groupId>com.tenantcore</groupId>
<artifactId>tenant-platform</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>

<modules>
    <module>common-lib</module>
    <module>core-service</module>
    <module>gateway-service</module>
    <module>logiflow-service</module>
</modules>
```

Lưu ý:

```text
${project.version} là biến có sẵn của Maven.
Nó lấy giá trị từ <version> của project hiện tại hoặc parent.
Không cần khai báo riêng <project.version>.
```

### 3.2 Module con phải trỏ về parent

Ví dụ trong `core-service/pom.xml`:

```xml
<parent>
    <groupId>com.tenantcore</groupId>
    <artifactId>tenant-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>core-service</artifactId>
```

Tương tự cho:

```text
common-lib/pom.xml
gateway-service/pom.xml
logiflow-service/pom.xml
```

---

## 4. Git

### 4.1 Cần commit vào Git

```text
pom.xml
common-lib/pom.xml
core-service/pom.xml
gateway-service/pom.xml
logiflow-service/pom.xml
mvnw
mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
src/
```

### 4.2 Không commit

```gitignore
target/
.idea/
*.iml
.env
*.env
application-local.yml
logs/
.DS_Store
```

Không hard-code database password vào source code hoặc `application.yml`.

---

## 5. `common-lib`

### 5.1 Vị trí source đúng

Các class Java phải nằm trong:

```text
common-lib/src/main/java/com/tenantcore/common/
```

Nếu IntelliJ báo:

```text
Java file is located outside of the module source root, so it won't be compiled
```

thì kiểm tra lại file có đang nằm đúng dưới:

```text
common-lib/src/main/java/
```

Hoặc trong IntelliJ:

```text
Right click common-lib/src/main/java
→ Mark Directory as
→ Sources Root
```

Sau đó reload Maven.

### 5.2 Cấu trúc package cần có

```text
common-lib/src/main/java/com/tenantcore/common/
├── context/
│   ├── TenantContext.java
│   └── UserContext.java
├── dto/
│   ├── ApiResponse.java
│   ├── ErrorResponse.java
│   └── PageResponse.java
├── exception/
│   ├── BusinessException.java
│   ├── ErrorCode.java
│   └── GlobalExceptionHandler.java
└── security/
    └── SecurityConstants.java
```

### 5.3 Các class cần tạo

#### `ApiResponse.java`

```java
package com.tenantcore.common.dto;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                "SUCCESS",
                "Success",
                data,
                LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                "SUCCESS",
                message,
                data,
                LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(
                code,
                message,
                null,
                LocalDateTime.now()
        );
    }
}
```

#### `ErrorResponse.java`

```java
package com.tenantcore.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> details,
        String path,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(
                code,
                message,
                null,
                path,
                LocalDateTime.now()
        );
    }

    public static ErrorResponse of(
            String code,
            String message,
            Map<String, Object> details,
            String path
    ) {
        return new ErrorResponse(
                code,
                message,
                details,
                path,
                LocalDateTime.now()
        );
    }
}
```

#### `PageResponse.java`

```java
package com.tenantcore.common.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> of(
            List<T> items,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = size <= 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        boolean first = page <= 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;

        return new PageResponse<>(
                items,
                page,
                size,
                totalElements,
                totalPages,
                first,
                last
        );
    }
}
```

#### `ErrorCode.java`

```java
package com.tenantcore.common.exception;

public enum ErrorCode {

    SUCCESS("SUCCESS", "Success", 200),

    BAD_REQUEST("BAD_REQUEST", "Bad request", 400),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation error", 400),

    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized", 401),
    INVALID_TOKEN("INVALID_TOKEN", "Invalid token", 401),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token expired", 401),

    FORBIDDEN("FORBIDDEN", "Forbidden", 403),
    TENANT_REQUIRED("TENANT_REQUIRED", "Tenant code is required", 400),
    TENANT_FORBIDDEN("TENANT_FORBIDDEN", "Tenant code is invalid or not allowed", 403),

    NOT_FOUND("NOT_FOUND", "Resource not found", 404),
    DATA_CONFLICT("DATA_CONFLICT", "Data conflict", 409),

    USER_NOT_FOUND("USER_NOT_FOUND", "User not found", 404),
    USER_DISABLED("USER_DISABLED", "User is disabled", 403),
    INVALID_USERNAME_OR_PASSWORD("INVALID_USERNAME_OR_PASSWORD", "Invalid username or password", 401),

    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found", 404),
    PERMISSION_DENIED("PERMISSION_DENIED", "Permission denied", 403),

    FILE_NOT_FOUND("FILE_NOT_FOUND", "File not found", 404),
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "File upload failed", 500),

    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
```

#### `BusinessException.java`

```java
package com.tenantcore.common.exception;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getCode() {
        return errorCode.getCode();
    }

    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
```

#### `TenantContext.java`

```java
package com.tenantcore.common.context;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;

public final class TenantContext {

    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantCode(String tenantCode) {
        TENANT_CODE_HOLDER.set(tenantCode);
    }

    public static String getTenantCode() {
        return TENANT_CODE_HOLDER.get();
    }

    public static String requireTenantCode() {
        String tenantCode = getTenantCode();

        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_REQUIRED);
        }

        return tenantCode;
    }

    public static boolean hasTenant() {
        String tenantCode = getTenantCode();
        return tenantCode != null && !tenantCode.isBlank();
    }

    public static void clear() {
        TENANT_CODE_HOLDER.remove();
    }
}
```

#### `UserContext.java`

```java
package com.tenantcore.common.context;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUser(CurrentUser user) {
        USER_HOLDER.set(user);
    }

    public static CurrentUser getCurrentUser() {
        return USER_HOLDER.get();
    }

    public static CurrentUser requireCurrentUser() {
        CurrentUser user = getCurrentUser();

        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return user;
    }

    public static UUID getUserId() {
        CurrentUser user = getCurrentUser();
        return user == null ? null : user.userId();
    }

    public static UUID requireUserId() {
        return requireCurrentUser().userId();
    }

    public static String getUsername() {
        CurrentUser user = getCurrentUser();
        return user == null ? null : user.username();
    }

    public static boolean hasRole(String roleCode) {
        CurrentUser user = getCurrentUser();

        if (user == null || user.roles() == null) {
            return false;
        }

        return user.roles().contains(roleCode);
    }

    public static boolean hasPermission(String permissionCode) {
        CurrentUser user = getCurrentUser();

        if (user == null || user.permissions() == null) {
            return false;
        }

        return user.permissions().contains(permissionCode);
    }

    public static void clear() {
        USER_HOLDER.remove();
    }

    public record CurrentUser(
            UUID userId,
            String tenantCode,
            String username,
            String fullName,
            Set<String> roles,
            Set<String> permissions
    ) {
        public CurrentUser {
            roles = roles == null ? Collections.emptySet() : Set.copyOf(roles);
            permissions = permissions == null ? Collections.emptySet() : Set.copyOf(permissions);
        }

        public List<String> roleList() {
            return roles.stream().toList();
        }

        public List<String> permissionList() {
            return permissions.stream().toList();
        }
    }
}
```

#### `SecurityConstants.java`

```java
package com.tenantcore.common.security;

public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String TENANT_HEADER = "X-Tenant-Code";

    public static final String USER_ID_CLAIM = "userId";
    public static final String TENANT_CODE_CLAIM = "tenantCode";
    public static final String USERNAME_CLAIM = "username";
    public static final String ROLES_CLAIM = "roles";
    public static final String PERMISSIONS_CLAIM = "permissions";

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_DELETED = "DELETED";

    public static final String PERMISSION_USER_VIEW = "USER_VIEW";
    public static final String PERMISSION_USER_CREATE = "USER_CREATE";
    public static final String PERMISSION_USER_UPDATE = "USER_UPDATE";
    public static final String PERMISSION_USER_DELETE = "USER_DELETE";

    public static final String PERMISSION_ROLE_VIEW = "ROLE_VIEW";
    public static final String PERMISSION_ROLE_CREATE = "ROLE_CREATE";
    public static final String PERMISSION_ROLE_UPDATE = "ROLE_UPDATE";
    public static final String PERMISSION_ROLE_DELETE = "ROLE_DELETE";

    public static final String PERMISSION_LOGIFLOW_ORDER_VIEW = "LOGIFLOW_ORDER_VIEW";
    public static final String PERMISSION_LOGIFLOW_ORDER_CREATE = "LOGIFLOW_ORDER_CREATE";
    public static final String PERMISSION_LOGIFLOW_ORDER_UPDATE = "LOGIFLOW_ORDER_UPDATE";
    public static final String PERMISSION_LOGIFLOW_ORDER_DELETE = "LOGIFLOW_ORDER_DELETE";
    public static final String PERMISSION_LOGIFLOW_ORDER_ASSIGN = "LOGIFLOW_ORDER_ASSIGN";
    public static final String PERMISSION_LOGIFLOW_ORDER_TRACKING = "LOGIFLOW_ORDER_TRACKING";
}
```

#### `GlobalExceptionHandler.java`

```java
package com.tenantcore.common.exception;

import com.tenantcore.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponse response = ErrorResponse.of(
                errorCode.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(),
                details,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(),
                details,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath().toString();
            details.put(field, violation.getMessage());
        });

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getMessage(),
                details,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", ex.getParameterName());
        details.put("parameterType", ex.getParameterType());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.BAD_REQUEST.getCode(),
                "Required request parameter is missing",
                details,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("headerName", ex.getHeaderName());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.BAD_REQUEST.getCode(),
                "Required request header is missing",
                details,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.BAD_REQUEST.getCode(),
                "Request body is invalid or malformed",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("method", ex.getMethod());
        details.put("supportedMethods", ex.getSupportedMethods());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.BAD_REQUEST.getCode(),
                "HTTP method is not supported",
                details,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
```

### 5.4 `common-lib/pom.xml`

Vì `GlobalExceptionHandler` dùng Spring Web, validation và servlet API, `common-lib` cần compile được các dependency này.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
    </dependency>

    <dependency>
        <groupId>jakarta.validation</groupId>
        <artifactId>jakarta.validation-api</artifactId>
    </dependency>

    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## 6. Service scan package

Vì `GlobalExceptionHandler` nằm trong package:

```text
com.tenantcore.common
```

nên các service phải scan package cha `com.tenantcore`.

### 6.1 `CoreServiceApplication.java`

```java
package com.tenantcore.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tenantcore")
public class CoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}
```

### 6.2 `LogiflowServiceApplication.java`

```java
package com.tenantcore.logiflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tenantcore")
public class LogiflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogiflowServiceApplication.class, args);
    }
}
```

Ghi chú:

```text
Gateway dùng Reactive WebFlux.
GlobalExceptionHandler kiểu Spring MVC không dùng trực tiếp cho Gateway.
Sau này cần tạo exception handler riêng cho gateway-service nếu muốn response lỗi gateway cũng chuẩn.
```

---

## 7. Dependency cho các service

### 7.1 `core-service`

Cần có:

```xml
<dependencies>
    <dependency>
        <groupId>com.tenantcore</groupId>
        <artifactId>common-lib</artifactId>
        <version>${project.version}</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
</dependencies>
```

### 7.2 `logiflow-service`

Cần có:

```xml
<dependencies>
    <dependency>
        <groupId>com.tenantcore</groupId>
        <artifactId>common-lib</artifactId>
        <version>${project.version}</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

Không thêm Flyway vào `logiflow-service` ở MVP.

### 7.3 `gateway-service`

Nên có:

```xml
<dependencies>
    <dependency>
        <groupId>com.tenantcore</groupId>
        <artifactId>common-lib</artifactId>
        <version>${project.version}</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

Không cần:

```text
Spring Web
Spring Data JPA
PostgreSQL Driver
Flyway
GraphQL
OAuth2 Client
```

---

## 8. Application config

### 8.1 Neon connection string

Connection string gốc:

```text
postgresql://neondb_owner:*********@ep-dry-silence-aoa09kmu.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
```

JDBC URL:

```text
jdbc:postgresql://ep-dry-silence-aoa09kmu.c-2.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require
```

Không commit password.

Dùng biến môi trường:

```text
NEON_DB_PASSWORD
```

### 8.2 `core-service/src/main/resources/application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: core-service

  datasource:
    url: jdbc:postgresql://ep-dry-silence-aoa09kmu.c-2.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require
    username: neondb_owner
    password: ${NEON_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

    hikari:
      maximum-pool-size: 3
      minimum-idle: 1
      connection-timeout: 30000
      idle-timeout: 300000
      max-lifetime: 900000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### 8.3 `logiflow-service/src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: logiflow-service

  datasource:
    url: jdbc:postgresql://ep-dry-silence-aoa09kmu.c-2.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require
    username: neondb_owner
    password: ${NEON_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

    hikari:
      maximum-pool-size: 3
      minimum-idle: 1
      connection-timeout: 30000
      idle-timeout: 300000
      max-lifetime: 900000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: false
```

### 8.4 `gateway-service/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service

  cloud:
    gateway:
      routes:
        - id: core-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/core/**,/api/auth/**,/api/users/**,/api/roles/**,/api/permissions/**,/api/files/**,/api/audit/**,/api/notifications/**

        - id: logiflow-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/logiflow/**
```

---

## 9. Set biến môi trường DB password

### 9.1 IntelliJ

```text
Edit Configurations
→ Chọn CoreServiceApplication
→ Environment variables
→ NEON_DB_PASSWORD=your_real_password
```

Làm tương tự cho:

```text
LogiflowServiceApplication
```

### 9.2 Windows PowerShell

```powershell
$env:NEON_DB_PASSWORD="your_real_password"
```

### 9.3 Git Bash / Linux / macOS

```bash
export NEON_DB_PASSWORD="your_real_password"
```

---

## 10. Flyway migration

### 10.1 Nguyên tắc

```text
core-service chạy Flyway
logiflow-service không chạy Flyway
gateway-service không dùng DB
```

Migration nằm tại:

```text
core-service/src/main/resources/db/migration/
```

File cần có:

```text
V001__create_core_tables.sql
V002__create_logiflow_tables.sql
V003__seed_initial_data.sql
```

### 10.2 `V001__create_core_tables.sql`

Nên chứa:

```text
CREATE EXTENSION pgcrypto
tenants
users
user_tenants
roles
permissions
user_roles
role_permissions
refresh_tokens
files
audit_logs
notifications
core indexes
updated_at trigger function
updated_at triggers
```

### 10.3 `V002__create_logiflow_tables.sql`

Nên chứa:

```text
logiflow_customers
logiflow_drivers
logiflow_vehicles
logiflow_orders
logiflow_delivery_assignments
logiflow_tracking_events
logiflow_cod_records
logiflow_reconciliations
logiflow indexes
updated_at triggers
```

### 10.4 `V003__seed_initial_data.sql`

Nên chứa:

```text
tenant demo
permissions IAM
permissions FILE
permissions AUDIT
permissions NOTIFICATION
permissions LOGIFLOW
roles OWNER / ADMIN / MEMBER
role_permissions cho OWNER / ADMIN / MEMBER
```

Lưu ý:

```text
Không seed admin user với password raw trong migration.
Admin user nên tạo bằng setup API hoặc script riêng có BCrypt.
```

---

## 11. Health API để test service

### 11.1 `CoreHealthController.java`

File:

```text
core-service/src/main/java/com/tenantcore/core/health/CoreHealthController.java
```

```java
package com.tenantcore.core.health;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoreHealthController {

    @GetMapping("/api/core/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("core-service is running");
    }

    @GetMapping("/api/core/error-test")
    public ApiResponse<String> errorTest() {
        throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
    }
}
```

### 11.2 `LogiflowHealthController.java`

File:

```text
logiflow-service/src/main/java/com/tenantcore/logiflow/health/LogiflowHealthController.java
```

```java
package com.tenantcore.logiflow.health;

import com.tenantcore.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogiflowHealthController {

    @GetMapping("/api/logiflow/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("logiflow-service is running");
    }
}
```

---

## 12. Chạy local/debug

### 12.1 Thứ tự chạy

```text
1. core-service      port 8081
2. logiflow-service  port 8082
3. gateway-service   port 8080
```

Gateway chạy sau cùng vì route sang 2 service còn lại.

### 12.2 Chạy trong IntelliJ

Mở các main class:

```text
CoreServiceApplication.java
LogiflowServiceApplication.java
GatewayServiceApplication.java
```

Bấm:

```text
Run hoặc Debug
```

### 12.3 Chạy bằng Maven từ root

```bash
mvn -pl core-service -am spring-boot:run
```

```bash
mvn -pl logiflow-service -am spring-boot:run
```

```bash
mvn -pl gateway-service -am spring-boot:run
```

Trong đó:

```text
-pl = chọn module cần chạy
-am = build kèm module phụ thuộc, ví dụ common-lib
```

---

## 13. Test API

### 13.1 Test trực tiếp service

```bash
curl http://localhost:8081/api/core/health
curl http://localhost:8082/api/logiflow/health
```

### 13.2 Test qua gateway

```bash
curl http://localhost:8080/api/core/health
curl http://localhost:8080/api/logiflow/health
```

### 13.3 Test exception handler

```bash
curl http://localhost:8081/api/core/error-test
```

Kết quả mong muốn:

```json
{
  "code": "TENANT_FORBIDDEN",
  "message": "Tenant code is invalid or not allowed",
  "details": null,
  "path": "/api/core/error-test",
  "timestamp": "..."
}
```

---

## 14. Build

### 14.1 Build toàn bộ

```bash
mvn clean install -DskipTests
```

### 14.2 Build một service kèm dependency

```bash
mvn -pl core-service -am clean install -DskipTests
```

```bash
mvn -pl logiflow-service -am clean install -DskipTests
```

```bash
mvn -pl gateway-service -am clean install -DskipTests
```

---

## 15. Checklist cho IDE Agent

### 15.1 Giai đoạn 1 — Chuẩn hóa project

```text
[ ] Kiểm tra parent pom.xml có packaging pom
[ ] Kiểm tra modules gồm common-lib, core-service, gateway-service, logiflow-service
[ ] Kiểm tra các module con trỏ đúng parent
[ ] Kiểm tra ${project.version} resolve được
[ ] Reload Maven
[ ] Build toàn bộ bằng mvn clean install -DskipTests
```

### 15.2 Giai đoạn 2 — Hoàn thiện common-lib

```text
[ ] Tạo ApiResponse
[ ] Tạo ErrorResponse
[ ] Tạo PageResponse
[ ] Tạo ErrorCode
[ ] Tạo BusinessException
[ ] Tạo TenantContext
[ ] Tạo UserContext
[ ] Tạo SecurityConstants
[ ] Tạo GlobalExceptionHandler
[ ] Bổ sung dependency compile cho common-lib
[ ] Build common-lib thành công
```

### 15.3 Giai đoạn 3 — Service bootstrapping

```text
[ ] core-service add common-lib dependency
[ ] logiflow-service add common-lib dependency
[ ] gateway-service add common-lib dependency nếu cần
[ ] core-service scanBasePackages = "com.tenantcore"
[ ] logiflow-service scanBasePackages = "com.tenantcore"
[ ] Tạo health API core
[ ] Tạo health API logiflow
[ ] Config port 8081 cho core
[ ] Config port 8082 cho logiflow
[ ] Config port 8080 cho gateway
[ ] Gateway route được /api/core/**
[ ] Gateway route được /api/logiflow/**
```

### 15.4 Giai đoạn 4 — Kết nối Neon PostgreSQL

```text
[ ] core-service thêm JPA + PostgreSQL + Flyway
[ ] logiflow-service thêm JPA + PostgreSQL
[ ] core-service application.yml dùng JDBC Neon URL
[ ] logiflow-service application.yml dùng JDBC Neon URL
[ ] Password dùng NEON_DB_PASSWORD
[ ] Không commit password
[ ] Set env var trong IntelliJ cho core-service
[ ] Set env var trong IntelliJ cho logiflow-service
```

### 15.5 Giai đoạn 5 — Flyway migration

```text
[ ] Tạo core-service/src/main/resources/db/migration/
[ ] Tạo V001__create_core_tables.sql
[ ] Tạo V002__create_logiflow_tables.sql
[ ] Tạo V003__seed_initial_data.sql
[ ] Chạy core-service để Flyway migrate
[ ] Kiểm tra flyway_schema_history trong Neon
[ ] Kiểm tra tenants có demo tenant
[ ] Kiểm tra permissions có IAM/FILE/AUDIT/NOTIFICATION/LOGIFLOW
[ ] Kiểm tra các bảng logiflow_* được tạo
```

### 15.6 Giai đoạn 6 — API mock trước khi làm DB service thật

```text
[ ] AuthController mock /api/auth/login
[ ] AuthController mock /api/auth/me
[ ] LogiflowOrderController mock POST /api/logiflow/orders
[ ] LogiflowOrderController mock GET /api/logiflow/orders/{id}
[ ] Test qua gateway
```

### 15.7 Giai đoạn 7 — API thật sau khi DB ổn

```text
[x] Tạo BaseEntity
[x] Tạo TenantEntity hoặc mapping tenant_code chuẩn
[x] Tạo Tenant entity/repository
[x] Tạo User entity/repository
[x] Tạo Role entity/repository
[x] Tạo Permission entity/repository
[x] Tạo RefreshToken entity/repository
[x] Làm BCrypt password
[x] Làm JWT service
[x] Làm login thật
[x] Làm refresh token thật
[x] Làm logout/revoke token
[x] Làm /api/auth/me thật
```

---

## 16. Nguyên tắc quan trọng cần giữ

### 16.1 Multi-tenant

```text
Mọi bảng nghiệp vụ phải có tenant_code.
Mọi query nghiệp vụ phải filter tenant_code.
Không tin hoàn toàn X-Tenant-Code từ frontend.
Tenant trong header phải khớp tenant trong JWT.
```

### 16.2 Migration

```text
Không sửa migration đã chạy trên DB có dữ liệu thật.
Nếu local/dev DB mới bắt đầu, có thể reset DB rồi chạy lại migration.
Nếu DB đã có dữ liệu, tạo migration version mới.
```

### 16.3 Service responsibility

```text
core-service:
  Auth
  Tenant
  User
  Role
  Permission
  File metadata
  Audit
  Notification

logiflow-service:
  Customer
  Driver
  Vehicle
  Order
  Delivery assignment
  Tracking event
  COD
  Reconciliation

gateway-service:
  Route
  JWT filter
  Tenant filter
  CORS
  Request logging
```

### 16.4 Chưa nên làm ở MVP

```text
Service discovery
Config server
Kafka
Elasticsearch
Kubernetes
Distributed transaction
Tách auth-service/user-service/permission-service riêng
MongoDB cho log
Multi database theo tenant
```

---

## 17. Prompt gợi ý cho IDE Agent

Có thể dùng prompt này trong IDE Agent:

```text
Bạn đang làm trong Maven multi-module project tenant-platform gồm common-lib, core-service, gateway-service, logiflow-service.

Hãy bám sát tài liệu này và triển khai theo từng bước nhỏ, không tự ý đổi kiến trúc.

Yêu cầu:
1. common-lib chứa DTO, exception, context, security constants dùng chung.
2. core-service là Spring Boot MVC app, port 8081, scanBasePackages = "com.tenantcore".
3. logiflow-service là Spring Boot MVC app, port 8082, scanBasePackages = "com.tenantcore".
4. gateway-service là Spring Cloud Gateway WebFlux app, port 8080.
5. core-service chạy Flyway migration.
6. logiflow-service dùng PostgreSQL nhưng tắt Flyway.
7. Database dùng Neon PostgreSQL qua env var NEON_DB_PASSWORD.
8. Không hard-code password hoặc secret.
9. Mọi bảng nghiệp vụ phải có tenant_code.
10. API response dùng ApiResponse/ErrorResponse từ common-lib.
11. Exception dùng BusinessException/ErrorCode/GlobalExceptionHandler.
12. Triển khai từng bước và đảm bảo mvn clean install -DskipTests chạy được sau mỗi nhóm thay đổi.
```

---

## 18. Thứ tự tiếp theo nên làm ngay

```text
1. Build common-lib thành công
2. Add common-lib vào core/logiflow
3. Chạy được core-service health API
4. Chạy được logiflow-service health API
5. Chạy được gateway-service route sang core/logiflow
6. Gắn Neon PostgreSQL vào core-service
7. Tạo Flyway migration V001/V002/V003
8. Chạy migration thành công
9. Code mock Auth API và LogiFlow Order API
10. Sau đó mới code entity/repository/service thật
```

---

## 19. Progress log (updated 2026-05-13)

```text
[x] Stage 1 - Maven multi-module normalized
[x] Stage 2 - common-lib DTO/exception/context/security classes in place
[x] Stage 2 - common-lib dependencies include spring-web, spring-context, jakarta-validation, jakarta-servlet-api
[x] Stage 3 - core-service scanBasePackages = com.tenantcore
[x] Stage 3 - logiflow-service scanBasePackages = com.tenantcore
[x] Stage 3 - core health APIs: /api/core/health and /api/core/error-test
[x] Stage 3 - logiflow health API: /api/logiflow/health
[x] Stage 3 - gateway port 8080 + routes for /api/core/** and /api/logiflow/**
[x] Stage 6 - mock auth APIs: POST /api/auth/login, GET /api/auth/me
[x] Stage 6 - mock logiflow order APIs: POST /api/logiflow/orders, GET /api/logiflow/orders/{id}
[x] Build check after each change group with mvn clean install -DskipTests
[x] Stage 5 runtime verify on Neon: completed on 2026-05-11 (DB auth fixed, Flyway validated 3 migrations, schema version 003, /api/core/health responded SUCCESS)
[x] Stage 7 completed (2026-05-13): BaseEntity + IAM entities/repositories + BCrypt/JWT + real auth flow (`/setup-password`, `/login`, `/me`, `/refresh`, `/logout`) all finalized
[x] Gateway integration note (2026-05-11): added gateway SecurityWebFilterChain to permit /api/** and /actuator/health for MVP routing tests (avoid gateway-level 401 before forwarding)
[x] Gateway E2E verify (2026-05-11): core health/logiflow health/auth setup-login-me-refresh-logout and logiflow order create/get all passed via :8080; refresh-after-logout returned 401 as expected
[x] Stage 7 progress (2026-05-11): logiflow order APIs transitioned from mock to DB-backed implementation in logiflow-service (entity/repository/service/controller, tenant-aware query by id+tenant_code)
[x] Stage 7 verify (2026-05-11): gateway test passed for DB-backed logiflow orders (create/get success, id+orderCode+tenantCode consistent)
[x] Stage 7 refactor (2026-05-11): applied layer separation by module (api contract, web controller impl, application facade, service) for core auth and logiflow order; smoke tests via gateway still passed
[x] Stage 7 extension (2026-05-11): added DB-backed order list/search API with pagination and tenant filter (GET /api/logiflow/orders with page/size/status/keyword), verified list-all and keyword filter success via gateway
[x] Stage 7 extension (2026-05-11): added order status update API PATCH /api/logiflow/orders/{id}/status (tenant-aware), verified create -> update COMPLETED -> get/list filter flow success via gateway
[x] Tooling update (2026-05-11): added Postman collection for gateway flow at postman/tenantcore-gateway-mvp.postman_collection.json (health/auth/order/status workflow, with auto variable capture for accessToken/refreshToken/orderId)
[x] Security hardening (2026-05-11): added core-service JWT authentication filter + stateless security; verified /api/auth/me requires valid bearer token (success with token, 401 without token via gateway)
[x] Security hardening (2026-05-11): added logiflow-service JWT filter + permission checks (VIEW/CREATE/UPDATE) + tenant header/token enforcement; verified 401 for missing token, 403 for tenant mismatch, and success for valid token+tenant via gateway
[x] Stage 7 logistics extension (2026-05-11): added assign/tracking/cod endpoints (POST /api/logiflow/orders/{id}/assign, /tracking, /cod) with tenant-aware security; verified create->assign->tracking->cod success and 401 for no-token assign via gateway
```
