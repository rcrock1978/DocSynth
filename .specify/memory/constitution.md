<!--
  Sync Impact Report
  Version change: (template) → 1.0.0
  Modified principles: (all new — initial fill)
  Added sections: Core Principles (I–V), Technology & Architecture Constraints,
    Development Workflow & Quality Gates, Governance
  Removed sections: none
  Templates requiring updates:
    - .specify/templates/plan-template.md → ✅ already aligned (Constitution Check gate)
    - .specify/templates/spec-template.md → ✅ already aligned
    - .specify/templates/tasks-template.md → ✅ already aligned
  Follow-up TODOs: RATIFICATION_DATE unknown — marked TODO
-->

# DocSynth Constitution

## Core Principles

### I. Spec-Driven Development (NON-NEGOTIABLE)

The spec is the authoritative source of truth. Code, tests, and documentation MUST
derive from and trace back to the spec (`spec.md`). Every change starts with a
spec update. No implementation is accepted without a corresponding spec entry and
passing acceptance criteria. This ensures the product remains auditable,
testable, and coherent from day one.

### II. Clean Architecture & DDD

Dependency rules point strictly inward: Domain → Application → Infrastructure →
Presentation. Bounded contexts are service boundaries. Anti-Corruption Layers
MUST protect domain purity from external models, SDKs, and frameworks.
ArchUnit tests in CI enforce these boundaries and fail the build on violations.

### III. Test-First with AI Evaluation

Acceptance criteria MUST be written before implementation. Unit and integration
tests follow the Red-Green-Refactor cycle. AI-generated outputs MUST pass the
eval harness (relevance, faithfulness, task success) before reaching users.
All eval thresholds are versioned in `specs/evals/` and gated in CI.

### IV. Observability by Default

Every service MUST emit structured JSON logs (daily-rotated, 30d on-disk,
90d archive), distributed traces, and RED/USE metrics. Correlation/trace IDs
propagate across all service boundaries. Logging configuration is part of the
service definition — not an afterthought. Any production issue without
reproducible traces is considered a gap in observability.

### V. Simplicity & YAGNI

Start with the simplest design that satisfies the current spec. Every dependency
MUST justify its inclusion. Avoid premature abstraction, over-engineering, or
building for hypothetical future requirements. Complexity must be documented and
approved when unavoidable (see Complexity Tracking in `plan.md`).

## Technology & Architecture Constraints

- **Backend**: Java 21, Spring Boot 3.x, Spring Cloud, Spring Data JPA / Hibernate
- **Frontend**: Vue 3 (Vite + Pinia + Vue Router), responsive PWA
- **AI**: Python 3.12, LangChain/LlamaIndex, Spring AI (orchestration layer)
- **Database**: PostgreSQL + pgvector (operational + vector store)
- **Messaging**: Azure Service Bus / RabbitMQ via Spring Cloud Stream + transactional outbox
- **Infrastructure**: Docker, Kubernetes (AKS), Terraform, GitHub Actions
- **Multi-tenancy**: Row-level security + tenant key on every table
- **API**: REST (versioned, OpenAPI-documented) + internal gRPC + MCP for agent tools
- **Security**: OIDC/OAuth2, RBAC/ABAC, AES-256 at rest, TLS 1.2+ in transit

## Development Workflow & Quality Gates

1. **Specify** — Capture intent in `spec.md` with acceptance criteria.
2. **Review Gate** — Approve or reject the spec before planning.
3. **Plan** — Generate `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`.
4. **Review Gate** — Approve or reject the plan before task generation.
5. **Tasks** — Decompose into `tasks.md` with traceability IDs.
6. **Implement** — Execute tasks phase by phase; TDD cycle within each story.
7. **CI Validation** — Build → test → AI evals → SAST/SCA → container sign → deploy.
8. **Constitution Check** — Every PR MUST pass the Constitution Check gate in
   `plan.md`. Violations require explicit justification in Complexity Tracking.

## Governance

This Constitution supersedes all other development practices. Amendments
require documented rationale, team approval, and a migration plan.

- **Versioning**: MAJOR bump for backward-incompatible principle
  removals/redefinitions; MINOR for new principles or materially expanded
  guidance; PATCH for clarifications and typo fixes.
- **Compliance**: Architecture reviews and PR checks MUST verify constitution
  adherence. ArchUnit tests provide automated enforcement for dependency rules.
- **Frequency**: Reviewed at project milestones or when a spec change triggers a
  principle conflict.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE) | **Last Amended**: 2026-07-01
