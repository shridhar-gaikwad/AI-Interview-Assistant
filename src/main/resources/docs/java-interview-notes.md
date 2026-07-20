# Java Interview Notes

A structured knowledge base for interview preparation, grouped by topic area:
Core Java, JVM & Memory, Concurrency, Modern Java, Design Patterns, the Spring
ecosystem, Architecture & System Design, and Cloud.

---

## 1. Core Java

### 1.1 String, StringBuffer, and StringBuilder
String is immutable: every modification creates a new object, which is safe to
share but wasteful in loops. StringBuilder is a mutable, non-synchronized
sequence of characters, ideal for building strings in single-threaded code.
StringBuffer is the same as StringBuilder but synchronized (thread-safe), and
therefore slower. Rule of thumb: use String for constants, StringBuilder for
local concatenation, and StringBuffer only when multiple threads mutate the same
buffer. String literals live in the String pool, so identical literals are reused;
`new String("x")` forces a separate heap object. Because String is immutable it is
safe as a HashMap key and for caching hash codes.

### 1.2 Exception Handling
An exception is an event that disrupts normal program flow. Java's hierarchy is
rooted at Throwable, which splits into Error (serious JVM problems, not meant to be
caught, e.g. OutOfMemoryError) and Exception.
- Checked exceptions (subclasses of Exception, excluding RuntimeException) must be
  declared with `throws` or handled at compile time (e.g. IOException, SQLException).
- Unchecked exceptions (subclasses of RuntimeException) represent programming
  errors and are not checked by the compiler (e.g. NullPointerException,
  IllegalArgumentException).
Handling constructs: `try` guards risky code, `catch` handles specific types,
`finally` always runs (for cleanup), `throw` raises an exception, and `throws`
declares one. `try-with-resources` auto-closes resources implementing
AutoCloseable. Best practices: catch the most specific exception first, never
swallow exceptions silently, prefer custom domain exceptions for clarity, and
wrap lower-level exceptions to preserve the original cause.

### 1.3 Java Collection Framework
A unified architecture for storing and manipulating groups of objects. Core
interfaces:
- List (ordered, allows duplicates): ArrayList (fast random access), LinkedList
  (fast insert/remove at ends).
- Set (no duplicates): HashSet (unordered), LinkedHashSet (insertion order),
  TreeSet (sorted).
- Map (key-value pairs, not a true Collection): HashMap (unordered), LinkedHashMap
  (insertion order), TreeMap (sorted), ConcurrentHashMap (thread-safe).
- Queue/Deque: LinkedList, ArrayDeque, PriorityQueue.
HashMap vs Hashtable: HashMap is unsynchronized and allows one null key and
multiple null values; Hashtable is synchronized and allows no nulls. For
concurrency prefer ConcurrentHashMap. Iterators are fail-fast (throw
ConcurrentModificationException on structural change). Comparable defines natural
ordering (compareTo); Comparator defines custom ordering. Generics provide
compile-time type safety across the framework.

### 1.4 The `final` Keyword
A final variable cannot be reassigned, a final method cannot be overridden, and a
final class cannot be extended. It communicates intent and enables some compiler
and JVM optimizations.

---

## 2. JVM & Memory Management

### 2.1 JVM (Java Virtual Machine)
The JVM runs Java bytecode and makes Java platform-independent ("write once, run
anywhere"). Java source is compiled by javac into .class bytecode, which the JVM
loads, verifies, and executes. Key subsystems: the ClassLoader (loads classes),
the runtime data areas (Heap for objects, Stack per thread, Method Area/Metaspace
for class metadata, PC registers), the Execution Engine (interpreter + JIT
compiler that compiles hot paths to native code), and the Garbage Collector.
JVM, JRE, and JDK differ: JVM executes bytecode, JRE = JVM + core libraries, and
JDK = JRE + development tools (javac, debugger).

### 2.2 Garbage Collection (GC)
Java automatically reclaims memory occupied by objects that are no longer
reachable. The developer cannot force collection; System.gc() is only a hint to
the JVM. Generational GC divides the heap into the Young generation (Eden +
Survivor spaces, where most objects die quickly) and the Old/Tenured generation
(long-lived objects). Minor GC cleans the young gen; Major/Full GC cleans the old
gen. Modern collectors include G1 (default, region-based, low-pause), ZGC, and
Shenandoah (very low latency). Objects become eligible for GC when they are no
longer reachable from any GC root.

---

## 3. Concurrency

### 3.1 Multithreading and Concurrency
Multithreading runs multiple threads within a process to use CPU cores and stay
responsive. A thread is created by extending Thread or implementing Runnable/
Callable. The Executor framework (ExecutorService, thread pools) is preferred
over manually creating threads. Concurrency concerns:
- Synchronization with `synchronized` or Lock (ReentrantLock) to protect shared
  state and avoid race conditions.
- `volatile` guarantees visibility of a variable's latest value across threads.
- java.util.concurrent: ConcurrentHashMap, BlockingQueue, CountDownLatch,
  Semaphore, and atomic classes (AtomicInteger) for lock-free updates.
- CompletableFuture enables asynchronous, non-blocking pipelines.
- Thread lifecycle: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED.
- Common pitfalls: deadlock, livelock, and starvation. The `wait/notify`
  mechanism coordinates threads on an object's monitor.

---

## 4. Modern Java

### 4.1 Java 8 Features
Java 8 was a major release focused on functional-style programming:
- Lambda expressions: concise anonymous functions, e.g. `(a, b) -> a + b`.
- Functional interfaces: single-abstract-method interfaces (`Predicate`,
  `Function`, `Supplier`, `Consumer`), enabled by the `@FunctionalInterface` hint.
- Stream API: declarative processing of collections (filter/map/reduce/collect),
  supporting lazy evaluation and parallel streams.
- Optional: a container to represent presence/absence and avoid NullPointerException.
- Default and static methods in interfaces: add behaviour without breaking
  implementors.
- Method references: shorthand for lambdas, e.g. `String::toUpperCase`.
- New Date/Time API (java.time): immutable, thread-safe replacement for
  Date/Calendar.

---

## 5. Java Design Patterns
Reusable solutions to common design problems, grouped into three categories:
- Creational: Singleton (one instance), Factory Method / Abstract Factory
  (create objects without exposing construction), Builder (step-by-step
  construction of complex objects), Prototype (clone existing objects).
- Structural: Adapter (bridge incompatible interfaces), Decorator (add behaviour
  dynamically), Proxy (control access), Facade (simplified interface), Composite.
- Behavioural: Strategy (interchangeable algorithms), Observer (publish/
  subscribe), Template Method, Command, Iterator, Chain of Responsibility.
Spring itself relies heavily on Singleton (beans), Factory, Proxy (AOP), and
Template patterns.

---

## 6. Spring Ecosystem

### 6.1 Spring
Spring is a modular Java framework built around Inversion of Control (IoC) and
Dependency Injection (DI): the Spring container creates and wires beans instead
of code doing it manually, reducing coupling. Core features include the
ApplicationContext (the IoC container), Aspect-Oriented Programming (AOP) for
cross-cutting concerns like logging and transactions, and Spring MVC for web
applications. Beans are managed objects with a lifecycle and configurable scope
(singleton by default, prototype, request, session). Configuration can be XML,
Java `@Configuration`, or annotations (`@Component`, `@Service`, `@Repository`,
`@Autowired`).

### 6.2 Spring Boot
Spring Boot is an opinionated layer on top of Spring that makes it easy to build
stand-alone, production-ready applications. Key ideas:
- Auto-configuration: sensibly configures beans based on the classpath and
  properties, minimizing boilerplate.
- Starters: curated dependency bundles (e.g. spring-boot-starter-web) that pull
  in everything needed for a feature.
- Embedded servers: Tomcat/Jetty/Netty are bundled, so apps run as a single jar
  with `java -jar` — no external server needed.
- Externalized configuration via application.properties/yml and profiles.
- Actuator: production endpoints for health, metrics, and monitoring.
It follows "convention over configuration" to get developers productive quickly.

---

## 7. Architecture & System Design

### 7.1 Microservices Architecture
An architectural style that structures an application as a collection of small,
independently deployable services, each owning a single business capability and
its own database. Benefits: independent scaling and deployment, technology
diversity, and fault isolation. Trade-offs: distributed-system complexity,
network latency, and data consistency challenges. Common building blocks:
- API Gateway: single entry point handling routing, auth, and rate limiting.
- Service Discovery: services register/locate each other dynamically (Eureka).
- Inter-service communication: synchronous REST/gRPC or asynchronous messaging
  (Kafka/RabbitMQ).
- Resilience patterns: circuit breaker (Resilience4j), retries, timeouts,
  bulkheads.
- Centralized config, distributed tracing, and centralized logging for
  observability. Data consistency often uses the Saga pattern instead of
  distributed transactions.

### 7.2 System Design: Load Balancer, Caching, Rate Limiting
- Load Balancer: distributes incoming traffic across multiple servers to improve
  availability and scalability. Algorithms include round-robin, least-connections,
  and IP-hash. Operates at Layer 4 (transport) or Layer 7 (application).
- Caching: stores frequently accessed data in fast storage to reduce latency and
  database load. Strategies: cache-aside (lazy loading), read-through,
  write-through, and write-back. Requires an eviction policy (LRU, LFU, TTL) and
  careful cache-invalidation. Tools: Redis, Memcached, CDNs for static content.
- Rate Limiting: caps how many requests a client can make in a time window to
  prevent abuse and protect resources. Common algorithms: token bucket, leaky
  bucket, fixed window, and sliding window log/counter. Often enforced at the API
  gateway and backed by Redis for distributed counters.

### 7.3 Apache Kafka
Kafka is a distributed, fault-tolerant event-streaming platform for high-throughput
publish/subscribe messaging. Core concepts:
- Producer: publishes records to topics. Consumer: reads records from topics.
- Topic: a named stream of records, split into partitions for parallelism and
  scalability; order is guaranteed only within a partition.
- Consumer Group: consumers share a group so each partition is consumed by one
  member, enabling horizontal scaling.
- Broker: a Kafka server; a cluster of brokers stores partitions with replication
  for durability. Offsets track a consumer's position.
Kafka retains messages on disk for a configurable period (unlike a traditional
queue), enabling replay. It is widely used for decoupling microservices, event
sourcing, log aggregation, and real-time stream processing (Kafka Streams).

---

## 8. Cloud

### 8.1 AWS (Amazon Web Services)
AWS is a comprehensive cloud platform offering on-demand infrastructure and
managed services. Commonly discussed services:
- Compute: EC2 (virtual servers), Lambda (serverless functions), ECS/EKS
  (containers).
- Storage: S3 (object storage), EBS (block storage), EFS (file storage).
- Database: RDS (managed relational), DynamoDB (managed NoSQL), ElastiCache
  (Redis/Memcached).
- Networking: VPC (isolated networks), Route 53 (DNS), CloudFront (CDN), ELB
  (load balancing).
- Messaging/Integration: SQS (queues), SNS (pub/sub), API Gateway.
- Security/Ops: IAM (access control), CloudWatch (monitoring), CloudFormation
  (infrastructure as code). AWS operates on a shared-responsibility model and
  spans multiple Regions and Availability Zones for high availability and
  disaster recovery.
