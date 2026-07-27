# Vendra Architecture

Vendra is implemented as a Spring Boot modular monolith. The application keeps modules in one deployable unit for simple local development and portfolio demos, while keeping package boundaries clear enough to split into services later.

## System Context

```mermaid
flowchart LR
    User["Procurement users"] --> API["Spring Boot REST API"]
    Swagger["Swagger UI"] --> API
    MCP["MCP-style clients"] --> ToolAPI["/mcp tool endpoints"]
    Chat["AI chat client"] --> ChatAPI["/ai/chat"]

    API --> Security["JWT + RBAC"]
    ToolAPI --> Security
    ChatAPI --> Security

    Security --> Modules["Procurement modules"]
    Modules --> DB[("H2 local / PostgreSQL docker")]
    Modules --> Cache[("Redis cache and rate limit")]
    Modules --> Kafka["Kafka events"]
    Modules --> Audit[("Audit log")]

    Kafka --> Notifications["Notification consumer"]
```

## Runtime Architecture

```mermaid
flowchart TB
    subgraph App["Vendra Spring Boot application"]
        Controllers["REST controllers"]
        SecurityLayer["Security filter + method security"]
        Services["Domain services"]
        Repositories["Spring Data repositories"]
        AuditAspect["Audit AOP aspect"]
        EventPublisher["Kafka event publisher"]
        ToolRegistry["MCP-style tool registry"]
        ChatService["Chat orchestration"]
        RagServices["RAG and document services"]
    end

    Controllers --> SecurityLayer
    SecurityLayer --> Services
    ToolRegistry --> Services
    ChatService --> RagServices
    Services --> Repositories
    Services --> EventPublisher
    Services --> AuditAspect
    RagServices --> Repositories
```

## Module Boundaries

| Package | Responsibility |
|---|---|
| `auth` | Users, roles, JWT login/refresh, security configuration |
| `vendor` | Vendor CRUD, filtering, risk score, performance history |
| `quotation` | Quote intake, RFQ listing, deterministic quote comparison scoring |
| `purchaseorder` | PO lifecycle, approval chain, approval/rejection workflow |
| `contract` | Contract records, document upload, risk analysis |
| `ai.rag` | Text extraction, chunking, chunk storage, keyword retrieval |
| `ai.chat` | Chat endpoint, rate limiting, lightweight orchestration |
| `ai.mcp` | Tool discovery and tool-call API over secured services |
| `inventory` | SKU stock lookup |
| `audit` | `@AuditLogged` annotation and audit log persistence |
| `notification` | Kafka consumer stub with idempotent processed-event storage |
| `infra.kafka` | Event DTOs and Kafka/no-op publisher abstraction |
| `infra.seed` | Demo users and sample procurement data |
| `common` | Shared DTOs, exceptions, error handling, security helpers |

## Domain Model

```mermaid
erDiagram
    APP_USER ||--o{ APP_USER_ROLE : has
    VENDOR ||--o{ VENDOR_PERFORMANCE_HISTORY : records
    VENDOR ||--o{ QUOTATION : submits
    VENDOR ||--o{ PURCHASE_ORDER : receives
    VENDOR ||--o{ CONTRACT : owns
    PURCHASE_ORDER ||--o{ APPROVAL_STEP : requires
    CONTRACT ||--o{ DOCUMENT_CHUNK : produces
    AUDIT_LOG }o--|| APP_USER : actor

    VENDOR {
        long id
        string name
        string category
        decimal rating
        string complianceStatus
        int riskScore
    }

    PURCHASE_ORDER {
        long id
        string status
        decimal amount
        string approverChain
        string createdBy
    }

    CONTRACT {
        long id
        string documentUrl
        string riskLevel
        date expiryDate
    }

    DOCUMENT_CHUNK {
        long id
        long sourceDocId
        long vendorId
        string sourceType
        string content
        string metadataJson
    }
```

## Key Flows

### Authentication

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant UserRepository
    participant JwtService

    Client->>AuthController: POST /auth/login
    AuthController->>AuthService: login(email, password)
    AuthService->>UserRepository: findByEmail
    AuthService->>JwtService: generateAccessToken
    AuthService->>UserRepository: persist refresh token hash
    AuthService-->>Client: accessToken + refreshToken
```

### Purchase Order Approval

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> PENDING_APPROVAL: submit
    PENDING_APPROVAL --> APPROVED: all approval steps approved
    PENDING_APPROVAL --> REJECTED: any approver rejects
    APPROVED --> FULFILLED: downstream fulfillment
    REJECTED --> [*]
    FULFILLED --> [*]
```

### Contract RAG and Risk Analysis

```mermaid
sequenceDiagram
    participant User
    participant ContractController
    participant ContractService
    participant Extractor
    participant Chunker
    participant ChunkRepo
    participant RiskService

    User->>ContractController: POST /contracts/{id}/documents
    ContractController->>ContractService: uploadDocument
    ContractService->>Extractor: extract PDF/text
    ContractService->>Chunker: split text into chunks
    ContractService->>ChunkRepo: save DocumentChunk rows
    User->>ContractController: POST /contracts/{id}/risk-analysis
    ContractController->>RiskService: analyze
    RiskService->>ChunkRepo: load contract chunks
    RiskService-->>User: risk score + cited findings
```

## Security Architecture

- JWT access tokens protect all non-public endpoints.
- Method-level RBAC is enforced with `@PreAuthorize` on services and controllers.
- Tool calls in `ai.mcp` reuse the same service methods as REST, so tools do not bypass authorization.
- Refresh tokens are stored as SHA-256 hashes on `AppUser`.
- Mutating workflows use `@AuditLogged`, which writes actor, action, entity, timestamp, and serialized arguments to `audit_logs`.

Roles:

- `ADMIN`
- `PROCUREMENT_OFFICER`
- `APPROVER_L1`
- `APPROVER_L2`
- `VENDOR_MANAGER`

## Event Architecture

Kafka is optional in the local profile and enabled in the Docker profile.

| Event | Producer | Consumer |
|---|---|---|
| `po.created` | `PurchaseOrderService` | Notification consumer |
| `po.approved` | `PurchaseOrderService` | Notification consumer |
| `po.rejected` | `PurchaseOrderService` | Notification consumer |
| `document.uploaded` | `ContractService` | Future RAG async worker |
| `vendor.risk.flagged` | Future risk scheduler | Notification consumer |

The current publisher logs events when Kafka is disabled. In Docker mode, it publishes to Kafka.

## Deployment Profiles

| Profile | Database | Cache | Kafka | Use case |
|---|---|---|---|---|
| default | H2 in-memory | Spring simple cache | Disabled | Fast local demo |
| test | H2 in-memory | Spring simple cache | Disabled | Unit/context tests |
| docker | PostgreSQL | Redis | Enabled | Integrated environment |

Docker Compose includes:

- App container
- PostgreSQL 16
- Redis 7
- Kafka
- Zookeeper

## API Surfaces

- Human-facing REST API: `/auth`, `/vendors`, `/quotations`, `/purchase-orders`, `/contracts`, `/inventory`
- AI chat endpoint: `/ai/chat`
- Tool API: `/mcp/tools` and `/mcp/tools/{toolName}/call`
- Documentation: `/swagger-ui.html`
- Operations: `/actuator/health`, `/actuator/info`, `/actuator/metrics`

## Extension Points

- Replace `HeuristicLlmClient` with an OpenAI-backed implementation.
- Replace keyword retrieval with pgvector cosine similarity.
- Move document ingestion from synchronous upload to a Kafka consumer.
- Add an outbox table for reliable event publication.
- Add Testcontainers for PostgreSQL, Redis, and Kafka integration flows.
