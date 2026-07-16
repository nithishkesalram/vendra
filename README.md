# Procure AI

AI Procurement and Vendor Intelligence Platform built as a Spring Boot modular monolith.

Architecture details and diagrams live in [ARCHITECTURE.md](ARCHITECTURE.md).

## What is included

- JWT auth with role-based method security
- Vendor CRUD, filtering, scoring, and performance history
- Quotation intake and deterministic AI-style comparison scoring
- Purchase order drafting, submission, and L1/L2 approval workflow
- Contract upload, PDF/text extraction, chunk storage, retrieval, and risk analysis
- Inventory lookup
- Audit logging for mutating operations
- Kafka-ready event publisher and notification consumer
- Chat endpoint with rate limiting
- MCP-style tool discovery and tool-call endpoints that reuse secured services
- Docker Compose for app, PostgreSQL, Redis, Kafka, and Zookeeper

## Run locally

The default profile uses in-memory H2 so the app starts without Docker:

```powershell
mvn spring-boot:run
```

Open:

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console

## Demo users

All seeded users use password `password`.

| Email | Role |
|---|---|
| admin@procureai.local | ADMIN |
| officer@procureai.local | PROCUREMENT_OFFICER |
| approver1@procureai.local | APPROVER_L1 |
| approver2@procureai.local | APPROVER_L2 |
| vendor-manager@procureai.local | VENDOR_MANAGER |

Login:

```http
POST /auth/login
Content-Type: application/json

{
  "email": "admin@procureai.local",
  "password": "password"
}
```

Use the returned access token as `Authorization: Bearer <token>`.

## Useful demo endpoints

- `GET /vendors`
- `GET /vendors/{id}/performance`
- `POST /quotations/rfq/RFQ-2026-001/compare`
- `GET /inventory/ELEC-CTRL-01`
- `POST /purchase-orders`
- `POST /purchase-orders/{id}/submit`
- `POST /purchase-orders/{id}/approve`
- `POST /contracts`
- `POST /contracts/{id}/documents`
- `POST /contracts/{id}/risk-analysis`
- `POST /ai/chat`
- `GET /mcp/tools`
- `POST /mcp/tools/{toolName}/call`

## Docker profile

```powershell
docker compose up --build
```

The Docker profile switches persistence to PostgreSQL, cache infrastructure to Redis, and enables Kafka event publication.

## Verification

```powershell
mvn test
```
