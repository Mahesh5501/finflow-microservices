# Composable Business Platform

## Product, Architecture and Engineering Blueprint --- Draft 0.1

**Status:** Requirements and architecture discovery draft\
**Purpose:** Define the proposed product, responsibilities, request
flows, technology landscape, engineering standards, and delivery plan
before implementation begins.\
**Important:** This document is a planning baseline, not approval to
build every listed capability. Existing repository behaviour must be
audited before deciding what to retain, change, or replace.

------------------------------------------------------------------------

## 1. Executive Summary

The proposed system is a **Composable Business Platform**: a shared
technical foundation for building and integrating multiple business
applications without forcing them to share the same business rules.

Candidate business domains include:

-   **FinFlow:** wallet-like accounts, simulated transfers, transaction
    lifecycle, ledger-style records, reconciliation.
-   **Commerce:** products, inventory, orders, simulated payments,
    fulfilment and returns.
-   **Booking and subscriptions:** availability, reservations,
    subscription lifecycle and recurring business operations.
-   **Integration and workflows:** external API integrations, webhooks,
    event processing, retries, failure tracking and recovery.

Candidate shared capabilities include identity and access management,
business organisations, API security, audit history, notifications,
integration contracts, observability, testing and delivery automation.

### Primary objectives

1.  Build a useful, coherent product rather than a collection of
    unrelated demos.
2.  Develop strong Java backend and senior-engineer skills through real
    business and reliability problems.
3.  Keep the system maintainable and extensible, with clear ownership
    and documented trade-offs.
4.  Start with a manageable first release and expand based on
    demonstrated requirements.
5.  Preserve and assess the existing `finflow-microservices` project
    rather than assuming it must be rewritten.

### Guiding rule

**Requirements first, architecture second, implementation third.** The
system must not adopt a technology, service boundary or design pattern
solely to add it to a résumé.

------------------------------------------------------------------------

## 2. Product Vision and Target Users

### 2.1 Product vision

Build a platform that can host multiple business capabilities and
connect them to external systems. Common capabilities should be reusable
where their responsibilities genuinely overlap; domain-specific rules
and authoritative data should remain with the relevant business domain.

### 2.2 Candidate users

  -----------------------------------------------------------------------
  User or actor                       Candidate responsibilities
  ----------------------------------- -----------------------------------
  Business owner / organisation       Set up an organisation, invite
  administrator                       members, assign roles and configure
                                      enabled capabilities.

  Business employee / operator        Perform permitted work such as
                                      managing orders, reviewing
                                      transactions or resolving failed
                                      operations.

  End customer                        Use an application to place an
                                      order, check a booking or view a
                                      transaction.

  Platform operator                   Maintain deployment, health,
                                      configuration, security and
                                      operational visibility. This role
                                      must not automatically grant access
                                      to all business data.

  External system                     Call APIs, send webhooks, receive
                                      events or exchange data through a
                                      defined integration contract.
  -----------------------------------------------------------------------

The precise roles and permission model remain requirements to be
approved.

### 2.3 Target users and product positioning

The initial intended audience is small and medium-sized businesses,
while the project's primary purpose is a strong Java backend portfolio
and interview-preparation system. The first release should demonstrate
one coherent business workflow. It should not attempt to become a
complete commercial SaaS suite immediately.

------------------------------------------------------------------------

## 3. Product Scope

### 3.1 Candidate business domains

#### FinFlow

Potential capabilities: - Account or wallet lifecycle. - Simulated
transfers and transaction status. - Balance and ledger-style records. -
Idempotent request handling. - Transaction history and reconciliation. -
Defined handling for rejected, pending and failed operations.

Any financial behaviour is a learning simulation unless separately and
explicitly scoped otherwise. No real funds or live banking integrations
are assumed.

#### Commerce

Potential capabilities: - Product catalogue and prices. - Customer
records. - Stock availability and reservation. - Order creation, items,
totals and lifecycle. - Simulated payment outcome. - Fulfilment status,
cancellation and returns. - Handling for concurrent orders and payment
failures.

#### Booking and subscriptions

Potential later capabilities: - Availability and reservation. -
Prevention of conflicting bookings. - Reservation expiry and
cancellation. - Subscription lifecycle and recurring workflow records. -
External calendar or billing sandbox integrations.

#### Integration and workflows

Potential later capabilities: - Outbound API connectors. - Inbound
webhooks. - Event consumers and producers. - Retry policy and backoff. -
Idempotent processing. - Dead-letter handling and operator replay
controls. - Integration execution history and diagnostics.

A generic workflow designer is not part of the first release unless a
specific requirement justifies it.

### 3.2 Candidate shared platform capabilities

-   Identity and authentication.
-   Organisation membership and authorisation.
-   API entry, routing and request controls.
-   Common error and API-contract conventions.
-   Audit events and correlation identifiers.
-   Notification delivery abstraction.
-   External integration contracts.
-   Metrics, logs, tracing and health reporting.
-   Automated testing and build/deployment pipelines.

"Shared" does not mean every feature must be implemented as a separate
microservice. A capability may initially be a module or library. Its
deployment boundary is an architectural decision to make after
requirements and repository assessment.

### 3.3 Explicit exclusions from the initial release

Unless requirements change, the first release excludes: - Real-money
payment processing and live banking integrations. - Claims of production
banking or regulatory compliance. - A full web or mobile user
interface. - Complex tax, invoicing or regulatory features. -
Multi-warehouse supply-chain planning. - A general-purpose no-code
workflow builder. - Machine-learning recommendations or AI agents. -
Kubernetes or complex cloud infrastructure before the local product is
stable. - Every candidate business domain at once.

These are potential future directions, not permanent prohibitions.

------------------------------------------------------------------------

## 4. System Context and Logical Architecture

The following is a **logical view**, not a finalized deployment diagram.

``` text
 Human users / External clients / Partner systems
                       |
                       v
             Client applications
       (web UI later, API client, partner)
                       |
                       v
                API Gateway
       routing, edge policies, request IDs
                       |
          +------------+-------------+
          |            |             |
          v            v             v
      Identity      FinFlow       Commerce
      capability    domain        domain
          |            |             |
          v            v             v
     Identity data  FinFlow data  Commerce data
                       |
                       v
              Integration / Events
           (when required by workflow)
                       |
          +------------+-------------+
          |            |             |
          v            v             v
     Notification  External API   Other domain
       delivery     connectors    capabilities

Cross-cutting concerns:
security, audit, observability, configuration/secrets,
automated tests, CI/CD, containerised local environment.
```

Important qualifications: - The diagram shows logical responsibilities,
not a commitment to a service per box. - Each domain must have clear
ownership of its authoritative data. - A domain must not bypass another
domain's API or event contract to read or write its private tables. -
Synchronous APIs and asynchronous events should be selected based on
business needs. - No single cross-domain database transaction should be
assumed to exist across independent services.

------------------------------------------------------------------------

## 5. Client Applications: Responsibilities and Boundaries

A client can eventually be a browser-based application, mobile
application, API client, command-line tool, or external partner system.
The first release may use API clients and automated tests rather than a
full frontend.

### Client responsibilities

-   Collect user input and present business results.
-   Perform basic usability validation, while never being the sole
    enforcement point for business rules.
-   Send requests using documented API contracts.
-   Include authentication credentials or tokens as required.
-   Use an idempotency key for operations whose contract supports safe
    retries.
-   Handle validation errors, permission errors, conflicts, timeouts and
    pending statuses.
-   Avoid blindly repeating non-idempotent operations.
-   Display operation status and useful user-facing messages without
    exposing internal stack traces.
-   Never store secrets that belong on the server.

### Client must not

-   Connect directly to a service's private database.
-   Decide authoritative prices, balances, stock, permissions or
    transaction outcomes.
-   Treat a client-side success message as proof that the server
    committed an operation.
-   assume a timeout means the operation failed; the server may have
    completed it.
-   expose another organisation's identifiers or data as an
    authorisation mechanism.

### Client request lifecycle

1.  User initiates an operation.
2.  Client validates input for usability.
3.  Client sends the request to the public API entry point.
4.  Client receives a response with a result or operation/status
    identifier.
5.  For long-running work, client queries status or receives a supported
    notification.
6.  Client displays the final state returned by the authoritative
    backend.

------------------------------------------------------------------------

## 6. API Gateway: Responsibilities and Boundaries

The API Gateway is the controlled entry point for client-facing APIs. It
routes requests to the correct capability and applies suitable edge
policies.

### Candidate responsibilities

-   Route requests to the appropriate backend endpoint.
-   Terminate HTTPS in the deployed environment.
-   Enforce request-size limits and selected rate limits.
-   Add or propagate a request/correlation identifier.
-   Apply coarse-grained authentication checks where appropriate.
-   Apply common edge policies and standardise gateway-level errors.
-   Support API version routing or deprecation policy if required.
-   Avoid exposing internal hostnames and implementation details.
-   Provide basic request metrics and access logs with sensitive data
    redacted.

### Responsibilities that remain with domain services

-   Authoritative business validation.
-   Fine-grained business authorisation.
-   Ownership checks for organisation-specific resources.
-   Database transactions and domain invariants.
-   Idempotency enforcement for business operations.
-   Payment, wallet, order, stock or booking rules.
-   Domain-specific audit records and state transitions.

A gateway check is not a substitute for service-level authorisation.
Internal services must not trust a caller merely because the request
passed through the gateway.

### Gateway should not become

-   A place where business rules for every domain are implemented.
-   A shared database access layer.
-   A synchronous orchestrator for every multi-step business process.
-   A single point of uncontrolled dependency for internal
    service-to-service traffic.
-   A dumping ground for every cross-cutting concern.

### Gateway request flow

1.  Receive request and validate basic protocol requirements.
2.  Establish or propagate a request ID.
3.  Apply configured edge controls.
4.  Route to the target API.
5.  Return the response using the agreed public API contract.
6.  Record safe operational metadata without logging secrets,
    credentials or unnecessary personal data.

------------------------------------------------------------------------

## 7. Identity, Authentication and Authorisation

Identity is a candidate shared capability, but its exact implementation
depends on the existing repository and requirements.

### Responsibilities

-   Register or provision users according to the agreed product model.
-   Authenticate users and issue/validate supported credentials or
    tokens.
-   Manage organisation membership and role assignments.
-   Support resource-level authorisation rules.
-   Define account lifecycle and credential-recovery behaviour where in
    scope.
-   Record security-relevant events without logging secrets.
-   Support service identities for trusted service-to-service calls when
    needed.

### Key distinction

-   **Authentication:** Who or what is making the request?
-   **Authorisation:** Is that identity allowed to perform this action
    on this resource?
-   **Tenant/organisation isolation:** Does the resource belong to an
    organisation the caller is authorised to access?

Resource IDs must never be treated as proof of permission. Organisation
context and resource ownership must be checked server-side.

### Security requirements to define

-   Credential and token model.
-   Password or identity-provider policy.
-   Role and permission model.
-   Organisation membership rules.
-   Service-to-service trust.
-   Secret rotation and storage.
-   Rate limiting and abuse controls.
-   Audit and data-retention requirements.

Do not invent a custom authentication protocol if a well-supported
standard or identity provider meets the requirements.

------------------------------------------------------------------------

## 8. Business Domains and Data Ownership

### 8.1 FinFlow domain

Owns its authoritative financial simulation state, such as transaction
lifecycle and ledger/balance records. Other domains must interact
through defined contracts rather than directly modifying FinFlow
records.

Required design topics: - Money representation and currency rules. -
Ledger and balance invariants. - Idempotency. - Concurrent requests and
locking/optimistic concurrency. - Transaction status and failure
semantics. - Reconciliation and auditability. - Compensation or reversal
policy.

### 8.2 Commerce domain

Owns products, stock and orders according to the final product scope.

Required design topics: - Product and price validity. - Order totals and
currency. - Stock reservation/decrement/release policy. - Concurrent
order attempts. - Payment simulation state. - Cancellation and
refund-like simulation. - Order lifecycle and audit history.

### 8.3 Booking domain

If added, it owns availability and reservation invariants, including
conflict prevention, expiry, cancellation and lifecycle.

### 8.4 Data ownership rules

-   Each domain is the authoritative owner of its business records.
-   A domain may store a snapshot of external data needed for its own
    workflow, with a defined update policy.
-   Cross-domain reads should use APIs, events, or purpose-built read
    models where justified.
-   No service should write another domain's tables.
-   Schema changes should be managed through reviewed database
    migrations.
-   Retention, deletion, backup and recovery requirements must be
    specified.

The number of databases and services remains open until the domain
boundaries and operational needs are assessed.

------------------------------------------------------------------------

## 9. Integration and Messaging

### 9.1 Synchronous API calls

Use a synchronous API when the caller needs an immediate answer and the
operation can reasonably complete within the request budget.

Requirements: - Timeouts must be explicit. - Errors must be
classified. - Retries must be limited and safe. - Calls must not retry
non-idempotent operations blindly. - Circuit breaking or bulkheads may
be added where the failure risk justifies them. - Service contracts must
be versioned and tested.

### 9.2 Asynchronous events

Use events when independent follow-up work, buffering, decoupling or
eventual consistency is acceptable.

Illustrative events: - `OrderConfirmed` - `PaymentSimulationCompleted` -
`TransferCompleted` - `NotificationRequested` - `ShippingRequestFailed`

Event names and schemas are illustrative, not approved contracts.

### 9.3 Reliability patterns

Candidate patterns, introduced only when relevant: - Idempotency keys
and idempotent consumers. - Transactional outbox to coordinate database
changes and event publication. - Retry with backoff and jitter. -
Dead-letter handling and safe replay. - Saga or explicit process
orchestration for multi-step business operations. - Reconciliation for
detecting and repairing inconsistent outcomes. - Contract testing and
schema compatibility checks.

### 9.4 Delivery guarantees

Do not assume a message broker provides end-to-end exactly-once business
effects. A message may be delivered more than once, and a consumer may
fail after committing its database change but before acknowledging the
message. Design for duplicate delivery and define idempotent business
effects.

------------------------------------------------------------------------

## 10. Notifications and External Integrations

### Notification capability

Potential channels include email, in-app notifications or other
providers as requirements justify them.

Responsibilities: - Accept a defined notification request. - Select a
template and permitted channel. - Avoid exposing sensitive content. -
Track delivery attempt and result. - Retry transient failures within
limits. - Record terminal failures for investigation. - Avoid
duplicating externally visible effects where possible.

### External integration capability

Potential integrations include: - Payment-provider sandboxes. - Shipping
and fulfilment services. - CRM systems. - Email providers. -
Calendar/booking systems. - Webhook senders and receivers. - Other
business websites and APIs.

Requirements: - Keep credentials in secure server-side configuration. -
Define timeouts, retries and rate limits. - Validate inbound webhook
authenticity when supported. - Handle duplicate and out-of-order webhook
events. - Map provider-specific data into internal contracts. - Track
integration attempts and failure reasons. - Redact secrets and sensitive
data from logs. - Provide controlled replay or operator recovery where
justified.

Begin with one integration whose value is clear. Do not build a generic
integration marketplace prematurely.

------------------------------------------------------------------------

## 11. Core Business Flows

These flows are examples for requirements discovery. Exact status names
and transition rules must be approved.

### 11.1 Illustrative order flow

1.  Client submits an order request.
2.  Gateway routes the request and applies edge controls.
3.  Commerce validates identity, organisation access, products and
    quantities.
4.  Commerce calculates totals from authoritative prices.
5.  Commerce records the order and manages stock according to the
    approved reservation policy.
6.  Payment simulation produces an approved or declined result.
7.  Commerce transitions the order to the appropriate state.
8.  A committed business change may generate an event through a reliable
    publication mechanism.
9.  Notification and shipping integrations process follow-up work.
10. The client or operator can inspect the order status and audit
    history.

Failure cases to specify: - Invalid product or quantity. - Insufficient
stock. - Concurrent requests for the final item. - Duplicate order
request. - Payment decline. - Timeout with unknown payment outcome. -
Event publication failure. - Notification or shipping failure. -
Cancellation after reservation. - Recovery after a service restart.

### 11.2 Illustrative FinFlow transfer flow

1.  Client requests a simulated transfer with an idempotency key.
2.  API entry and identity controls are applied.
3.  FinFlow validates sender, recipient, amount, currency and
    permissions.
4.  FinFlow ensures duplicate requests do not create duplicate transfer
    effects.
5.  The system applies the defined debit/credit and ledger invariants
    using appropriate transactional and concurrency controls.
6.  The transfer reaches a documented state.
7.  Events or notifications are published reliably when needed.
8.  History and reconciliation allow the result to be inspected.

Failure cases to specify: - Insufficient balance. - Duplicate request. -
Concurrent transfers from the same account. - Timeout after the
transaction committed. - Failure during multi-step processing. -
Duplicate event delivery. - Discrepancy discovered by reconciliation.

A simulation must not be described as a real-money payment system.

### 11.3 Illustrative booking flow

1.  Client requests a time slot or resource.
2.  Booking validates the requested period and permissions.
3.  The system checks availability using a concurrency-safe operation.
4.  A reservation is created with a defined expiry or confirmation
    policy.
5.  Confirmation and notification follow.
6.  Cancellation or expiry releases the resource according to the agreed
    rules.

------------------------------------------------------------------------

## 12. State Machines and Business Rules

Each important domain object should have a documented lifecycle rather
than accepting arbitrary status updates.

Example order lifecycle, subject to requirements:

``` text
DRAFT / CREATED
      |
      v
PENDING_PAYMENT
   /        \
  v          v
CONFIRMED  PAYMENT_FAILED
  |
  v
FULFILMENT_PENDING
  |
  v
FULFILLED
```

Cancellation, expiry, refunds and retries require explicit transitions
and preconditions. The final model may differ.

For each transition, document: - The permitted source state. - The
target state. - Who or what can trigger it. - Preconditions. - Database
changes and invariants. - Events produced. - Retry and duplicate-request
behaviour. - Recovery or compensation behaviour. - Audit fields and
timestamps.

Never rely solely on the client to enforce state transitions.

------------------------------------------------------------------------

## 13. Technology Baseline and Maintenance Strategy

The following is a candidate technology inventory, not a requirement to
adopt every tool.

  ----------------------------------------------------------------------------
  Area              Candidate           Intended role     Adoption stage
  ----------------- ------------------- ----------------- --------------------
  Language          Java 21             Main backend      Preserve existing
                                        language          baseline pending
                                                          audit

  Backend framework Spring Boot 3.5.x   APIs and          Preserve existing
                                        application       baseline pending
                                        services          audit

  Security          Spring Security;    Authentication    First-release
                    standards-based     and authorisation requirement
                    identity where                        
                    appropriate                           

  Persistence       Spring Data JPA     Persistence and   First-release
                    where appropriate;  data access       requirement
                    SQL                                   

  Database          PostgreSQL, subject Relational        Candidate; verify
                    to repository audit business data     current state

  API contracts     REST and OpenAPI    Client and        First release
                                        service contracts 

  Migrations        Flyway or           Versioned schema  First release
                    Liquibase, selected changes           
                    after audit                           

  Unit testing      JUnit 5, Mockito    Business and unit First release
                                        tests             

  Integration       Testcontainers and  Test real         First release where
  testing           application         dependencies      useful
                    integration tests                     

  Local runtime     Docker, Docker      Reproducible      First release
                    Compose             local environment 

  Messaging         Kafka, if           Events and        Later milestone
                    asynchronous use    decoupled work    
                    cases justify it                      

  Cache             Redis, if a         Cache and         Later milestone
                    measured use case   selected          
                    justifies it        coordination      

  Observability     Structured logs,    Diagnose          Introduce
                    metrics,            behaviour and     progressively
                    OpenTelemetry       failures          
                    tracing                               

  CI/CD             Repository-hosted   Build, test and   Early delivery stage
                    automation such as  delivery checks   
                    GitHub Actions or                     
                    an equivalent                         

  API documentation OpenAPI and a       Discoverability   First release
                    maintained          and contracts     
                    developer guide                       

  Cloud             A selected cloud    Hosted deployment After local
                    provider, later                       stability

  Orchestration     Kubernetes, later   Container         Advanced stage
                    if useful           orchestration     

  Infrastructure as Terraform or an     Reproducible      When cloud scope is
  code              equivalent          infrastructure    defined

  AI integrations   Spring AI or direct Optional          Future,
                    provider SDKs, if   AI-backed         requirement-driven
                    justified           features          

  API style         gRPC where          Selected internal Optional
  extension         justified           contracts         
  ----------------------------------------------------------------------------

### Version and dependency policy

-   Keep the existing Java 21 and Spring Boot 3.5.x baseline during
    discovery.
-   Verify the actual versions and dependency status from the repository
    before recommending upgrades.
-   Do not upgrade only because a newer major version exists.
-   Prefer supported, maintained versions and record the reason for each
    major dependency.
-   Use a dependency-management strategy consistent with the existing
    Maven setup.
-   Keep secrets out of source control.
-   Document environment variables and local setup.
-   Run automated tests and security/dependency checks in CI.
-   Review dependencies periodically for security fixes and
    compatibility.
-   Pin or constrain versions reproducibly.
-   Record upgrade and rollback considerations for meaningful changes.

### Technology selection rule

Every technology must have: 1. A concrete requirement it solves. 2. A
reason simpler alternatives are insufficient or less suitable. 3.
Operational and maintenance costs understood. 4. A testing and
monitoring plan. 5. A decision record when the choice materially affects
architecture.

------------------------------------------------------------------------

## 14. Quality Attributes and Non-Functional Requirements

These are proposed goals. Quantitative service-level objectives remain
to be defined.

### Correctness and consistency

-   Monetary values use decimal-safe representations and explicit
    currency rules.
-   Stock and booking availability are protected against concurrent
    conflicting operations.
-   Business state transitions are validated server-side.
-   Database constraints complement application validation.
-   Cross-domain partial failure has a defined recovery strategy.

### Security and privacy

-   Authenticate callers and authorise every protected operation.
-   Enforce organisation/resource isolation server-side.
-   Validate input and use parameterised data access.
-   Store secrets securely and rotate them when required.
-   Avoid logging passwords, tokens, secrets or unnecessary personal
    data.
-   Define audit and data-retention requirements.
-   Use least privilege for users, services and infrastructure.

### Reliability

-   Use explicit timeouts for remote calls.
-   Retry only operations whose retry behaviour is safe and bounded.
-   Make duplicate delivery and request retries safe where required.
-   Track failed asynchronous work.
-   Document recovery after process restarts and partial failures.
-   Backups and restore tests are required before claiming recovery
    readiness.

### Performance

-   Define latency and throughput targets before performance claims.
-   Measure before adding caches or infrastructure.
-   Inspect database queries, indexes, connection pools and resource
    use.
-   Run repeatable load tests when a target workload is known.

### Maintainability

-   Keep business rules close to the domain that owns them.
-   Separate API models, application logic, domain logic and
    infrastructure concerns appropriately.
-   Avoid unnecessary abstractions and patterns.
-   Use consistent naming, error handling and documentation.
-   Prefer small, reviewable changes with automated tests.

### Observability

-   Use request/correlation identifiers.
-   Emit structured logs with safe contextual fields.
-   Record important business outcomes and failure reasons.
-   Expose health and readiness information appropriate to the runtime.
-   Add metrics and distributed tracing when multiple components make
    them useful.
-   Avoid sensitive information in telemetry.

### Portability and delivery

-   Document reproducible local startup and test steps.
-   Keep configuration environment-specific and outside source code.
-   Make build and test results repeatable in CI.
-   Add container and cloud deployment complexity incrementally.

------------------------------------------------------------------------

## 15. Testing Strategy

Testing must verify business correctness, not only endpoint status
codes.

### Unit tests

-   Business calculations and validation.
-   State transitions.
-   Permission decisions where practical.
-   Idempotency decisions and error classification.

### Integration tests

-   Database persistence and transactions.
-   Schema constraints and migrations.
-   API serialization and error responses.
-   Authentication and authorisation.
-   Messaging or external connector behaviour when introduced.

### Contract tests

-   Public API contract compatibility.
-   Service-to-service contract compatibility.
-   Event schema compatibility.
-   External provider adapter behaviour.

### End-to-end tests

-   A complete approved business journey.
-   Failure and recovery scenarios.
-   Organisation isolation.
-   Repeated requests and status polling.

### Concurrency and resilience tests

-   Two orders competing for the final unit of stock.
-   Concurrent transfers against the same balance.
-   Duplicate message delivery.
-   Timeouts and retries.
-   Consumer restart and recovery.
-   Integration provider unavailable.
-   Failure after database commit but before event
    acknowledgement/publication.

Use realistic test dependencies where useful; avoid making all tests
depend on a running production-like infrastructure stack.

------------------------------------------------------------------------

## 16. Error Handling and API Contracts

Define a consistent API error format with, at minimum: - A stable
machine-readable error code. - A safe, understandable message. -
Request/correlation identifier. - Optional field-level validation
details. - No stack traces or secrets in public responses.

Candidate HTTP semantics should be documented consistently, for
example: - `400` for malformed requests. - `401` for missing or invalid
authentication. - `403` for authenticated but unauthorised requests. -
`404` when a resource is not found or must not be disclosed. - `409` for
a business-state conflict. - `422` for a semantically invalid request if
adopted consistently. - `429` for rate limiting. - `5xx` for unexpected
server-side failures.

The final API contract must choose and consistently apply its
conventions. Do not return success for a failed business operation
simply to avoid handling errors.

------------------------------------------------------------------------

## 17. Data, Audit and Reconciliation

### Data principles

-   Identify the authoritative owner for every business record.
-   Define identifiers, timestamps, currency and status fields
    consistently.
-   Use migrations for schema evolution.
-   Define deletion, retention and privacy rules.
-   Do not share private database credentials across domain boundaries.
-   Define backup and restore procedures before making recovery claims.

### Audit history

Record significant business actions with appropriate information such
as: - Actor or service identity. - Organisation context. - Action and
affected resource. - Timestamp. - Result and safe reason code. -
Correlation/request identifier.

Audit history is not the same as debug logging. It should have explicit
integrity, access and retention requirements.

### Reconciliation

For financial-like operations and other multi-step workflows,
reconciliation compares recorded states and identifies discrepancies.
Define how discrepancies are reported, investigated and corrected.
Corrections must be controlled, auditable operations rather than silent
data edits.

------------------------------------------------------------------------

## 18. Repository Structure and Code Organisation

The existing repository must be inspected before its structure is
changed. The following is a **candidate organisational model**, not a
mandate to create all these directories immediately.

``` text
finflow-microservices/
├── api-gateway/
├── identity-service/
├── wallet-service/             # Existing name may be retained if appropriate
├── notification-service/
├── commerce-service/            # Future, if requirements justify it
├── booking-service/             # Future, if requirements justify it
├── integration-service/         # Future, if requirements justify it
├── infrastructure/
│   ├── local/
│   └── deployment/              # Added when justified
├── docs/
│   ├── product/
│   ├── architecture/
│   ├── decisions/
│   ├── api/
│   ├── operations/
│   └── learning/
├── compose.yaml                  # Actual location to verify
└── README.md
```

Do not create placeholder services solely to match this picture.

### Candidate internal package organisation

Use domain/feature-oriented packages with clear responsibility
boundaries, adapting to the existing codebase:

``` text
com.example.domain
├── api/                 # Controllers, request/response DTOs
├── application/         # Use cases, orchestration within the domain
├── domain/              # Business rules, entities/value objects, domain events
└── infrastructure/      # Persistence adapters, external clients, messaging
```

This is a conceptual template. Exact package names and whether a
separate domain layer is appropriate depend on the service complexity.
Do not force a large layered structure into trivial code.

### Code organisation principles

-   Keep request/response DTOs separate from persistence entities where
    useful.
-   Avoid exposing internal database models as public API contracts.
-   Put business rules in the appropriate domain/application layer, not
    in controllers.
-   Use dependency inversion for genuine external boundaries.
-   Apply SOLID principles pragmatically.
-   Use design patterns when they make a real responsibility or
    variation easier to manage.
-   Avoid giant service classes, duplicated rules and generic
    abstractions with no demonstrated use case.
-   Keep public contracts explicit and versioned where required.

------------------------------------------------------------------------

## 19. API Gateway and Service Communication Rules

### Client-to-platform

-   Client communicates through the documented public entry point.
-   Gateway applies routing and edge controls.
-   Target capability validates business input and authorisation.
-   Domain owner returns a contract-defined result.

### Service-to-service

-   Use a documented API or event contract.
-   Authenticate service identities as appropriate.
-   Set timeouts and bounded retry rules.
-   Propagate correlation identifiers.
-   Avoid circular synchronous dependencies.
-   Avoid chains of synchronous calls that make a simple request depend
    on many services.
-   Do not use another service's database as an integration mechanism.

### Public versus internal APIs

-   Public APIs should be stable, documented and designed for client
    needs.
-   Internal contracts can evolve under controlled compatibility rules.
-   Do not expose internal endpoints unintentionally.
-   Define versioning, deprecation and backward compatibility policies.

------------------------------------------------------------------------

## 20. Operations, Deployment and Environments

### Local development

Initial target: local development using Docker Compose, once the current
setup has been audited.

Candidate services include the application components and required
dependencies. Do not add PostgreSQL, Kafka, Redis or other
infrastructure until we verify what exists and which requirements need
them.

### Environments

Define at least: - Local development. - Automated CI/test environment. -
A later staging environment if deployment scope justifies it. -
Production-like deployment only when the project is ready and its
limitations are clearly stated.

### Configuration

-   Keep configuration outside compiled code.
-   Use environment-specific configuration.
-   Never commit credentials.
-   Document required variables and safe local defaults.
-   Use managed secrets in hosted environments where available.

### Health and operations

-   Distinguish process liveness from readiness to handle requests.
-   Avoid reporting ready when essential dependencies make the service
    unable to fulfil its contract.
-   Document startup, shutdown and migration behaviour.
-   Define logs, metrics and alerts as requirements mature.
-   Test recovery from a restart and dependency outage.

### CI/CD

Candidate pipeline: 1. Checkout source. 2. Validate build and dependency
resolution. 3. Run formatting/static checks where configured. 4. Run
unit tests. 5. Run integration/contract tests at the appropriate stage.
6. Build container images. 7. Run security and dependency checks. 8.
Publish versioned artifacts only after required checks pass. 9. Deploy
through an explicit, documented process when deployment is in scope.

------------------------------------------------------------------------

## 21. Risks and Mitigations

  -----------------------------------------------------------------------
  Risk                                Mitigation
  ----------------------------------- -----------------------------------
  Scope becomes too broad             Select one coherent first-release
                                      workflow and maintain explicit
                                      exclusions.

  Too many microservices              Justify boundaries through domain
                                      ownership and deployment needs; do
                                      not split solely for technology
                                      demonstrations.

  Shared platform becomes a dumping   Keep common capabilities small,
  ground                              cohesive and based on repeated
                                      requirements.

  Cross-domain data inconsistency     Define ownership, API/event
                                      contracts, idempotency and recovery
                                      behaviour.

  Duplicate requests cause duplicate  Specify idempotency semantics for
  effects                             important commands.

  Concurrency corrupts balances or    Define invariants and test
  stock                               database-level concurrency control.

  Technology churn                    Preserve the verified baseline and
                                      upgrade through reviewed
                                      compatibility decisions.

  Sensitive data leaks through logs   Define redaction, access control
                                      and telemetry policy.

  External API instability            Use timeouts, bounded retries,
                                      failure tracking and safe replay.

  Existing defects are overlooked     Audit the actual repository and
                                      establish a test baseline before
                                      changes.

  Overengineering harms learning      Introduce each tool/pattern in
                                      response to a concrete requirement
                                      and explain alternatives.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 22. Delivery Roadmap

### Phase 0 --- Requirements and repository discovery

-   Approve product vision and intended users.
-   Define first-release business workflow.
-   Define user journeys and business rules.
-   Define non-functional requirements and measurable targets.
-   Inspect the existing `finflow-microservices` repository,
    dependencies, tests, configuration and local deployment.
-   Record current behaviour and gaps without assuming historical issues
    are still present.
-   Produce a reviewed PRD and architecture decision log.

**Exit criterion:** The first release has clear scope, acceptance
criteria and known risks. No implementation changes are made before
approval.

### Phase 1 --- First usable workflow

-   Implement or stabilise the selected business flow.
-   Establish API contracts, validation and persistence.
-   Add unit and integration tests.
-   Document errors, state transitions and local execution.
-   Verify all acceptance criteria.

**Exit criterion:** The workflow works end to end and has repeatable
tests.

### Phase 2 --- Correctness and resilience

-   Add idempotency and concurrency controls where required.
-   Test timeouts, duplicate requests and partial failures.
-   Define recovery and audit behaviour.
-   Add observability for the relevant flow.

**Exit criterion:** Important failure scenarios are reproducible, tested
and handled according to documented rules.

### Phase 3 --- First integration

-   Select one external sandbox or service integration.
-   Define authentication, contract, timeouts and failure policy.
-   Add tests and safe failure visibility.
-   Add asynchronous messaging only if the use case warrants it.

**Exit criterion:** Integration failures do not silently corrupt the
authoritative business state.

### Phase 4 --- Reusable platform capabilities

-   Identify capabilities genuinely shared by multiple workflows.
-   Extract or standardise only where the benefits are demonstrated.
-   Add organisation isolation and shared identity behaviour according
    to approved requirements.

**Exit criterion:** At least two use cases can reuse an appropriate
capability without sharing inappropriate business rules or private data.

### Phase 5 --- Additional domain

-   Add commerce, FinFlow extensions, booking or another approved
    domain.
-   Define its domain-specific invariants and acceptance criteria.
-   Demonstrate reuse without coupling the new domain to another
    domain's persistence.

### Phase 6 --- Production-minded delivery

-   Add performance tests and measurable targets.
-   Strengthen CI/CD, dependency security and observability.
-   Add cloud deployment and infrastructure as code if useful.
-   Consider Kubernetes only when its operational learning value
    justifies its cost.
-   Document limitations and operational runbooks.

Phases can be reordered based on repository findings and requirements.
Do not treat the roadmap as a commitment to build every capability.

------------------------------------------------------------------------

## 23. Interview Learning Built Into the Project

For each substantial feature, document and practise:

1.  Business requirement and acceptance criteria.
2.  Domain model and state transitions.
3.  API design and error semantics.
4.  Data model, SQL and indexes.
5.  Transaction boundaries and consistency.
6.  Concurrency and locking decisions.
7.  Security and authorisation.
8.  Unit, integration, contract and failure tests.
9.  Alternatives and trade-offs.
10. Performance and operational implications.
11. Relevant design patterns and why they are justified.
12. Interview follow-up questions and concise answers.

The objective is not only to make the system work. It is to be able to
explain why it works, where it can fail and how it should evolve.

------------------------------------------------------------------------

## 24. Decisions Still Open

These questions must be resolved before architecture is finalised:

-   What is the exact first-release workflow: a FinFlow transaction, a
    commerce order, or an integration workflow?
-   Should multiple organisations be supported in the first release?
-   Which existing services and capabilities currently work, and what
    needs to be fixed?
-   What is the first external integration and what business value does
    it provide?
-   Which operations require synchronous responses, and which may be
    asynchronous?
-   Which persistence and messaging technologies are already configured
    and suitable?
-   What performance, availability, recovery and data-retention targets
    are appropriate?
-   Does the first release need a UI, or are APIs and an API client
    sufficient?
-   Which capabilities should be shared, and which should remain
    domain-owned?

No answer should be assumed merely because it appears in a conceptual
diagram.

------------------------------------------------------------------------

## 25. Definition of Success

The platform is successful when:

-   It solves a defined business problem through a complete workflow.
-   Business invariants are explicit and protected by code and tests.
-   Authentication, authorisation and organisation isolation are
    enforced server-side.
-   Duplicate requests, concurrency and partial failures are handled
    according to documented policies.
-   Data ownership and API/event contracts are clear.
-   The system can be run and tested reproducibly.
-   Operational behaviour can be investigated through appropriate logs,
    metrics and traces.
-   A second domain or integration can be added without uncontrolled
    coupling.
-   The technology choices are justified, maintained and understood.
-   The developer can explain the system's architecture, trade-offs and
    failure modes at senior-engineer interview depth.

------------------------------------------------------------------------

## 26. Immediate Next Steps

1.  Review this blueprint and correct the product direction if
    necessary.
2.  Approve or revise the initial user journeys and candidate scope.
3.  Select the first-release workflow.
4.  Audit the existing repository and verify actual versions, service
    responsibilities, data stores, tests and local runtime.
5.  Update this document with verified current-state facts and
    decisions.
6.  Produce a concise, approved first-release PRD and a separate
    architecture decision record.
7.  Only then plan the first small implementation milestone.

**Current status:** Planning draft. The product vision is proposed;
service boundaries, infrastructure choices, first-release scope and
detailed acceptance criteria remain subject to review and approval.
