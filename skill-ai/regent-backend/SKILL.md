---
name: regent-backend
description: "Backend development with design patterns, best practices, and provenance tracking. Use for Spring Boot, Java, API design, database optimization. Examples: 'Implement user service with proper patterns', 'Debug payment API error', 'Optimize database queries'"
---

# Backend Development with Re_gent

## When to Use

- Implementing new backend features (REST APIs, services, repositories)
- Refactoring backend code with proper design patterns
- Debugging backend errors (500 errors, exceptions, performance issues)
- API design and optimization
- Database query optimization and N+1 detection
- Applying SOLID principles and design patterns
- Code review with architecture quality focus

## Workflow

```
1. rgt init (if not initialized)                            → Initialize provenance tracking
2. rgt status                                                → Check current state
3. gitnexus_query({query: "feature/bug context"})            → Understand existing codebase
4. gitnexus_context({name: "target symbol"})                 → Get full context of target
5. gitnexus_impact({target, direction: "upstream"})          → Check blast radius BEFORE changes
6. APPLY DESIGN PATTERNS & BEST PRACTICES                    → Implement with quality
7. rgt log --session current -n 10                           → Review what was changed
8. gitnexus_detect_changes()                                 → Verify impact scope
9. RUN TESTS & BUILD                                         → Quality verification
10. If error → DEBUGGING WORKFLOW                            → Systematic error resolution
```

> **IMPORTANT:** Always run `gitnexus_impact` BEFORE making changes to understand dependencies.

## Design Patterns Checklist

### Creational Patterns

**Factory Pattern** - Use when object creation logic is complex or varies by type
```java
// Example: PaymentProcessorFactory
public interface PaymentProcessor {
    PaymentResult process(PaymentRequest request);
}

public class PaymentProcessorFactory {
    public PaymentProcessor create(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD -> new CreditCardProcessor();
            case PAYPAL -> new PayPalProcessor();
            case BANK_TRANSFER -> new BankTransferProcessor();
        };
    }
}
```

**Builder Pattern** - Use for complex object construction with many optional parameters
```java
// Example: QueryBuilder
public class UserQuery {
    private String name;
    private Integer minAge;
    private String email;
    private List<String> roles;
    
    public static class Builder {
        public Builder withName(String name) { ... }
        public Builder withMinAge(Integer age) { ... }
        public Builder withEmail(String email) { ... }
        public Builder withRoles(List<String> roles) { ... }
        public UserQuery build() { ... }
    }
}
```

**Singleton Pattern** - Use for shared resources (with thread-safety consideration)
```java
// Example: DatabaseConnectionPool
public class ConnectionPool {
    private static volatile ConnectionPool instance;
    private final HikariDataSource dataSource;
    
    private ConnectionPool() {
        this.dataSource = new HikariDataSource(config);
    }
    
    public static ConnectionPool getInstance() {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {
                    instance = new ConnectionPool();
                }
            }
        }
        return instance;
    }
}
```

### Structural Patterns

**Adapter Pattern** - Use for third-party API integration
```java
// Example: External payment gateway adapter
public interface PaymentGateway {
    PaymentResult charge(Money amount, PaymentMethod method);
}

public class StripeAdapter implements PaymentGateway {
    private final StripeClient stripeClient;
    
    @Override
    public PaymentResult charge(Money amount, PaymentMethod method) {
        // Adapt our interface to Stripe's API
        ChargeRequest stripeRequest = convertToStripeRequest(amount, method);
        StripeResponse response = stripeClient.charge(stripeRequest);
        return convertToPaymentResult(response);
    }
}
```

**Decorator Pattern** - Use for dynamic behavior extension
```java
// Example: Logging decorator for repository
public class LoggingUserRepository implements UserRepository {
    private final UserRepository delegate;
    private final Logger logger;
    
    @Override
    public User findById(Long id) {
        logger.info("Finding user by id: {}", id);
        User user = delegate.findById(id);
        logger.info("Found user: {}", user);
        return user;
    }
}
```

**Facade Pattern** - Use for subsystem simplification
```java
// Example: Order processing facade
@Service
public class OrderFacade {
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;
    private final NotificationService notificationService;
    
    public OrderResult processOrder(OrderRequest request) {
        // Simplify complex subsystem interactions
        inventoryService.reserve(request.getItems());
        PaymentResult payment = paymentService.charge(request.getPayment());
        ShippingLabel label = shippingService.createShipment(request.getAddress());
        notificationService.sendConfirmation(request.getCustomerId());
        return new OrderResult(payment, label);
    }
}
```

### Behavioral Patterns

**Strategy Pattern** - Use for algorithm selection at runtime
```java
// Example: Pricing strategy
public interface PricingStrategy {
    Money calculatePrice(Order order);
}

@Service
public class PricingService {
    private final Map<CustomerType, PricingStrategy> strategies;
    
    public Money calculatePrice(Order order, CustomerType type) {
        PricingStrategy strategy = strategies.get(type);
        return strategy.calculatePrice(order);
    }
}
```

**Observer Pattern** - Use for event handling
```java
// Example: Domain events
@Service
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;
    
    public void createOrder(OrderRequest request) {
        Order order = new Order(request);
        orderRepository.save(order);
        
        // Notify observers
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
    }
}

@Component
public class OrderCreatedListener {
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Send email, update inventory, etc.
    }
}
```

**Template Method Pattern** - Use for algorithm skeleton with customizable steps
```java
// Example: Data import template
public abstract class DataImporter<T> {
    public final ImportResult importData(InputStream input) {
        List<String> lines = readLines(input);
        validate(lines);
        List<T> entities = parse(lines);
        save(entities);
        return new ImportResult(entities.size());
    }
    
    protected abstract void validate(List<String> lines);
    protected abstract List<T> parse(List<String> lines);
    protected abstract void save(List<T> entities);
}
```

**Chain of Responsibility Pattern** - Use for request handling pipeline
```java
// Example: Request validation chain
public interface ValidationHandler {
    void setNext(ValidationHandler next);
    void validate(Request request);
}

public class AuthenticationHandler implements ValidationHandler {
    private ValidationHandler next;
    
    @Override
    public void validate(Request request) {
        if (!isAuthenticated(request)) {
            throw new UnauthorizedException();
        }
        if (next != null) {
            next.validate(request);
        }
    }
}
```

### Architectural Patterns

**Repository Pattern** - Use for data access abstraction
```java
// Example: User repository
public interface UserRepository {
    Optional<User> findById(Long id);
    List<User> findByRole(String role);
    User save(User user);
    void delete(Long id);
}

@Repository
public class JpaUserRepository implements UserRepository {
    private final EntityManager em;
    
    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }
}
```

**Service Layer Pattern** - Use for business logic organization
```java
// Example: User service with transaction management
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    public User registerUser(RegistrationRequest request) {
        // Business logic here
        validateEmail(request.getEmail());
        User user = new User(request);
        user = userRepository.save(user);
        emailService.sendWelcomeEmail(user);
        return user;
    }
}
```

**DTO Pattern** - Use for data transfer between layers
```java
// Example: User DTO
public record UserDTO(
    Long id,
    String username,
    String email,
    List<String> roles
) {
    public static UserDTO from(User user) {
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRoles().stream().map(Role::getName).toList()
        );
    }
}
```

## Backend Best Practices

### API Design

- [ ] **RESTful conventions**
  - GET for retrieval, POST for creation, PUT/PATCH for updates, DELETE for removal
  - Proper HTTP status codes (200, 201, 204, 400, 401, 403, 404, 500)
  - Resource-based URLs (`/users/{id}` not `/getUser?id=123`)

- [ ] **Consistent error response format**
```json
{
  "timestamp": "2026-05-16T03:20:07Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "errors": [
    {"field": "email", "message": "Invalid email format"}
  ]
}
```

- [ ] **API versioning strategy**
  - URL versioning: `/api/v1/users`
  - Header versioning: `Accept: application/vnd.api.v1+json`
  - Choose one and be consistent

- [ ] **Request/Response validation**
  - Use `@Valid` with Bean Validation annotations
  - Custom validators for complex rules
  - Return detailed validation errors

- [ ] **Pagination, filtering, sorting**
```java
@GetMapping("/users")
public Page<UserDTO> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String role,
    @RequestParam(defaultValue = "id,asc") String[] sort
) {
    // Implementation
}
```

- [ ] **Rate limiting and throttling**
  - Use `@RateLimiter` or custom interceptor
  - Return `429 Too Many Requests` with `Retry-After` header

### Database Optimization

- [ ] **N+1 query detection and fix**
```java
// BAD: N+1 query
List<User> users = userRepository.findAll();
users.forEach(user -> {
    List<Order> orders = orderRepository.findByUserId(user.getId()); // N queries!
});

// GOOD: Join fetch
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders")
List<User> findAllWithOrders();
```

- [ ] **Proper indexing strategy**
  - Index foreign keys
  - Index columns used in WHERE, JOIN, ORDER BY
  - Composite indexes for multi-column queries
  - Monitor slow query log

- [ ] **Transaction boundary optimization**
```java
// Keep transactions short and focused
@Transactional
public void processOrder(OrderRequest request) {
    // Database operations only
    Order order = orderRepository.save(new Order(request));
    inventoryRepository.decrementStock(request.getItems());
}

// External calls outside transaction
public void completeOrder(OrderRequest request) {
    Order order = processOrder(request); // Transactional
    emailService.sendConfirmation(order); // Non-transactional
}
```

- [ ] **Connection pool configuration**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

- [ ] **Migration safety (backward compatible)**
  - Add columns as nullable first, then make NOT NULL in next migration
  - Rename in two steps: add new column, copy data, drop old column
  - Test migrations on production-like data

### Security

- [ ] **Input validation and sanitization**
```java
@PostMapping("/users")
public UserDTO createUser(@Valid @RequestBody CreateUserRequest request) {
    // @Valid triggers validation
    // Sanitize HTML input to prevent XSS
    String sanitizedBio = HtmlUtils.htmlEscape(request.getBio());
}
```

- [ ] **SQL injection prevention**
  - Use parameterized queries (JPA/JDBC PreparedStatement)
  - Never concatenate user input into SQL strings

- [ ] **Authentication/Authorization**
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<UserDTO> getAllUsers() { }

@PreAuthorize("hasRole('USER') and #userId == authentication.principal.id")
@GetMapping("/users/{userId}")
public UserDTO getUser(@PathVariable Long userId) { }
```

- [ ] **Sensitive data encryption**
  - Encrypt passwords with BCrypt (never plain text or MD5)
  - Encrypt PII at rest
  - Use HTTPS for data in transit

- [ ] **CORS configuration**
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://app.example.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        // ...
    }
}
```

### Code Quality

- [ ] **SOLID Principles**
  - **S**ingle Responsibility: One class, one reason to change
  - **O**pen/Closed: Open for extension, closed for modification
  - **L**iskov Substitution: Subtypes must be substitutable for base types
  - **I**nterface Segregation: Many specific interfaces > one general interface
  - **D**ependency Inversion: Depend on abstractions, not concretions

- [ ] **DRY (Don't Repeat Yourself)**
  - Extract common logic into utility methods
  - Use inheritance or composition for shared behavior
  - Avoid copy-paste code

- [ ] **Proper exception handling**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        logger.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(new ErrorResponse("Internal server error"));
    }
}
```

- [ ] **Logging strategy (structured logging)**
```java
// Use SLF4J with structured logging
logger.info("User created", 
    kv("userId", user.getId()),
    kv("username", user.getUsername()),
    kv("action", "user.created")
);

// Log levels:
// ERROR: System errors requiring immediate attention
// WARN: Recoverable issues, deprecated usage
// INFO: Important business events
// DEBUG: Detailed diagnostic information
// TRACE: Very detailed diagnostic information
```

- [ ] **Unit test coverage (>80%)**
```java
@Test
void shouldCreateUserWithValidData() {
    // Given
    CreateUserRequest request = new CreateUserRequest("john", "john@example.com");
    
    // When
    UserDTO result = userService.createUser(request);
    
    // Then
    assertThat(result.username()).isEqualTo("john");
    verify(userRepository).save(any(User.class));
}
```

## Debugging Workflow

```
1. ERROR OCCURS (exception, 500 error, unexpected behavior)
   ↓
2. rgt log --session current -n 20                           → Review recent changes
   ↓
3. rgt show <step-hash>                                      → Get full context of suspect step
   ↓
4. gitnexus_query({query: "error message/symptom"})          → Find related code
   ↓
5. gitnexus_context({name: "suspect function"})              → Analyze dependencies
   ↓
6. READ gitnexus://repo/{name}/process/{name}                → Trace execution flow
   ↓
7. ANALYZE ROOT CAUSE with multiple fix approaches           → Systematic analysis
   ↓
8. RECOMMEND BEST FIX with trade-offs explanation            → Informed decision
   ↓
9. IMPLEMENT FIX                                             → Apply solution
   ↓
10. If fix fails → rgt blame <file>:<line>                   → Identify problematic change
    ↓
11. MANUAL ROLLBACK specific lines                           → Surgical recovery
```

## Error Analysis Template

When debugging, always provide structured analysis:

```markdown
## Error Analysis

**Symptom:** [Error message or unexpected behavior]

**Stack Trace:** [Relevant stack trace excerpt]

**Root Cause:** [Technical explanation of why this happened]

**Affected Components:**
- [Component 1] - [Why it's affected]
- [Component 2] - [Why it's affected]

**Fix Approaches:**

### Approach 1: [Name]
**Pros:**
- [Benefit 1]
- [Benefit 2]

**Cons:**
- [Drawback 1]
- [Drawback 2]

**Risk:** [LOW/MEDIUM/HIGH]

**Implementation:**
1. [Step 1]
2. [Step 2]

### Approach 2: [Name]
**Pros:**
- [Benefit 1]

**Cons:**
- [Drawback 1]

**Risk:** [LOW/MEDIUM/HIGH]

**Implementation:**
1. [Step 1]

**Recommended:** Approach [X] because [reasoning with technical justification]
```

## Debugging Patterns

| Symptom | Investigation Steps | Common Causes |
|---------|-------------------|---------------|
| NullPointerException | `rgt blame` on line → check initialization | Missing null check, uninitialized field |
| 500 Internal Server Error | Check logs → `gitnexus_query` for handler | Unhandled exception, validation failure |
| Slow API response | Profile query → check N+1 | Missing index, N+1 query, inefficient algorithm |
| Transaction rollback | Check transaction boundaries | Exception in @Transactional method |
| Connection pool exhausted | Check pool config → find leaks | Unclosed connections, pool too small |
| Deadlock | Analyze lock order → `gitnexus_context` | Inconsistent lock acquisition order |

## Tools Reference

### Re_gent Commands

```bash
rgt init                          # Initialize .regent/ tracking
rgt status                        # Check current state
rgt log                           # Show all steps
rgt log --session current         # Current session only
rgt log -n 10                     # Last 10 steps
rgt log --json                    # JSON output for parsing
rgt show <step-hash>              # Full context of a step
rgt blame <file>                  # Per-line provenance
rgt blame <file>:<line>           # Specific line provenance
rgt sessions                      # List active sessions
```

### GitNexus Tools

```javascript
gitnexus_query({query: "payment processing"})
// → Find execution flows related to concept

gitnexus_context({name: "processPayment"})
// → 360-degree view: callers, callees, processes

gitnexus_impact({target: "validateUser", direction: "upstream"})
// → Blast radius: what depends on this

gitnexus_detect_changes({scope: "staged"})
// → What do current git changes affect

gitnexus_rename({symbol_name: "oldName", new_name: "newName", dry_run: true})
// → Safe multi-file rename
```

## Examples

### Example 1: Implement User Registration with Proper Patterns

```
User request: "Implement user registration API"

1. rgt init && rgt status
2. gitnexus_query({query: "user authentication registration"})
   → Found: LoginFlow, but no registration
3. gitnexus_context({name: "UserService"})
   → Callers: LoginController, AuthController
4. Design with patterns:
   - Builder Pattern for User entity (many optional fields)
   - Strategy Pattern for password validation (different rules)
   - Repository Pattern for data access
   - DTO Pattern for request/response
5. Implementation:
   - Create RegistrationRequest DTO with @Valid
   - Create UserRegistrationService with @Transactional
   - Apply password encryption with BCrypt
   - Add email validation
   - Implement proper error handling
6. rgt log -n 5 → Review changes
7. gitnexus_detect_changes() → Verify impact
8. Run tests → All pass
```

### Example 2: Debug "Payment API returns 500 intermittently"

```
1. rgt log --session current -n 20
   → See recent changes to PaymentController, PaymentService
2. rgt show <step-hash-of-payment-change>
   → Full context: added external API call without timeout
3. gitnexus_query({query: "payment processing error"})
   → Processes: CheckoutFlow, RefundFlow
4. gitnexus_context({name: "processPayment"})
   → Outgoing calls: validateCard, chargeStripe (external!)
5. Check logs: "SocketTimeoutException" from Stripe API

## Error Analysis

**Symptom:** 500 Internal Server Error on POST /api/payments (intermittent)

**Root Cause:** External Stripe API call has no timeout, causing thread blocking when Stripe is slow

**Affected Components:**
- PaymentService.processPayment() - Makes the external call
- CheckoutFlow - Depends on payment processing
- Thread pool - Threads get exhausted

**Fix Approaches:**

### Approach 1: Add timeout to HTTP client
**Pros:**
- Simple fix
- Prevents indefinite blocking
**Cons:**
- Still blocks thread during timeout period
**Risk:** LOW
**Implementation:**
1. Configure RestTemplate with timeout
2. Add retry logic with exponential backoff

### Approach 2: Async processing with timeout
**Pros:**
- Non-blocking
- Better resource utilization
**Cons:**
- More complex (requires async handling)
- Changes API contract (returns 202 Accepted)
**Risk:** MEDIUM
**Implementation:**
1. Use @Async for payment processing
2. Return 202 with tracking ID
3. Client polls for result

**Recommended:** Approach 1 because it's simpler, fixes the immediate issue, and doesn't require API contract changes. We can consider Approach 2 later if we need better scalability.

6. Implement Approach 1:
   - Add timeout configuration
   - Add circuit breaker pattern
7. Test with slow network simulation
8. Deploy and monitor
```

### Example 3: Optimize Slow User List API

```
1. Profile endpoint: 2.5s response time
2. rgt log → Recent changes to UserController
3. gitnexus_context({name: "UserController.getUsers"})
   → Calls: userRepository.findAll(), orderRepository.findByUserId()
4. Analyze queries: N+1 problem detected!
   - 1 query to fetch users
   - N queries to fetch orders for each user
5. Fix approaches:

### Approach 1: Join fetch
**Pros:** Single query, simple
**Cons:** Cartesian product if user has many orders
**Risk:** LOW

### Approach 2: Batch fetch with @BatchSize
**Pros:** Reduces to 2 queries, no cartesian product
**Cons:** Still 2 queries
**Risk:** LOW

### Approach 3: Separate endpoint for orders
**Pros:** Clean separation, client controls loading
**Cons:** Requires API change
**Risk:** MEDIUM

**Recommended:** Approach 2 (batch fetch) - good balance of performance and simplicity

6. Implement:
   @BatchSize(size = 25)
   @OneToMany(mappedBy = "user")
   private List<Order> orders;
7. Test: Response time reduced to 180ms
8. gitnexus_detect_changes() → Only UserRepository affected
9. Commit with detailed explanation
```

## Checklist for Every Backend Task

Before starting:
- [ ] `rgt init` (if not initialized)
- [ ] `gitnexus_query` to understand existing code
- [ ] `gitnexus_impact` to check blast radius

During implementation:
- [ ] Apply appropriate design patterns
- [ ] Follow SOLID principles
- [ ] Add proper error handling
- [ ] Add logging at key points
- [ ] Write unit tests

Before committing:
- [ ] `rgt log` to review changes
- [ ] `gitnexus_detect_changes` to verify impact
- [ ] Run all tests
- [ ] Check code coverage
- [ ] Review for security issues

If error occurs:
- [ ] Follow debugging workflow
- [ ] Provide structured error analysis
- [ ] Consider multiple fix approaches
- [ ] Recommend best fix with reasoning
- [ ] Use `rgt blame` for rollback if needed

## Integration with GitNexus

Always combine Re_gent provenance with GitNexus impact analysis:

```
1. gitnexus_impact({target: "UserService", direction: "upstream"})
   → d=1: UserController, AdminController (WILL BREAK)
   → d=2: AuthFilter, AuditLogger (LIKELY AFFECTED)

2. rgt blame src/service/UserService.java
   → Lines 42-58: Added by prompt "implement email verification"
   → Lines 89-102: Added by prompt "add role-based access"

3. ASSESS RISK with full context:
   - 2 direct callers (d=1)
   - Recent changes from 2 different prompts
   - Risk: MEDIUM
   - Recommendation: Add integration tests before changing
```

## Tips for Success

1. **Always run impact analysis first** - Understand dependencies before changing code
2. **Use design patterns appropriately** - Don't over-engineer, but don't under-engineer either
3. **Keep transactions short** - Only database operations inside @Transactional
4. **Log structured data** - Use key-value pairs for easy searching
5. **Test with realistic data** - Edge cases, large datasets, concurrent requests
6. **Monitor after deployment** - Check logs, metrics, error rates
7. **Document non-obvious decisions** - Why you chose this approach over alternatives
8. **Use `rgt blame` for archaeology** - Understand why code was written this way

## Troubleshooting

**Re_gent not tracking changes:**
- Check `.regent/` directory exists
- Verify hooks are configured: `rgt status`
- Restart Claude Code to reload hooks

**GitNexus index stale:**
- Run `npx gitnexus analyze` in terminal
- Restart Claude Code to reload MCP server

**Performance issues:**
- Check `.regent/` size: `du -sh .regent/`
- Clean old sessions if needed
- Consider adding `.regent/` to `.gitignore`

**Merge conflicts in provenance:**
- Re_gent tracks per-session, conflicts are rare
- If occurs, resolve git conflicts normally
- Re-run `rgt init` if needed
