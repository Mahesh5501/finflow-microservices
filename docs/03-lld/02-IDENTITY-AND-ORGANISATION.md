# 2. Identity and Organisation — Complete Design

**Process:** `identity-service` · Port `8081` · Spring Boot + Security + JPA  
**Owns:** Users, organisations, memberships, roles, permissions, JWT issue  
**Database:** `identity_db`

---

## 1. Position

```mermaid
flowchart TD
    Client[Client] --> Gateway[API Gateway]
    Gateway --> IAM

    subgraph IAM["Identity Service"]
        Authn[Authentication]
        Token[JWT Issue / Validate]
        Context[User + Org Context]
        Policy[Roles and Permissions]
    end

    IAM --> IDB[(identity_db)]
    Gateway --> FinFlow
    Gateway --> Commerce
    FinFlow -.->|uses token context| IAM
    Commerce -.->|uses token context| IAM
```

**Three questions every request answers**

| Question | Concept |
|---|---|
| Who are you? | Authentication |
| What may you do? | Authorisation (permission) |
| Which resource? | Resource + organisation ownership (in domain) |

---

## 2. Internal architecture

```mermaid
flowchart TB
    API[Controller · DTOs]
    APP[Use cases\nRegister · Login · Invite · Assign role]
    DOMAIN[User · Organisation · Membership · Role]
    REPO[Repositories]
    DB[(identity_db)]

    API --> APP --> DOMAIN
    APP --> REPO --> DB
```

```text
com.finflow.identity
  api
  application
  domain
  infrastructure.persistence
  infrastructure.security
```

---

## 3. Domain model

```mermaid
erDiagram
    USERS ||--o{ ORGANISATION_MEMBERSHIPS : has
    ORGANISATIONS ||--o{ ORGANISATION_MEMBERSHIPS : has
    ORGANISATION_MEMBERSHIPS ||--o{ MEMBERSHIP_ROLES : has
    ROLES ||--o{ MEMBERSHIP_ROLES : assigned
    ROLES ||--o{ ROLE_PERMISSIONS : has
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : granted

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar status
        datetime created_at
    }
    ORGANISATIONS {
        bigint id PK
        varchar name
        varchar status
        datetime created_at
    }
    ORGANISATION_MEMBERSHIPS {
        bigint id PK
        bigint user_id FK
        bigint organisation_id FK
        varchar status
    }
    ROLES {
        bigint id PK
        varchar code UK
        varchar name
    }
    PERMISSIONS {
        bigint id PK
        varchar code UK
    }
    MEMBERSHIP_ROLES {
        bigint membership_id FK
        bigint role_id FK
    }
    ROLE_PERMISSIONS {
        bigint role_id FK
        bigint permission_id FK
    }
```

**Membership status:** `INVITED` → `ACTIVE` → `SUSPENDED` / `REMOVED`

**Example roles:** `ORG_ADMIN`, `FINANCE_OPERATOR`, `ORDER_MANAGER`, `MEMBER`, `READ_ONLY`

**Example permissions:** `wallets:read`, `transfers:create`, `orders:create`, `orders:cancel`, `organisation:members:manage`

---

## 4. APIs

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | Create user |
| POST | `/api/v1/auth/login` | Issue JWT |
| GET | `/api/v1/users/me` | Current user |
| POST | `/api/v1/organisations` | Create organisation |
| POST | `/api/v1/organisations/{id}/members` | Invite / add member |
| PUT | `/api/v1/organisations/{id}/members/{userId}/roles` | Assign roles |
| GET | `/api/v1/organisations/{id}/members` | List members |

**Register**

```json
{ "email": "a@example.com", "password": "long-secret" }
```

**Login response**

```json
{
  "accessToken": "<jwt>",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

**JWT claims:** `sub` (userId), `email`, `orgIds[]`, `permissions[]`, `iss`, `exp`

Password: BCrypt. Never return hash. Disabled user cannot login.

---

## 5. Auth flows

### Login

```mermaid
sequenceDiagram
    actor User
    participant GW as Gateway
    participant ID as Identity
    participant DB as identity_db

    User->>GW: POST /auth/login
    GW->>ID: Forward
    ID->>DB: Load user by email
    ID->>ID: Verify password hash
    ID->>ID: Build JWT claims
    ID-->>User: accessToken
```

### Protected call

```mermaid
sequenceDiagram
    actor User
    participant GW as Gateway
    participant Svc as FinFlow / Commerce

    User->>GW: API + Bearer token
    GW->>GW: Validate signature, iss, exp
    GW->>Svc: Forward + X-User-Id + X-Organisation-Id
    Svc->>Svc: Check permission + resource ownership
    alt Not allowed
        Svc-->>User: 403
    else Allowed
        Svc-->>User: Business result
    end
```

Gateway JWT check is **not enough**. FinFlow/Commerce still verify wallet/order ownership.

---

## 6. Authorisation cascade

```mermaid
flowchart TD
    Req[Request] --> A{Authenticated?}
    A -->|No| R[401]
    A -->|Yes| B{Has permission?}
    B -->|No| R2[403]
    B -->|Yes| C{Org / resource owned?}
    C -->|No| R3[403]
    C -->|Yes| D{Business rules OK?}
    D -->|No| R4[422 / 409]
    D -->|Yes| OK[Execute]
    R --> AUD[Audit]
    R2 --> AUD
    R3 --> AUD
    R4 --> AUD
    OK --> AUD
```

---

## 7. Rules

- Knowing an id is not permission (no BOLA / IDOR).
- Role in Org A does not grant Org B.
- Client-supplied `organisationId` is untrusted — verify membership.
- Do not forward user JWT to every internal service by default.
- Service-to-service uses a service identity / client credentials.
- Membership removal must invalidate or short-expire access (short JWT lifetime).
