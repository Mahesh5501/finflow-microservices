# Java Backend Interview Master Question Bank

**Target:**  
**5 YOE Java Backend Engineer | 25+–30+ LPA**

This is the master interview question bank for Java Backend / SDE-II preparation.

The goal is to prepare for interviews across:

- Product-based companies
- SaaS companies
- FinTech
- E-commerce
- Gaming / iGaming
- Enterprise backend roles
- Platform / distributed systems roles
- Companies such as ARRISE and similar organizations

The questions are intentionally **company-neutral**. Company-specific interview questions should be treated as additional practice, not as a separate preparation roadmap.

---

# How We Will Study

We will **not study all sections at once**.

We will select one topic at a time and follow this learning flow:

> **Simple explanation → Why → Example → Code → Dry run → Your practice → Correction → Interview questions → Tricky follow-ups → Real-world scenario → Project connection**

For every major topic, the goal is not memorization.

We will understand:

> **WHAT → HOW → WHY → EDGE CASE → COMPARISON → CODING → CONCURRENCY/PERFORMANCE → REAL PROJECT SCENARIO → FOLLOW-UP**

For DSA:

> **Pattern recognition → Brute force → Optimization → Java solution → Dry run → Complexity → Edge cases → Variations → Interview follow-ups**

For project questions:

> Answers should be based on actual project experience rather than generic memorized answers.

---

# 1. Core Java

## OOP

- What are the four pillars of OOP?
- Give a real backend example for each OOP principle.
- What is abstraction?
- What is encapsulation?
- Abstraction vs encapsulation?
- Inheritance vs composition?
- Why is composition often preferred over inheritance?
- What is polymorphism?
- Compile-time vs runtime polymorphism?
- Method overloading vs method overriding?
- Can overloaded methods differ only by return type?
- Can static methods be overridden?
- Can private methods be overridden?
- Can final methods be overridden?
- Can constructors be overridden?
- What happens with `Parent p = new Child()`?
- What happens when parent and child contain fields with the same name?
- What happens when parent and child contain static methods with the same name?
- What happens when parent and child contain instance methods with the same signature?
- Field hiding vs method overriding?
- Why are static methods resolved using the reference type?
- Why are instance methods resolved using the actual object type?
- What is method hiding?
- What is dynamic method dispatch?
- Why can't static methods participate in runtime polymorphism?
- What is the difference between `this` and `super`?
- Why must `this()` or `super()` be the first statement in a constructor?
- Can `this()` and `super()` be used together in the same constructor?
- What happens if neither `this()` nor `super()` is explicitly written?
- Interface vs abstract class?
- When would you choose an interface?
- When would you choose an abstract class?
- Can an abstract class have constructors?
- Can an abstract class have static methods?
- Can an interface have fields?
- Can an interface have default methods?
- Can an interface have static methods?
- Can an interface have private methods?
- What happens when two interfaces contain the same default method?
- How do you resolve a default-method conflict?
- Can an interface extend another interface?
- Can a class implement multiple interfaces?

## Object / equals / hashCode

- What methods come from `Object`?
- What is the difference between `==` and `equals()`?
- What is the equals contract?
- What is the hashCode contract?
- Why must equal objects have the same hashCode?
- Can two unequal objects have the same hashCode?
- What is a hash collision?
- What happens if equals is overridden but hashCode is not?
- What happens if hashCode is overridden but equals is not?
- Why is this important for HashMap and HashSet?
- How should equals/hashCode be designed for mutable objects?
- What happens if an object used as a HashMap key is mutated?
- How does Lombok `@EqualsAndHashCode` affect this?
- How do Java records implement equality?

## Strings

- Why is String immutable?
- What is the String Pool?
- String literal vs `new String()`?
- What happens when two identical string literals are created?
- What does `intern()` do?
- `==` vs `equals()` for String?
- Why is String immutable from a security perspective?
- How does immutability help caching?
- How does immutability help thread safety?
- String vs StringBuilder vs StringBuffer?
- Why is StringBuilder faster than StringBuffer?
- When would you use StringBuffer?
- How does repeated String concatenation behave?
- What happens internally with String concatenation?
- How does String work in modern Java?
- Can a String be changed after creation?

## Constructors / keywords

- What is a constructor?
- Constructor vs method?
- Can constructors be overloaded?
- Can constructors be inherited?
- Can constructors be private?
- Why would you use a private constructor?
- `final` vs `finally` vs `finalize()`?
- Is `finalize()` recommended?
- What does `final` mean for variables?
- What does `final` mean for methods?
- What does `final` mean for classes?
- Can a final reference point to a mutable object?
- `this` vs `super`?

## Exceptions

- Checked vs unchecked exceptions?
- Exception vs Error?
- RuntimeException vs checked Exception?
- `throw` vs `throws`?
- Can you throw a checked exception without declaring it?
- How does exception propagation work?
- How does try-catch-finally work?
- Can finally be skipped?
- What happens if `return` is used inside try?
- What happens if finally also contains return?
- Why should return inside finally generally be avoided?
- Try-with-resources?
- How does try-with-resources close resources?
- What are suppressed exceptions?
- How would you design custom exceptions?
- Should business exceptions extend RuntimeException?

## Object copying

- Shallow copy vs deep copy?
- Clone vs copy constructor?
- Why is `Cloneable` often avoided?
- How would you safely copy nested mutable objects?

---

# 2. Collections

## ArrayList / LinkedList

- ArrayList vs LinkedList?
- How does ArrayList work internally?
- How does ArrayList grow?
- What is the default capacity?
- What is the resizing cost?
- Why is ArrayList usually preferred over LinkedList?
- What is random access?
- Why is ArrayList O(1) for indexed access?
- Why is insertion in the middle O(n)?
- When would LinkedList actually be useful?
- ArrayList vs array?

## HashMap

- What is HashMap?
- How does HashMap work internally?
- What happens during `put()`?
- What happens during `get()`?
- How is a bucket calculated?
- What is a hash collision?
- What happens when two keys have the same hash?
- How are collisions handled?
- What changed in HashMap after Java 8?
- What is treeification?
- When does a bucket become a tree?
- Why is a red-black tree used?
- What is load factor?
- What is initial capacity?
- How does resizing work?
- What happens when HashMap exceeds its threshold?
- Why should HashMap keys be immutable?
- What happens if a mutable key changes after insertion?
- What happens if equals/hashCode are implemented incorrectly?
- What is the expected time complexity of HashMap operations?
- What is the worst-case complexity?
- Why is HashMap not thread-safe?
- What happens when multiple threads modify HashMap concurrently?

## HashSet

- How does HashSet work internally?
- Why does HashSet use HashMap internally?
- How does HashSet prevent duplicates?
- How do equals/hashCode affect HashSet?
- Can HashSet contain null?
- HashSet vs LinkedHashSet?
- HashSet vs TreeSet?

## Map comparisons

- HashMap vs Hashtable?
- HashMap vs ConcurrentHashMap?
- HashMap vs LinkedHashMap?
- HashMap vs TreeMap?
- When would you choose each?
- Can HashMap contain null keys?
- Can HashMap contain null values?
- Can ConcurrentHashMap contain null keys or values?
- Why does ConcurrentHashMap not allow null?

## TreeMap / TreeSet

- How does TreeMap work internally?
- Why is TreeMap ordered?
- TreeMap vs HashMap?
- TreeSet vs HashSet?
- What is the complexity of TreeMap operations?
- What happens if the comparator is inconsistent with equals?

## Comparable / Comparator

- Comparable vs Comparator?
- When would you use Comparable?
- When would you use Comparator?
- Can multiple sorting strategies be defined?
- How do you sort by multiple fields?
- What happens if comparator returns inconsistent results?

## Iteration

- What is fail-fast iteration?
- What is weakly consistent iteration?
- Is "fail-safe" an official Java collection term?
- Why does ConcurrentHashMap not throw ConcurrentModificationException in the same way?
- What happens when a collection is modified during iteration?
- Iterator vs ListIterator?

## Collection selection

- How do you choose a collection for a requirement?
- ArrayList vs HashSet?
- HashSet vs TreeSet?
- HashMap vs TreeMap?
- Queue vs Deque?
- When would you use PriorityQueue?
- When would you use ConcurrentHashMap?

---

# 3. Java 8–17+

## Functional Programming

- What is a functional interface?
- What is `@FunctionalInterface`?
- What is a lambda expression?
- How does lambda work internally?
- What is a method reference?
- Predicate?
- Function?
- Consumer?
- Supplier?
- BiFunction?
- UnaryOperator?
- BinaryOperator?

## Streams

- What is a Stream?
- Stream vs Collection?
- How does a Stream work internally?
- Intermediate vs terminal operations?
- `filter()` vs `map()`?
- `map()` vs `flatMap()`?
- `findFirst()` vs `findAny()`?
- `reduce()` use cases?
- `collect()`?
- `groupingBy()`?
- `partitioningBy()`?
- `distinct()`?
- `sorted()`?
- Lazy evaluation?
- Why are streams lazy?
- Can a stream be reused?
- What happens if you try to reuse a consumed stream?
- When should streams not be used?
- Why can excessive stream usage hurt readability or performance?
- How do parallel streams work?
- What is the ForkJoinPool relationship?
- When can parallel streams hurt performance?
- Why should parallel streams be used carefully in backend applications?

## Optional

- Why does Optional exist?
- When should Optional be used?
- When should Optional not be used?
- `orElse()` vs `orElseGet()`?
- `orElseThrow()`?
- Can Optional be used as a field?
- Can Optional be used as a method parameter?
- Why should Optional generally not be serialized?
- Common Optional mistakes?

## Java 8 production usage

- Which Java 8 features have you used in production?
- Where have you used streams?
- Where have you used CompletableFuture?
- Where have you used functional interfaces?
- Where have you used Optional?
- Where would you avoid streams?

## Java 9–17

- What Java 9+ features do you know?
- What are Java records?
- Why were records introduced?
- Record vs normal POJO?
- What methods does a record automatically provide?
- Can records be mutable?
- Can records have methods?
- Can records implement interfaces?
- What are sealed classes?
- Sealed class vs final class?
- What problem do sealed classes solve?
- Pattern matching features?
- Text blocks?
- Private interface methods?
- What Java 17 features have you used or would use in production?

## Modern Java

- Virtual Threads?
- When are virtual threads useful?
- Virtual threads vs platform threads?
- What workloads benefit from virtual threads?
- What workloads do not automatically benefit?
- Structured concurrency concepts?
- What should you consider when moving an existing service to newer Java?

---

# 4. Concurrency & Multithreading

## Fundamentals

- Process vs thread?
- What is a thread?
- Why use multiple threads?
- What is concurrency?
- What is parallelism?
- CPU-bound vs I/O-bound work?
- What are thread states?
- What causes context switching?
- Why can excessive context switching hurt performance?

## Runnable / Callable / Future

- Runnable vs Callable?
- How does Callable return a result?
- What is Future?
- What are Future's limitations?
- Future vs CompletableFuture?

## ExecutorService

- Why use ExecutorService?
- How does a thread pool work?
- FixedThreadPool?
- CachedThreadPool?
- ScheduledThreadPool?
- What happens when the thread pool is exhausted?
- Queue capacity and rejection policies?
- What is `RejectedExecutionException`?
- Why should unlimited thread creation be avoided?
- How do you size a thread pool?
- CPU-bound vs I/O-bound pool sizing?
- What happens if a pool has too few threads?
- What happens if it has too many?

## synchronized

- What does synchronized guarantee?
- What is mutual exclusion?
- What is visibility?
- What is an object monitor?
- Object lock vs class lock?
- Synchronized instance method vs static synchronized method?
- What happens when two threads synchronize on different objects?
- Is synchronized reentrant?

## volatile

- What does volatile guarantee?
- What does volatile NOT guarantee?
- Why doesn't volatile make `i++` atomic?
- What is visibility?
- What is happens-before?
- When would you use volatile?
- Volatile vs AtomicInteger?

## Atomic classes

- What is CAS?
- How does Compare-And-Swap work?
- AtomicInteger?
- AtomicLong?
- AtomicReference?
- AtomicInteger vs synchronized?
- AtomicReference use case?
- What is ABA problem?
- AtomicLong vs LongAdder?
- When is LongAdder better?

## Locks

- Lock vs synchronized?
- ReentrantLock?
- What does reentrant mean?
- `tryLock()` use case?
- Lock fairness?
- How should locks be released safely?
- ReadWriteLock?
- When can ReadWriteLock improve performance?
- StampedLock?
- Optimistic read locking?

## Synchronizers

- CountDownLatch?
- CyclicBarrier?
- CountDownLatch vs CyclicBarrier?
- Semaphore?
- How can Semaphore limit concurrent access?
- Phaser?
- When would you use each synchronizer?

## Concurrent collections

- ConcurrentHashMap?
- Why is ConcurrentHashMap safer than HashMap for concurrent access?
- How does ConcurrentHashMap support concurrency?
- ConcurrentLinkedQueue?
- BlockingQueue?
- ArrayBlockingQueue vs LinkedBlockingQueue?

## CompletableFuture

- What is CompletableFuture?
- Future vs CompletableFuture?
- `supplyAsync()` vs `runAsync()`?
- `thenApply()`?
- `thenCompose()`?
- `thenCombine()`?
- `allOf()`?
- `anyOf()`?
- `handle()`?
- `exceptionally()`?
- `whenComplete()`?
- How do you handle exceptions?
- How do you implement timeout?
- How do you use a custom Executor?
- Which thread executes a continuation?
- What happens if you don't provide an executor?
- How can CompletableFuture cause thread-pool exhaustion?
- How do you handle partial failure?
- How would you implement scatter-gather?

## Concurrency problems

- What is a race condition?
- Give a real backend race-condition example.
- How do you prevent a race condition?
- What is deadlock?
- Four conditions required for deadlock?
- How do you prevent deadlock?
- How do you detect deadlock?
- How would you investigate deadlock using thread dumps/jstack?
- Starvation vs deadlock?
- Livelock vs deadlock?
- What is thread starvation?
- What is thread safety?
- How do you make a class thread-safe?
- How does immutability improve thread safety?
- How do you test concurrent code?
- How would you synchronize access to a shared resource?

## Advanced concurrency

- ForkJoinPool?
- Work stealing?
- How does parallelism work in ForkJoinPool?
- Virtual Threads?
- ThreadLocal?
- What problems can ThreadLocal cause?
- ThreadLocal memory leaks?
- Interrupts?
- How should interruption be handled?
- `wait()` vs `sleep()`?
- `wait()` vs `notify()` vs `notifyAll()`?
- Why must wait/notify be used with a monitor?
- Spurious wakeups?
- Producer-consumer problem?
- Throughput vs latency?
- How does synchronization affect throughput?
- What happens when many threads contend for the same lock?

---

# 5. Spring / Spring Boot

## Spring fundamentals

- What is IoC?
- What is Dependency Injection?
- Constructor vs setter vs field injection?
- Why is constructor injection preferred?
- What is a Spring Bean?
- How does Spring create and manage beans?
- Bean lifecycle?
- Bean scopes?
- Singleton vs prototype?
- Request/session scopes?
- `@Component` vs `@Service` vs `@Repository`?
- `@Bean` vs `@Component`?
- `@Primary` vs `@Qualifier`?
- Circular dependencies?
- How can circular dependencies be avoided?

## Spring Boot

- What is Spring Boot?
- Spring vs Spring Boot?
- What are starter dependencies?
- How does auto-configuration work?
- What is `@SpringBootApplication`?
- What does `@SpringBootApplication` contain?
- How does component scanning work?
- How does Spring decide which auto-configurations to apply?
- What are conditional beans?
- `@ConditionalOnProperty`?
- Profiles?
- Configuration properties?
- Environment variables?
- Externalized configuration?

## REST controllers

- `@RestController` vs `@Controller`?
- `@RequestParam` vs `@PathVariable` vs `@RequestBody`?
- How does request validation work?
- `@Valid` vs `@Validated`?
- How do you validate nested objects?
- How do you return validation errors?
- `@ControllerAdvice`?
- `@ExceptionHandler`?
- Global exception handling?
- Filter vs interceptor?
- When would you use each?

## AOP / Proxies

- What is AOP?
- Where is AOP useful?
- What are cross-cutting concerns?
- How does Spring AOP work?
- JDK dynamic proxy vs CGLIB?
- Why does `@Transactional` rely on proxies?
- What is self-invocation?
- Why can self-invocation bypass `@Transactional`?
- How do you solve self-invocation issues?

## Transactions

- What does `@Transactional` actually do?
- How does Spring manage transactions?
- Transaction propagation?
- REQUIRED?
- REQUIRES_NEW?
- SUPPORTS?
- NOT_SUPPORTED?
- MANDATORY?
- NEVER?
- NESTED?
- Transaction isolation?
- READ_UNCOMMITTED?
- READ_COMMITTED?
- REPEATABLE_READ?
- SERIALIZABLE?
- Common `@Transactional` pitfalls?
- What happens when a runtime exception occurs?
- Rollback rules?
- Checked exception rollback?
- Read-only transaction?
- Transaction boundaries?
- Why should transactions generally be short?
- Transaction + external API call: what problems can occur?

## Spring Cache

- What is Spring caching?
- `@Cacheable`?
- `@CachePut`?
- `@CacheEvict`?
- Cache-aside pattern?
- Cache invalidation?
- Stale data?
- Cache stampede?
- Cache penetration?
- Cache breakdown?
- Local cache vs distributed cache?

## Spring Security

- Authentication vs authorization?
- Spring Security filter chain?
- JWT authentication flow?
- Session-based authentication?
- JWT vs session?
- Access token vs refresh token?
- Role vs authority?
- How do you secure REST APIs?
- CORS vs CSRF?
- Where should authentication happen in a microservice architecture?

## Spring diagnostics

- Spring Actuator?
- Health endpoints?
- Metrics?
- Readiness vs liveness?
- Production diagnostics?

## Advanced Spring

- Application events?
- `@TransactionalEventListener`?
- Feign clients?
- Spring Cloud Config?
- WebFlux?
- Reactive programming?
- Backpressure?
- Reactor basics?
- Hibernate L2 cache?
- Flyway?
- Liquibase?
- Spring Batch?
- When would you use Spring Batch?

---

# 6. REST / gRPC / API Design

## REST

- What is REST?
- REST principles?
- Resource-oriented API design?
- Statelessness?
- PUT vs PATCH?
- POST vs PUT?
- GET vs POST?
- Idempotent HTTP methods?
- Safe HTTP methods?
- HTTP status codes?
- 200 vs 201?
- 400 vs 422?
- 401 vs 403?
- 404 vs 409?
- 429?
- 500 vs 502 vs 503 vs 504?

## API design

- How do you design a good REST API?
- Resource naming?
- Request validation?
- Error response design?
- API versioning?
- URL vs header vs content negotiation versioning?
- Pagination?
- Offset vs cursor pagination?
- Sorting/filtering?
- Backward compatibility?
- Contract compatibility?
- API deprecation?
- Rate limiting?
- Request size limits?
- Timeout handling?

## Security

- Authentication vs authorization?
- JWT vs session?
- OAuth2 basics?
- API keys?
- HMAC?
- TLS?
- mTLS?
- How do you prevent replay attacks?
- Input validation?
- Input sanitization?
- SQL injection?
- Sensitive information in logs?

## gRPC

- REST vs gRPC?
- Why use gRPC?
- What is Protobuf?
- Why is Protobuf used?
- Unary vs streaming RPC?
- Client streaming?
- Server streaming?
- Bidirectional streaming?
- gRPC status codes?
- gRPC deadlines?
- gRPC retries?
- How do you maintain backward compatibility in Protobuf?
- When is gRPC better than REST?
- When is REST better than gRPC?

## Service-to-service communication

- How do you handle timeouts?
- How do you handle retries?
- Exponential backoff?
- Jitter?
- Circuit breaker?
- What should not be retried?
- How do you prevent retry storms?
- How do you handle downstream failures?

---

# 7. Microservices & Distributed Systems

## Architecture

- Monolith vs microservices?
- Advantages/disadvantages of microservices?
- When should you NOT use microservices?
- How do you define service boundaries?
- How do you identify bounded contexts?
- Database per service?
- Shared database problems?
- Synchronous vs asynchronous communication?

## API Gateway

- What is an API Gateway?
- Why use one?
- Authentication at gateway vs service?
- Rate limiting at gateway?
- Routing?
- Request transformation?
- Gateway failure scenarios?

## Service discovery

- What is service discovery?
- Client-side vs server-side discovery?
- Health checks?
- Registration/deregistration?

## Load balancing

- What is load balancing?
- Round robin?
- Weighted round robin?
- Least connections?
- Consistent hashing?
- Client-side vs server-side load balancing?
- What happens when an instance becomes unhealthy?

## Resilience

- Timeout?
- Retry?
- Exponential backoff?
- Jitter?
- Circuit breaker?
- Bulkhead?
- Rate limiting?
- Fallback?
- Why can retries make an outage worse?
- Retry budget?
- How do you handle partial failure?

## Distributed transactions

- Why are distributed transactions difficult?
- What is Saga?
- Saga orchestration vs choreography?
- 2PC vs Saga?
- When would you choose Saga?
- Compensation transactions?
- What happens when compensation also fails?
- Eventual consistency?
- How do you communicate transaction state?

## Idempotency

- What is idempotency?
- Why is it critical for transaction systems?
- How do you implement idempotency?
- Idempotency key?
- Database unique constraint?
- Idempotency table?
- Redis-based idempotency?
- What happens if the same request arrives concurrently?
- What happens if the first request succeeds but the response is lost?
- How do you safely retry a transaction?
- At-most-once vs at-least-once?
- Exactly-once vs effectively-once processing?

## Distributed locking

- What is distributed locking?
- When is it needed?
- Redis-based distributed locks?
- Database-based locking?
- What problems can distributed locks introduce?
- Lock expiry?
- Fencing tokens?
- Why should distributed locks not be used as a default solution?

## Distributed systems concepts

- CAP theorem?
- Consistency vs availability?
- Network partition?
- Eventual consistency?
- Strong consistency?
- Read-after-write consistency?
- Quorum?
- Replication?
- Leader/follower?
- Active-active vs active-passive?
- Replica lag?
- Failover?
- Disaster recovery?

## Observability

- Centralized logging?
- Structured logging?
- Correlation ID?
- Trace ID?
- Span ID?
- Distributed tracing?
- OpenTelemetry?
- Metrics?
- Golden signals?
- How do you trace one request across multiple services?

## Caching

- Why cache?
- Cache-aside?
- Read-through?
- Write-through?
- Write-back?
- Cache invalidation?
- TTL?
- LRU?
- Cache stampede?
- Cache penetration?
- Cache breakdown?
- Local cache vs distributed cache?
- Redis use cases?
- Consistency trade-offs?

---

# 8. Kafka / Messaging

- Why use Kafka?
- Kafka vs traditional message queue?
- Topic?
- Partition?
- Offset?
- Producer?
- Consumer?
- Consumer group?
- How does Kafka provide scalability?
- How does partitioning work?
- How is ordering maintained?
- What happens when multiple consumers are in one consumer group?
- Consumer rebalancing?
- ISR?
- Leader/follower replicas?
- Replication factor?
- Producer acknowledgements?
- `acks=0`, `acks=1`, `acks=all`?
- Consumer offset management?
- At-most-once delivery?
- At-least-once delivery?
- Exactly-once semantics?
- Duplicate messages?
- Idempotent consumer?
- Dead Letter Queue?
- Retry topics?
- Poison messages?
- Kafka retention?
- Compaction?
- How do you guarantee ordering for a business key?
- What happens when a consumer crashes?
- What happens when a partition leader fails?
- Kafka vs REST for service communication?
- Event-driven vs synchronous architecture?

## Messaging patterns

- Outbox pattern?
- Transactional outbox?
- Inbox pattern?
- Idempotent consumer?
- Retry with backoff?
- Dead-letter processing?
- Event versioning?
- Schema evolution?
- Eventual consistency?

---

# 9. SQL & Databases

## SQL fundamentals

- INNER JOIN vs LEFT JOIN?
- LEFT JOIN vs RIGHT JOIN?
- CROSS JOIN?
- WHERE vs HAVING?
- GROUP BY?
- DISTINCT?
- Subquery vs JOIN?
- Correlated subquery?
- CTE?
- UNION vs UNION ALL?
- EXISTS vs IN?
- CASE expression?

## Window functions

- What is a window function?
- ROW_NUMBER vs RANK vs DENSE_RANK?
- Partitioning in window functions?
- Latest record per user?
- Top N records per group?
- Running total?
- Moving average?

## Common SQL problems

- Find second-highest salary.
- Find Nth-highest salary.
- Find duplicate records.
- Remove duplicates.
- Find latest record per user.
- Find top N records per group.
- Find employees with salary greater than department average.
- Find users with multiple transactions.
- Find consecutive records.
- Find missing records.
- Find users who performed an action more than once.

## Indexes

- What is an index?
- Why do indexes improve reads?
- Why do indexes slow writes?
- B-tree index?
- Hash index?
- Composite index?
- Column order in composite indexes?
- Leftmost-prefix rule?
- Covering index?
- Clustered vs non-clustered index?
- Cardinality?
- Selectivity?
- When won't an index be used?
- Too many indexes?
- Function on indexed column?
- Leading wildcard?
- Index on low-cardinality column?

## Query performance

- How do you investigate a slow query?
- EXPLAIN?
- EXPLAIN ANALYZE?
- Query execution plan?
- Full table scan?
- Index scan?
- Index seek?
- Filesort?
- Temporary table?
- Join order?
- Cardinality estimates?
- Statistics?
- How do you identify the bottleneck?
- How do you optimize a slow query?
- How do you validate that an optimization actually helped?

## Transactions / ACID

- What is ACID?
- Atomicity?
- Consistency?
- Isolation?
- Durability?
- Transaction boundaries?
- Commit vs rollback?

## Isolation

- READ_UNCOMMITTED?
- READ_COMMITTED?
- REPEATABLE_READ?
- SERIALIZABLE?
- Dirty read?
- Non-repeatable read?
- Phantom read?
- Snapshot isolation?
- MVCC?

## Locking

- Database locking?
- Row-level vs table-level locking?
- Shared vs exclusive lock?
- Optimistic locking?
- Pessimistic locking?
- Version column?
- Lost update?
- Database deadlock?
- Why do database deadlocks occur?
- How do you prevent database deadlocks?
- How do you diagnose a database deadlock?

## Database design

- Normalization?
- Denormalization?
- When should you denormalize?
- Primary key?
- Foreign key?
- Unique constraint?
- Database constraints?
- Referential integrity?
- Partitioning?
- Sharding?
- Replication?
- Read replicas?
- Connection pooling?

## Connection pools

- Why use HikariCP/UCP?
- What is a database connection pool?
- Maximum pool size?
- Minimum idle?
- Connection timeout?
- Idle timeout?
- Max lifetime?
- What happens when the pool is exhausted?
- How can connection leaks happen?
- How do connection pools affect API performance?

---

# 10. JVM & Performance

## JVM fundamentals

- JVM vs JDK vs JRE?
- JVM architecture?
- Class loading?
- ClassLoader?
- Bootstrap ClassLoader?
- Platform ClassLoader?
- Application ClassLoader?
- Parent delegation?
- What happens when a class is loaded?
- Linking?
- Verification?
- Preparation?
- Resolution?
- Initialization?

## Memory

- Heap vs stack?
- What is stored in heap?
- What is stored in stack?
- Stack frames?
- Metaspace?
- Program counter?
- Native method stack?
- Young generation?
- Old generation?
- Eden?
- Survivor spaces?

## Garbage Collection

- What is garbage collection?
- Minor GC?
- Major GC?
- Full GC?
- Stop-the-world?
- G1GC?
- ZGC?
- G1GC vs ZGC?
- How does G1 divide the heap?
- What causes frequent GC?
- What causes long GC pauses?
- How do you investigate GC problems?
- GC logs?
- Heap dump?

## Memory problems

- OutOfMemoryError?
- StackOverflowError?
- Java heap space?
- Metaspace OOM?
- Direct buffer memory?
- Memory leak in Java?
- How can Java applications leak memory?
- Static collections?
- ThreadLocal leaks?
- Listener/reference leaks?
- ClassLoader leaks?

## Reference types

- Strong reference?
- Weak reference?
- Soft reference?
- Phantom reference?
- When would you use WeakReference?

## JIT / runtime

- What is JIT compilation?
- Interpreted vs compiled execution?
- Tiered compilation?
- JVM warm-up?
- Why might performance improve after an application has been running?
- What is deoptimization?

## Performance troubleshooting

- How do you investigate high CPU?
- How do you identify the CPU-consuming thread?
- How do thread dumps help?
- How do heap dumps help?
- How do you investigate high memory?
- How do you investigate frequent GC?
- How do you investigate slow API response?
- How do database connection pools affect performance?
- How do thread pools affect performance?
- How do you identify CPU vs I/O bottlenecks?
- How do you distinguish application latency from downstream latency?

## Advanced JVM

- Reflection?
- Dynamic proxies?
- Serialization?
- `serialVersionUID`?
- Type erasure?
- Generic type erasure implications?
- Java module system?
- JNI?
- Off-heap/direct memory?
- Annotation processing?
- `IdentityHashMap`?
- ClassLoader memory leaks?

---

# 11. AWS / Docker / Kubernetes / CI-CD

## AWS

- EC2 basics?
- EKS basics?
- ECR?
- S3?
- IAM?
- IAM role vs user?
- Load Balancer?
- ALB vs NLB?
- Auto Scaling?
- CloudWatch?
- VPC basics?
- Security groups?
- Secrets management?
- Availability Zones?
- High availability?

## Docker

- What is Docker?
- Image vs container?
- Dockerfile?
- Docker layers?
- Docker build?
- Docker run?
- Port mapping?
- Container networking?
- Environment variables?
- Volumes?
- Multi-stage builds?
- How do you make Docker images smaller?
- How do you secure containers?

## Kubernetes

- Pod?
- Deployment?
- ReplicaSet?
- Service?
- ConfigMap?
- Secret?
- Ingress?
- Namespace?
- Node?
- Cluster?
- Liveness probe?
- Readiness probe?
- Startup probe?
- Liveness vs readiness?
- Horizontal Pod Autoscaler?
- Resource requests?
- Resource limits?
- Rolling deployment?
- Blue-green deployment?
- Canary deployment?
- Rollback?
- How does Kubernetes restart failed containers?
- How does Kubernetes route traffic?

## CI/CD

- What is CI/CD?
- GitLab CI/Jenkins pipeline?
- Build stage?
- Unit tests?
- Integration tests?
- Static analysis?
- Security scanning?
- Docker build?
- Image push?
- Deployment?
- Smoke testing?
- Rollback?
- How do you deploy a Spring Boot service safely?
- How do you avoid downtime during deployment?

---

# 12. System Design

## General approach

- How do you approach a system-design problem?
- How do you clarify requirements?
- Functional vs non-functional requirements?
- How do you estimate traffic?
- How do you estimate storage?
- How do you identify bottlenecks?
- How do you choose between synchronous and asynchronous processing?
- How do you choose SQL vs NoSQL?
- How do you design for failure?
- How do you design for high availability?
- How do you design for scalability?
- How do you design observability?

## Common designs

- Design a URL shortener.
- Design a notification service.
- Design a payment service.
- Design a transaction service.
- Design a wallet service.
- Design an order service.
- Design a ticket booking system.
- Design a rate limiter.
- Design a distributed cache.
- Design file storage.
- Design a chat system.
- Design a food delivery system.
- Design a ride-booking system.
- Design a social media feed.
- Design an e-commerce order system.

## Scalability

- How would you handle 10,000+ requests/sec?
- How would you handle 100,000+ requests/sec?
- How do you horizontally scale a service?
- Where would you cache?
- How do you handle cache invalidation?
- How do you partition data?
- How do you shard a database?
- How do you handle hot partitions?
- How do you scale reads?
- How do you scale writes?
- How do you use asynchronous processing?

## Reliability

- How do you design for failure?
- How do you handle dependency failure?
- How do you prevent cascading failures?
- Circuit breaker?
- Bulkhead?
- Timeout?
- Retry?
- Rate limiting?
- Load shedding?
- Graceful degradation?
- Disaster recovery?
- Backup and restore?
- RPO vs RTO?

## Consistency

- Strong vs eventual consistency?
- How do you prevent duplicate transactions?
- How do you guarantee safe retries?
- How do you maintain consistency across services?
- Saga?
- Outbox pattern?
- Idempotency?
- Distributed locking?
- Optimistic locking?

## Architecture patterns

- CQRS?
- Event sourcing?
- Saga?
- Outbox?
- Pub/sub?
- Event-driven architecture?
- API Gateway?
- Service discovery?
- Consistent hashing?
- Sharding?
- Replication?

---

# 13. Observability / Debugging / Production

## Logging

- Why structured logging?
- What should be logged?
- What should never be logged?
- Correlation ID?
- Trace ID?
- Span ID?
- MDC?
- How do you trace one request across services?

## Metrics

- What metrics do you monitor?
- Golden signals?
- Latency?
- Traffic?
- Errors?
- Saturation?
- CPU?
- Memory?
- GC?
- Thread pool?
- Connection pool?
- Database latency?
- Downstream latency?

## Distributed tracing

- What is distributed tracing?
- Trace vs span?
- How does trace propagation work?
- OpenTelemetry?
- How do you identify which service caused latency?

## Production debugging

- API latency suddenly doubles. How do you investigate?
- CPU suddenly spikes. What do you check?
- Memory suddenly spikes. What do you check?
- Frequent GC. What do you check?
- Thread pool exhausted. What could cause it?
- Connection pool exhausted. What could cause it?
- Database CPU suddenly spikes. What do you check?
- Database latency increases. What do you check?
- Downstream service becomes slow. What should your service do?
- One service is returning intermittent 5xx errors. How do you investigate?
- How do heap dumps help?
- How do thread dumps help?
- When would you use `jstack`?
- How would you identify a deadlock?
- How would you identify a memory leak?
- How would you identify a connection leak?
- How would you identify thread starvation?

## Incident management

- Tell me about a production issue you solved.
- How did you detect it?
- How did you isolate the root cause?
- What temporary mitigation did you use?
- What permanent fix did you implement?
- How did you prevent recurrence?
- How would you write a postmortem?

---

# 14. DSA / Coding Patterns

For every DSA problem:

> **Understand pattern → Brute force → Why brute force is slow → Optimization → Java code → Dry run → Time complexity → Space complexity → Edge cases → Variations → Follow-ups**

## Pattern 1 — Sliding Window

- What is sliding window?
- Fixed-size window.
- Maximum sum of K consecutive elements.
- Minimum sum of K consecutive elements.
- Maximum/minimum in a fixed window.
- Variable-size window.
- Longest substring without repeating characters.
- Minimum-size subarray sum.
- Maximum vowels in substring of size K.
- Sliding Window Maximum.
- When can sliding window be used?
- When does sliding window NOT work?
- How do you decide whether the window should expand or shrink?

## Pattern 2 — Two Pointers

- What are two pointers?
- Two Sum on sorted array.
- Three Sum.
- Remove duplicates from sorted array.
- Palindrome.
- Container With Most Water.
- Merge sorted arrays.
- How do you decide which pointer moves?


## Pattern 3 — Fast & Slow Pointers

- Find middle of linked list.
- Detect cycle.
- Find cycle start.
- Happy Number.
- Why does Floyd's algorithm work?



## Pattern 4 — HashMap / HashSet

- Two Sum.
- Frequency counting.
- First non-repeating character.
- Duplicate detection.
- Longest consecutive sequence.
- Group anagrams.
- Subarray sum problems.
- Prefix sum + HashMap.

## Pattern 5 — Stack / Monotonic Stack

- Valid Parentheses.
- Min Stack.
- Next Greater Element.
- Next Smaller Element.
- Daily Temperatures.
- Largest Rectangle in Histogram.
- What is a monotonic stack?
- When should you use one?

## Pattern 6 — Binary Search

- Normal binary search.
- First occurrence.
- Last occurrence.
- Lower bound.
- Upper bound.
- Search in rotated sorted array.
- Find minimum in rotated sorted array.
- Binary search on answer.
- How do you recognize binary search on answer?

## Pattern 7 — Linked List

- Reverse linked list.
- Reverse linked list in K groups.
- Merge two sorted lists.
- Detect cycle.
- Find cycle start.
- Remove Nth node.
- Find intersection.
- Palindrome linked list.
- Reorder linked list.

## Pattern 8 — Trees

- DFS.
- BFS.
- Preorder.
- Inorder.
- Postorder.
- Level-order traversal.
- Height/depth.
- Diameter.
- Lowest Common Ancestor.
- Validate BST.
- Serialize/deserialize tree.
- Balanced binary tree.

## Pattern 9 — Heap / PriorityQueue

- Kth largest.
- Kth smallest.
- Top K frequent elements.
- Merge K sorted lists.
- K closest elements.
- Median from data stream.
- When should you use a heap instead of sorting?

## Pattern 10 — Recursion / Backtracking

- Generate all subsequences.
- Subsets.
- Permutations.
- Combination Sum.
- N-Queens.
- Generate parentheses.
- Recursive directory traversal/deletion.
- How do you identify a backtracking problem?
- Choice → recursion → undo.

## Pattern 11 — Graph

- BFS.
- DFS.
- Number of Islands.
- Connected components.
- Cycle detection.
- Topological sort.
- Course Schedule.
- Shortest path.
- Dijkstra basics.
- Directed vs undirected graph.
- Adjacency list vs adjacency matrix.

## Pattern 12 — Dynamic Programming

- What is dynamic programming?
- Overlapping subproblems?
- Optimal substructure?
- Memoization vs tabulation?
- 1D DP.
- 2D DP.
- Longest Common Subsequence.
- Climbing Stairs.
- House Robber.
- Coin Change.
- Knapsack basics.
- How do you recognize DP?

## Pattern 13 — Intervals

- Merge Intervals.
- Insert Interval.
- Meeting Rooms.
- Overlapping intervals.
- Sort intervals by start/end.

## Pattern 14 — LRU Cache

- Design an LRU cache.
- Why HashMap + doubly linked list?
- Get/put complexity?
- How would you make LRU cache thread-safe?
- What changes for distributed cache?

## Complexity

For every coding solution:

- Time complexity?
- Space complexity?
- Can time be improved?
- Can space be improved?
- What is the brute-force complexity?
- Why is the optimized solution better?
- What happens with empty input?
- Null input?
- Duplicate values?
- Negative values?
- Integer overflow?
- Very large input?

---

# 15. Common Coding Questions

These should be practiced even when they overlap with the patterns above.

- Two Sum
- Three Sum
- Sort 0/1/2
- Longest Substring Without Repeating Characters
- Maximum Vowels in Substring K
- Minimum in Rotated Sorted Array
- Generate All Subsequences
- Subsets
- Permutations
- Valid Parentheses
- Reverse Linked List
- Reverse Linked List in K Groups
- Merge Two Sorted Lists
- Sliding Window Maximum
- Next Greater Element
- Top K Frequent Elements
- Merge Intervals
- Number of Islands
- Course Schedule
- Lowest Common Ancestor
- Binary Tree Traversals
- Longest Common Subsequence
- Recursive Directory Traversal
- Circular Array Next Greater Element

---

# 16. Java Multithreading Coding Questions

- Print odd/even numbers using two threads.
- Print 1–100 using multiple threads.
- Implement producer-consumer.
- Implement a thread-safe counter.
- Implement synchronization around a shared resource.
- Implement a deadlock.
- Detect/prevent deadlock.
- Implement a custom thread pool conceptually.
- Use wait/notify.
- Use CountDownLatch.
- Use CyclicBarrier.
- Use Semaphore.
- Use CompletableFuture for parallel API calls.
- Implement scatter-gather.
- Handle partial failure in parallel tasks.
- Implement a thread-safe cache.
- Implement rate limiting with concurrency control.

---

# 17. Senior Scenario Questions

## Concurrency / transactions

- Two requests update the same wallet balance simultaneously. What can go wrong?
- How would you solve it?
- A client times out, retries, and the first request actually succeeded. How do you prevent double processing?
- Database commit succeeds but API response is lost. What should the client do?
- Same transaction arrives twice concurrently. How do you handle it?
- How would you make an operation idempotent?
- How would you prevent a race condition?
- How would you safely retry a transaction?

## Distributed failures

- Downstream service takes 30 seconds. How should your service behave?
- One of five parallel downstream calls fails. What do you return?
- One dependency is unavailable. Should the entire API fail?
- How would you implement graceful degradation?
- How would you prevent cascading failure?
- How would you use timeout + retry + circuit breaker?
- What happens if retry itself causes more load?

## Database

- Database CPU suddenly spikes. What do you check?
- Database connection pool is exhausted. What could cause it?
- A query suddenly becomes slow. How do you investigate?
- An index exists but the database doesn't use it. Why?
- Two transactions deadlock. How do you investigate?
- How would you prevent lost updates?

## Performance

- API latency suddenly doubles. Walk through your investigation.
- CPU suddenly reaches 100%. What do you check?
- Memory suddenly increases. What do you check?
- GC frequency suddenly increases. What do you check?
- Thread pool is exhausted. What could cause it?
- Connection pool is exhausted. What could cause it?
- Downstream latency increases. How does it affect your service?
- Traffic increases 10x. What becomes the bottleneck?

## Cache

- Cache returns stale data. What consistency trade-off did you make?
- Cache goes down. Should the application fail?
- Cache stampede occurs. How do you handle it?
- Cache contains incorrect data. How do you recover?
- How would you invalidate cached configuration?

## Messaging

- Duplicate message arrives. What do you do?
- Message processing succeeds but acknowledgement fails. What happens?
- Consumer crashes after database commit but before acknowledgement.
- How do you prevent duplicate processing?
- How would you design a DLQ?
- How do you handle poison messages?

## Microservices

- Two services update related data and one fails. How do you recover?
- How do you maintain consistency across services?
- When would you use Saga?
- When would you use synchronous communication?
- When would you use asynchronous communication?

---

# 18. Behavioral / Managerial Questions

- Tell me about yourself.
- Walk me through your current project.
- Walk me through your previous project.
- Why are you looking for a change?
- Why this company?
- Why this role?
- What was your most difficult technical problem?
- Tell me about a production incident you handled.
- Tell me about a disagreement with a teammate.
- How do you prioritize multiple production issues?
- How do you review code?
- How do you ensure quality before release?
- Tell me about a performance improvement you made.
- Tell me about a design decision you disagreed with.
- Tell me about a mistake and what you learned.
- Tell me about a time you had to learn something quickly.
- Tell me about a time you worked under pressure.
- How do you mentor junior developers?
- How do you handle code review disagreements?
- How do you handle unclear requirements?
- How do you communicate technical risks?
- How do you estimate work?
- How do you handle changing requirements?
- What is your biggest technical strength?
- What area are you currently improving?
- Why should we hire you for this backend role?

---

# 19. Git / Engineering Practices

- Git merge vs rebase?
- When would you use rebase?
- What is cherry-pick?
- What is revert?
- Reset vs revert?
- How do you resolve merge conflicts?
- What is a good branching strategy?
- Pull request best practices?
- What should you check during code review?
- How do you review someone else's backend code?
- How do you handle a bad commit already pushed?
- How do you safely release a database change?
- Expand/contract database migration?
- Backward-compatible database changes?
- Feature flags?
- Rollback strategy?

---

# 20. Testing

- Unit testing vs integration testing?
- Testing pyramid?
- JUnit?
- Mockito?
- Mock vs Spy?
- When should you avoid mocking?
- REST Assured?
- WireMock?
- Contract testing?
- End-to-end testing?
- How do you test external service failures?
- How do you test timeout scenarios?
- How do you test retry behavior?
- How do you test idempotency?
- How do you test concurrency?
- How do you test transaction rollback?
- How do you test database queries?
- What should not be tested through mocks?
- How do you prevent flaky tests?

---

# 21. Advanced Distributed-System Topics

These are lower priority than the core interview topics but should be understood conceptually.

- Consistent hashing.
- Distributed cache.
- Distributed locking.
- Redlock limitations.
- Database replication.
- Read replicas.
- Replica lag.
- Sharding.
- Partitioning.
- Hot partitions.
- CQRS.
- Event sourcing.
- Eventual consistency.
- Snapshot isolation.
- Quorum.
- Leader election.
- Active-active architecture.
- Active-passive architecture.
- Data lake vs data warehouse.
- Chaos engineering.
- Fault injection.
- Backpressure.
- Load shedding.
- Graceful degradation.
- Bulkheads.
- Dead Letter Queues.
- Outbox pattern.
- Inbox pattern.

---

# 22. ARRISE / Product-Company Style Interview Coding

These are not a separate syllabus. They are practice problems inside the main DSA/concurrency roadmap.

- Three Sum
- Sort 0/1/2
- Longest Common Subsequence
- Longest Substring Without Repeating Characters
- Maximum Vowels in K-sized substring
- Minimum in Rotated Sorted Array
- Generate All Subsequences
- Reverse Linked List in K Groups
- Sliding Window Maximum
- Odd/even using two threads
- Print numbers using multiple threads
- Deadlock implementation
- Synchronization around a shared resource
- Circular array next greater element
- Recursive directory deletion
- Subsets
- Binary tree traversal
- Lowest Common Ancestor
- Number of Islands
- Course Schedule
- Top K Frequent Elements
- Merge Intervals
- Valid Parentheses
- Sort/merge/search variations
- Concurrent counter
- Producer-consumer
- CompletableFuture scatter-gather

---

# 23. Interviewer Follow-Up Method

The interviewer will often start with a simple question and progressively increase difficulty.

We should prepare every major topic this way.

## Example — HashMap

### Level 1 — Basic

- What is HashMap?
- Why use HashMap?

### Level 2 — Internal Working

- How does HashMap work internally?
- What happens during put?
- What happens during get?
- How is a bucket selected?

### Level 3 — Collision

- What is a hash collision?
- What happens when two keys have the same hash?
- How are collisions handled?
- What changed in Java 8?

### Level 4 — Edge Cases

- What happens with mutable keys?
- What happens if equals/hashCode are incorrect?
- What happens when the map resizes?
- What happens with null?

### Level 5 — Comparison

- HashMap vs ConcurrentHashMap?
- HashMap vs TreeMap?
- HashMap vs Hashtable?

### Level 6 — Concurrency

- Why isn't HashMap thread-safe?
- What happens if multiple threads update it?
- Why is ConcurrentHashMap different?

### Level 7 — Performance

- Expected complexity?
- Worst-case complexity?
- Load factor?
- Resizing?
- Treeification?

### Level 8 — Real Backend Scenario

- Suppose this HashMap is an in-memory cache in your service. How would you make it thread-safe?
- What happens if the cache becomes very large?
- How would you handle stale data?
- Would you use local cache or Redis?

---

# 24. CompletableFuture Interview Follow-Up Chain

### Level 1

- What is CompletableFuture?
- Future vs CompletableFuture?

### Level 2

- `supplyAsync()`?
- `runAsync()`?
- `thenApply()`?
- `thenCompose()`?
- `thenCombine()`?

### Level 3

- How are exceptions handled?
- `exceptionally()`?
- `handle()`?
- `whenComplete()`?

### Level 4

- How do you control the executor?
- What happens if you don't provide one?
- What happens if the thread pool is exhausted?

### Level 5

- How do you implement timeout?
- How do you handle partial failure?
- What if one of five calls fails?

### Level 6

- How would you implement scatter-gather?
- How would you aggregate results?
- Would you fail the entire request if one dependency fails?

### Level 7 — Real Backend Scenario

- You need data from five independent downstream services.
- Calling sequentially gives 1s + 1s + 1s + 1s + 1s.
- How would you reduce overall latency?
- What executor would you use?
- What happens if one service takes 10 seconds?
- What timeout would you apply?
- What happens if the downstream service is unavailable?
- How do you prevent retry storms?
- How would you monitor this flow?

---

# 25. Concurrency Interview Follow-Up Chain

## synchronized

- What does synchronized guarantee?
- Mutual exclusion?
- Visibility?
- Object lock vs class lock?
- Instance method vs static method?
- Reentrant?
- Performance implications?

## volatile

- What does volatile guarantee?
- What does it not guarantee?
- Why isn't `volatile int count; count++` safe?
- What is happens-before?
- When would you use volatile?

## AtomicInteger

- How does AtomicInteger work?
- What is CAS?
- Why is CAS useful?
- AtomicInteger vs synchronized?
- What is the ABA problem?

## ReentrantLock

- Why use ReentrantLock?
- What does reentrant mean?
- `tryLock()`?
- Fairness?
- How do you guarantee unlock?

## Deadlock

- What is deadlock?
- What are the four conditions?
- How can deadlock happen?
- How can you prevent it?
- How can you detect it?
- How would you diagnose it in production?

---

# 26. DSA Pattern Roadmap

We will learn one pattern at a time.

## Pattern 1 — Sliding Window

- What is a window?
- Fixed-size window.
- Maximum sum of K consecutive elements.
- Minimum sum of K consecutive elements.
- How do we move the window?
- Why remove the outgoing element?
- Why add the incoming element?
- Brute force vs optimized solution.
- Variable-size sliding window.
- Longest substring without repeating characters.
- Minimum-size subarray sum.
- Maximum vowels in K-sized substring.
- Sliding Window Maximum.
- Tricky variations.
- Complexity.
- Common mistakes.

## Pattern 2 — Two Pointers

- What are two pointers?
- Why use them?
- Two Sum on sorted array.
- Three Sum.
- Remove duplicates.
- Palindrome.
- Container With Most Water.
- How do you decide which pointer moves?
- Tricky variations.
- Complexity.

## Pattern 3 — Fast & Slow Pointers

- Find middle.
- Detect cycle.
- Find cycle start.
- Happy Number.
- Why does Floyd's algorithm work?
- Complexity.

## Pattern 4 — HashMap / HashSet

- Frequency counting.
- Two Sum.
- First non-repeating character.
- Longest consecutive sequence.
- Duplicate detection.
- Group anagrams.
- Prefix sum + HashMap.

## Pattern 5 — Stack / Monotonic Stack

- Valid Parentheses.
- Next Greater Element.
- Min Stack.
- Daily Temperatures.
- Largest Rectangle.
- Monotonic Stack concept.

## Pattern 6 — Binary Search

- Normal binary search.
- First/last occurrence.
- Lower/upper bound.
- Rotated sorted array.
- Minimum in rotated sorted array.
- Binary search on answer.

## Pattern 7 — Linked List

- Reverse.
- Merge.
- Cycle.
- Remove Nth node.
- Intersection.
- Palindrome.
- Reverse in K groups.

## Pattern 8 — Trees

- DFS.
- BFS.
- Height.
- Level order.
- Diameter.
- LCA.
- Validate BST.
- Serialize/deserialize.

## Pattern 9 — Heap

- Kth largest/smallest.
- Top K.
- Merge K sorted lists.
- Median from stream.

## Pattern 10 — Recursion / Backtracking

- Subsets.
- Subsequences.
- Permutations.
- Combination Sum.
- N-Queens.
- Generate Parentheses.

## Pattern 11 — Graph

- BFS.
- DFS.
- Connected components.
- Cycle detection.
- Topological sort.
- Course Schedule.
- Number of Islands.
- Shortest path.
- Dijkstra basics.

## Pattern 12 — Dynamic Programming

- Recognizing DP.
- Overlapping subproblems.
- Optimal substructure.
- Memoization.
- Tabulation.
- 1D DP.
- 2D DP.
- LCS.
- Knapsack.
- Coin Change.
- House Robber.

---

# 27. Senior Interview Depth Rule

For a 5 YOE / 25–30+ LPA backend interview, we should not stop at definitions.

Every important topic should eventually reach:

> **WHAT**
>
> ↓
>
> **HOW**
>
> ↓
>
> **WHY**
>
> ↓
>
> **EDGE CASE**
>
> ↓
>
> **COMPARISON**
>
> ↓
>
> **CODING**
>
> ↓
>
> **CONCURRENCY / PERFORMANCE**
>
> ↓
>
> **REAL-WORLD SCENARIO**
>
> ↓
>
> **PROJECT CONNECTION**
>
> ↓
>
> **TRICKY FOLLOW-UP**

Examples:

### HashMap

> HashMap → internals → buckets → collisions → treeification → mutable keys → equals/hashCode → resizing → complexity → thread safety → ConcurrentHashMap → backend cache scenario

### CompletableFuture

> CompletableFuture → async execution → executor → thenApply → thenCompose → thenCombine → exception handling → timeout → partial failure → thread-pool exhaustion → scatter-gather → backend scenario

### Transactions

> Transaction → ACID → isolation → locks → propagation → rollback → deadlocks → external calls → retries → idempotency → distributed transaction → Saga → real transaction workflow

### SQL Index

> Index → B-tree → lookup → composite index → column order → selectivity → covering index → EXPLAIN → filesort → slow query → production optimization

### REST API

> REST → HTTP methods → idempotency → status codes → validation → authentication → authorization → pagination → versioning → backward compatibility → retries → distributed transaction scenario

---

# 28. Preparation Priority

Not every question has equal interview probability.

## Tier 1 — Must Master Deeply

These should receive the highest preparation time.

- Core Java
- OOP
- String
- equals/hashCode
- Collections
- HashMap internals
- Java 8
- Streams
- Optional
- Concurrency
- synchronized
- volatile
- Atomic classes
- ExecutorService
- CompletableFuture
- Thread pools
- Race conditions
- Deadlocks
- Spring Boot
- Dependency Injection
- REST
- `@Transactional`
- Transaction propagation
- Transaction isolation
- SQL
- Indexes
- Query optimization
- Microservices
- Idempotency
- Retry/timeout
- Distributed failures
- DSA
- System Design
- Project deep dive

## Tier 2 — Strong Understanding

- JVM internals
- GC
- G1GC/ZGC
- Kafka
- Redis/caching
- Distributed locking
- Saga
- API Gateway
- Kubernetes
- AWS
- Observability
- Production debugging
- Database replication
- Sharding
- CQRS
- Outbox pattern

## Tier 3 — Conceptual Awareness

- JNI
- Java Modules
- Annotation processing
- Advanced reference types
- IdentityHashMap
- Spring Batch
- WebFlux internals
- Advanced Hibernate internals
- Data lake vs warehouse
- Chaos engineering
- Advanced distributed-system algorithms

Tier 3 topics should not take preparation time away from Tier 1.

---

# 29. Final Interview Preparation Checklist

Before considering a topic complete, I should be able to:

- Explain it in simple words.
- Explain why it exists.
- Explain how it works internally.
- Write a basic example.
- Write Java code when required.
- Dry-run the code.
- Explain time and space complexity.
- Explain common edge cases.
- Compare it with alternatives.
- Explain performance implications.
- Explain concurrency implications.
- Explain production failure scenarios.
- Connect it to backend development.
- Connect it to my actual project.
- Answer follow-up questions without memorized wording.
- Explain trade-offs.
- Debug a realistic scenario involving the concept.

---

# 30. Final Goal

The objective is **not** to memorize a list of questions.

The objective is to develop the ability to take an interviewer question from:

> **Basic definition**

to:

> **Internal implementation**

to:

> **Why the design exists**

to:

> **Trade-offs**

to:

> **Edge cases**

to:

> **Performance**

to:

> **Concurrency**

to:

> **Distributed systems**

to:

> **Real backend scenario**

to:

> **Actual project experience**

For a 5 YOE Java Backend Engineer, the preparation should demonstrate that I can:

- Write production-quality Java.
- Understand JVM and memory behavior.
- Work confidently with collections.
- Build thread-safe concurrent systems.
- Design Spring Boot services.
- Design reliable REST/gRPC APIs.
- Handle transactions and failures.
- Build idempotent transaction workflows.
- Work with SQL and optimize database access.
- Design scalable microservices.
- Debug production problems.
- Understand cloud/container deployment.
- Solve common DSA problems.
- Discuss system design and trade-offs.
- Explain my own projects deeply.
- Handle interviewer follow-up questions rather than only answering the initial question.

**This master question bank is the common preparation roadmap. Company-specific questions are additional practice on top of this roadmap, not a replacement for it.**