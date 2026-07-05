# DocSynth

Multi-tenant SaaS that ingests OpenAPI 3.x specs, generates versioned human-readable
API reference docs, detects structural drift, exposes an interactive "Try It" API
console, and publishes immutable versioned doc sets at stable URLs.

## Status

Implementation scaffold only. See `specs/001-docsynth-core/` for the authoritative
feature specification, plan, research, data model, contracts, and tasks.

- [spec.md](specs/001-docsynth-core/spec.md) — what we're building
- [plan.md](specs/001-docsynth-core/plan.md) — how we're building it
- [research.md](specs/001-docsynth-core/research.md) — key architectural decisions
- [data-model.md](specs/001-docsynth-core/data-model.md) — entities and relationships
- [contracts/](specs/001-docsynth-core/contracts/) — gRPC + REST contracts
- [tasks.md](specs/001-docsynth-core/tasks.md) — implementation task list

## Repository Layout

```
backend/      Java 21 / Spring Boot 3.x (orchestration plane)
ai-sidecar/   Python 3.12 (model plane; LangChain + LlamaIndex)
frontend/     Vue 3 + Vite (operator SPA + docs SSG)
contracts/    Cross-language gRPC proto + OpenAPI specs
infra/        Terraform (Azure: AKS, Postgres, Service Bus, Key Vault, Blob, Front Door)
specs/        Feature specifications
docs/         ADRs, security notes, metrics
```

## Prerequisites

- Java 21 (SDKMAN: `sdk use java 21.x`)
- Python 3.12
- Node 20+ / pnpm
- Terraform >= 1.9
- Docker (for local Postgres + Service Bus emulator)

## Constitution

See `.specify/memory/constitution.md` for binding principles (Spec-Driven
Development, Clean Architecture, Test-First + AI Evaluation, Observability by
Default, Simplicity & YAGNI).
# DocSynth
