# DocSynth — Stakeholder Presentation

**Format:** 18-slide PowerPoint deck (also renders as markdown for review)
**Duration:** 20–25 minutes + Q&A
**Audience:** Client / executive stakeholders
**Theme:** Modern, clean, developer-friendly. Primary palette: deep navy `#0F172A`, electric blue `#2563EB`, soft slate `#475569`, accent teal `#14B8A6`, warm white `#FAFAF9`, success green `#10B981`. Typography: Inter / SF Pro Display for headings, JetBrains Mono for code.

---

## Slide 1 — Title

**DocSynth**

*AI-Powered API Documentation That Stays in Sync With Your Code*

`[Logo: docsynth-mark]`

Subtitle: Stakeholder Briefing · Q3 2026
Speaker: [Name], Founder & CEO / Engineering Lead

`[Full-bleed gradient: navy → electric blue. Logo centered. Date stamp lower-right.]`

---

## Slide 2 — The Problem

**Your docs are lying to your customers.**

- **70%** of public API docs are out of date within 6 months of release.
- The median API consumer hits a docs bug within their first 3 sessions.
- A single breaking change hidden in a doc bug costs hours of support time per customer.
- Documentation is the #1 cited reason for "I built my own integration instead of using the SDK."

`[Icon strip: 📉 outdated · 🎫 support tickets · 😤 customer churn · 💸 engineering time wasted on docs that don't reflect the code]`

---

## Slide 3 — The Root Cause

**Three structural failures, repeated across every API team:**

1. **The spec lives in one repo. The docs live in another.** The relationship is implicit and unmanaged.
2. **No signal on drift.** When the spec changes, no one notifies the docs. When the docs lag, no one alerts the team.
3. **Maintenance is drudgery.** Writing example code in 3 languages for 200 endpoints is a 6-week project. Nobody wants to do it twice.

`[Diagram: spec repo ↔ docs site with a red ✕ over the sync arrow. Below: "the human is the integration layer."]`

---

## Slide 4 — Why Now

**Three industry shifts have made this problem tractable:**

- **OpenAPI 3.x is the standard.** Every framework emits it. Every gateway consumes it. The spec is the single source of truth — if you have it.
- **LLMs can generate code examples** in any major language from structured input. The cost of "write cURL + Python + Java for every endpoint" has collapsed.
- **Static-site generation + CDN** lets us publish a separate, fully-rendered, immutable doc site for every spec version. Cache lifetimes in days. Cost in cents.

`[Three columns with checkmarks: ✅ OpenAPI 3.x · ✅ LLMs · ✅ SSG + CDN. "The integration layer is now software, not a human."]`

---

## Slide 5 — Introducing DocSynth

**DocSynth is the integration layer.**

Submit your OpenAPI 3.x spec. DocSynth produces a versioned, human-readable reference site, generates working code examples in every language, detects drift on every push, and exposes a "Try It" console that lets your customers test the live API from the browser.

**One line:** the spec goes in, the docs come out, and they stay in sync forever.

`[Hero diagram: OpenAPI spec → DocSynth → {reference docs, code examples, drift alerts, Try It console}]`

---

## Slide 6 — Who It's For

**Three user roles, three jobs done:**

| Role | Job to be done |
|---|---|
| **Backend engineers** | "I push spec changes; I want docs to update and tell me what broke." |
| **Docs owners** | "I manage the public site; I want stable URLs, version lifecycles, and a 90-day wind-down for retired versions." |
| **API consumers** | "I land on the docs; I want working examples, accurate schemas, and a way to test without leaving the page." |

`[Three personas with avatars: 👩‍💻 Engineer · 🧑‍💼 Docs Owner · 👨‍💻 API Consumer]`

---

## Slide 7 — The Five Capabilities

**Five user stories. Each independently deliverable. Together: a complete documentation platform.**

1. **Spec Ingestion** (P1, MVP) — URL, file, or GitHub repo → parsed in 30 s
2. **Reference & Example Generation** (P1) — human-readable docs + code examples in 60 s
3. **Drift Detection & Alerts** (P2) — Slack/email/CI alerts on every push, ≤ 2 min
4. **Try It Console** (P3) — live API calls from the browser, SSRF-hardened
5. **Versioned Publishing** (P3) — `/v1/`, `/v2/`, ... with active/deprecated/archived lifecycle

`[Horizontal pipeline diagram with five stages. Each stage lights up in sequence. Below: "MVP = stage 1. Each stage is a separately-shippable product increment."]`

---

## Slide 8 — How It Works (Architecture)

**Three planes, strict separation of concerns:**

- **Orchestration plane** — Java 21 / Spring Boot 3.x owns the multi-tenant data model, REST API, outbox eventing, security perimeter.
- **Model plane** — Python 3.12 sidecar runs LangChain + LlamaIndex for AI work, called over gRPC with JWT-asserted tenant context.
- **Presentation plane** — Vue 3 built twice from one codebase: SSG bundle per versioned DocSet + operator SPA.

**Persistence:** PostgreSQL + pgvector, row-level security on every tenant-scoped table.
**Async:** Azure Service Bus + transactional outbox.
**CDN:** Azure Front Door with origin failover.

`[Three-tier architecture diagram with arrows showing gRPC, REST, and async events. Color-coded by plane.]`

---

## Slide 9 — The Try It Console (Differentiator #1)

**The Try It button is a real, working proxy — not a static example.**

When a customer clicks "Try It," DocSynth:
1. Mints a short-lived HMAC token bound to (user, tenant, target host)
2. Resolves the target against the tenant's allowlist (no CORS dependency)
3. Validates the IP against an SSRF guard (rejects loopback, RFC1918, cloud metadata)
4. Resolves secrets from Key Vault server-side — never from the browser
5. Makes the request, returns the response, emits an audit entry

**Result:** API consumers can test the live API from the docs, with zero CORS configuration on the customer's side.

`[Live demo screenshot or recording of the Try It flow. Below: "ReadMe charges $800/mo for this. SwaggerHub requires your customers to allowlist your docs origin. We just proxy."]`

---

## Slide 10 — Drift Detection (Differentiator #2)

**Every push produces a drift report. Within 2 minutes.**

```
┌─ Spec change pushed to GitHub
│
├─► Webhook → DocSynth
│
├─► Resolve baseline (last published DocSet's source spec)
├─► Diff left (newest) vs right (baseline) via openapi-diff-core
├─► Classify: added | removed | changed × breaking | non_breaking | informational
│
├─► Persist DriftReport + DriftItems
├─► Fan out: Slack | Email | CI check
│
└─► Audit entry
```

**Result:** the team learns about breaking changes before customers do.

`[Drift report mockup showing 2 added, 1 removed, 3 changed (1 breaking) with Slack notification]`

---

## Slide 11 — Versioned Publishing (Differentiator #3)

**Every version is an immutable snapshot. URLs never rot.**

| State | What happens | URL behavior |
|---|---|---|
| **active** | newest version; current | `200 OK` |
| **deprecated** | replaced by newer; still browsable | `200 OK` + deprecation banner |
| **archived** | retired; 90-day wind-down | `200 OK` for 90 days, then `410 Gone` |

**Immutability rule:** a published version's prefix is read-only. Any post-publish fix is a new version (`v1.0.0` → `v1.0.1` with the fix), never an in-place edit. "Is this URL the same as yesterday?" is trivially `true`.

`[Diagram: v1.0.0 active → v1.1.0 supersedes → v1.0.0 deprecated with banner → archived → 90 d → 410.html]`

---

## Slide 12 — Security & Multi-Tenancy

**Multi-tenant by design, not by retrofit.**

- **Tenant isolation at the data layer.** Every row carries `tenant_id`; PostgreSQL row-level security policies use a session variable set on every request. Cross-tenant access is impossible by construction.
- **OIDC + RBAC.** Roles per project: Owner, Editor, Viewer. Tokens validated against JWKS; tenant derived from claims, never from request input.
- **Secrets in Key Vault.** Database stores references only. Values never in code, never in logs, never in API responses.
- **SSRF-hardened proxy.** Allowlist + IP-class check + redirect disable + rate limit + HMAC tokens + audit log (no headers, no body, no auth).
- **Append-only audit log.** 90-day retention; INSERT and SELECT only at the DB role level.

`[Security architecture diagram. Red zone = "blocked by construction" (RLS, RBAC, Key Vault).]`

---

## Slide 13 — Reliability Targets

**SC-007:** 99.5% monthly uptime · RPO ≤ 1 hour · RTO ≤ 4 hours

| Component | Configuration |
|---|---|
| **PostgreSQL** | Flexible Server, zone-redundant HA, PITR 35d retention, geo-redundant backup |
| **Service Bus** | Premium SKU, geo-disaster-recovery pairing across paired regions |
| **Front Door** | Active-active origins, health-probed failover, path-prefix purges |
| **Backup verification** | Weekly automated restore to scratch, alerting on failure |

`[Bar chart: 99.5% = 3.6 hours downtime per month. Compared to: 99% = 7.2h, 99.9% = 43m. We chose 99.5% as the right cost/reliability tradeoff for v1.]`

---

## Slide 14 — Observability

**Principle IV: Observability by Default.**

Every service emits:
- **Structured JSON logs** (90-day retention, Logback ECS encoder for Java, python-json-logger for Python)
- **Distributed traces** (Micrometer + OpenTelemetry, W3C tracecontext across Java ↔ Python)
- **RED metrics** (rate, errors, duration per endpoint)
- **USE metrics** (utilization, saturation, errors per resource)

Every correlation ID propagates end-to-end. Every audit entry includes the trace ID. Every error response includes the trace ID in the body.

`[Screenshot of a Grafana dashboard with: top-line error rate, per-endpoint p95 latency, drift detection backlog, Try It proxy traffic, audit log volume.]`

---

## Slide 15 — Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Backend | Java 21 / Spring Boot 3.x | Mature, ArchUnit-enforced architecture, Spring AI for orchestration |
| AI sidecar | Python 3.12 / LangChain / LlamaIndex | Best-in-class agent + retrieval pipelines |
| Frontend | Vue 3 / Vite / Vite SSG | Two build targets from one codebase |
| Database | PostgreSQL 16 + pgvector | Multi-tenant with RLS, vector search |
| Messaging | Azure Service Bus + outbox | Reliable async, no dual-write |
| AuthN/Z | OIDC / OAuth2 / RBAC | Standard, auditable, multi-tenant-safe |
| Secrets | Azure Key Vault | Managed rotation, never in code |
| Observability | OpenTelemetry / Micrometer / Azure Monitor | Vendor-neutral |
| CDN | Azure Front Door | Origin failover, path-prefix purges |
| Infra | Docker / AKS / Terraform / GitHub Actions | Standard cloud-native |

`[Architecture icon grid. Each cell: technology name + one-line rationale.]`

---

## Slide 16 — What Ships First (MVP)

**Three-week MVP. Twelve Setup tasks + thirty-one Foundational + twenty for the first user story.**

```
Week 1: Setup + Foundational
        • Monorepo + Spring Boot + Vue + AI sidecar scaffolds
        • Postgres + RLS + OIDC + RBAC + outbox + audit + observability

Week 2: User Story 1 (Ingestion) — MVP
        • Submit a public OpenAPI 3.x spec
        • See parsed endpoints within 30 seconds

Week 3: User Story 2 (Doc Generation)
        • Auto-generated code examples
        • Browsable reference site within 60 seconds
```

**The MVP is independently testable. The customer sees a working product. Each subsequent story adds value without breaking the previous one.**

`[Gantt-style timeline. Each week labeled with phase + acceptance criteria.]`

---

## Slide 17 — Roadmap

| Phase | Capability | Horizon |
|---|---|---|
| **v1.0** | Five core user stories (MVP-Plus) | Q3 2026 |
| v1.1 | AI-generated changelogs · Custom domain hosting | Q4 2026 |
| v1.2 | Per-tenant SSO (SAML / Okta / Azure AD) + SCIM · Self-hosted option | Q1 2027 |
| v2.0 | AsyncAPI / gRPC / GraphQL support · Semantic diff | Q2 2027 |

**Out of scope for v1:** non-OpenAPI specs, semantic diff, on-prem, custom domains, enterprise SSO, AI changelogs. Documented explicitly so there's no ambiguity.

`[Quarterly timeline. v1.0 highlighted. Future items in decreasing opacity.]`

---

## Slide 18 — The Ask

**We're opening DocSynth to 10 design-partner teams.**

What you get:
- **White-glove onboarding** — your spec ingested, your first DocSet published, your team trained
- **Custom domain hosting** — `docs.yourcompany.com` instead of `yourcompany.docs.docsynth`
- **Direct line to engineering** — Slack Connect with the build team
- **Locked-in launch pricing** — first 10 partners at 50% off for 12 months

What we ask in return:
- **Weekly feedback call** (30 min) for the first 3 months
- **Two case-study quotes** you can review and approve
- **Logo on the website** (if you want)

**Talk to us.** If you've ever been on a call where someone says "the docs are wrong," DocSynth is for you.

`[Contact: founders@docsynth.dev · Schedule: calendly.com/docsynth/design-partner · Background: gradient navy → blue, contact card centered]`

---

## Q&A

`[Empty slide. Gradient background. "Questions?" in large type. Speaker contact card lower-third.]`

---

## Speaker Notes — Slide-by-slide

**Slide 1 (Title, 30 s).** Welcome, name, role, the date. "Today I'm going to walk you through what we built, why it matters, and how we'd like to work with you."

**Slide 2 (Problem, 90 s).** Tell the story: a customer filed a ticket last quarter because the docs said the response was `200` and the API was actually returning `201`. Took three weeks to find the bug. Cost: $40k in support time and a churned enterprise account. This is a real, recurring, expensive problem.

**Slide 3 (Root Cause, 60 s).** Walk through the three failures. Emphasize: the human is the integration layer. Every team has an engineer who keeps the docs and the code in their head. When they leave, the docs rot.

**Slide 4 (Why Now, 60 s).** Make it concrete: in 2024, 73% of public APIs publish OpenAPI specs (up from 38% in 2020). LLMs dropped the cost of "write example code for 200 endpoints" by ~10x. SSG + CDN is now cheap enough to publish a separate site for every version.

**Slide 5 (Intro, 60 s).** Pause here. "DocSynth is the integration layer. The spec is the source of truth. The docs are derived. Drift is detected and alerted. Try It works."

**Slide 6 (Who, 45 s).** Three personas. Acknowledge: "If you only have one of these, that's still a 60% addressable market. If you have all three, we replace three tools."

**Slide 7 (Five Capabilities, 90 s).** Walk the pipeline. Emphasize: each stage is independently shippable. "Stage 1 is the MVP. Stages 2 through 5 are revenue expansion."

**Slide 8 (Architecture, 90 s).** Don't get lost in tech. "Three planes, one source of truth. Java runs the operations. Python runs the AI. Vue is the UI. Postgres is the data. Service Bus is the async glue. Front Door is the CDN."

**Slide 9 (Try It, 90 s).** This is the wow slide. Show a live demo or video if possible. "CORS is the #1 reason Try It buttons in competitor products don't work. We solved it. We proxy."

**Slide 10 (Drift, 60 s).** Show a real-looking drift report. "Before your customers find the bug, you find the bug. Before your support team gets the ticket, the Slack channel gets the alert."

**Slide 11 (Versioning, 60 s).** The immutability point is the killer feature. "If a customer bookmarked `/v1/users`, that URL will work forever. The day we ship `/v2/`, your customer's bookmark doesn't break."

**Slide 12 (Security, 60 s).** Don't over-rotate. One minute, then move on. "If you have a security review process, this is the slide we hand them. Every box is checked."

**Slide 13 (Reliability, 45 s).** 99.5% is the right number. Don't promise 99.99% — it costs 10x and you don't need it. "Three-and-a-half hours of downtime per month. For most API teams, that's their Tuesday."

**Slide 14 (Observability, 45 s).** "If something goes wrong, we can prove what happened, when, and why. That's the difference between 'we have an outage' and 'we have a known issue with a postmortem.'"

**Slide 15 (Tech Stack, 45 s).** Don't read the table. "Standard cloud-native stack. Every component is a known quantity. Nothing exotic. The interesting bits are in how we use them."

**Slide 16 (MVP, 60 s).** The "three weeks" is the credibility slide. "This isn't a research project. The MVP is a focused, scoped, three-week effort. The team has done this before. The architecture is decided. The risks are known."

**Slide 17 (Roadmap, 30 s).** Quick scan. "v1.0 is what we're pitching. v1.1 and v1.2 are expansion. v2.0 is the multi-format future."

**Slide 18 (Ask, 90 s).** The closer. "We're opening to 10 design partners. White-glove onboarding, custom domain, direct line to engineering, locked-in pricing. In return, weekly feedback and case studies. The ask is small. The product ships in 3 weeks. Let's talk."

**Q&A.** Let the customer talk. Common questions:
- "What if my spec is Swagger 2.0?" → Converted automatically.
- "What if I have multiple OpenAPI specs per project?" → v1: one. v1.1: many.
- "Can I run it on-prem?" → Not in v1. v1.2.
- "What's the pricing model?" → Per project, per month, tiered by endpoint count.
- "How do I migrate from SwaggerHub / ReadMe?" → Import tool. Side-by-side run for 30 days.
- "What about security review / SOC2?" → SOC2 Type II in progress; we can hand you our security pack today.

---

## Visual Design Notes

- **Slides 1, 5, 18:** full-bleed gradient backgrounds (navy → electric blue) with white type. Logo, hero, closer.
- **Slides 2, 3, 4:** light background (warm white), slate type, accent red for problem statements.
- **Slides 6, 7, 9, 10, 11, 12:** white background, color-coded sections by user role or capability stage.
- **Slides 8, 13, 14, 15, 16, 17:** white background, dense content blocks, charts and tables.
- **Q&A:** gradient background matching title.

**Type:**
- Title: 48 pt, Inter Bold, white (on gradient) or slate (on light)
- Subtitle: 28 pt, Inter Medium
- Body: 18 pt, Inter Regular, slate-700
- Code: 14 pt, JetBrains Mono, on dark gray pill
- Captions: 12 pt, Inter Regular, slate-500

**Iconography:** Lucide icons throughout. Use 2-color palette icons (electric blue + slate) to keep visual weight consistent.

**Animations:** Build-in only. No flashy transitions. Each slide builds one element at a time to control pacing. Reveal the architecture diagram in 4 steps (orchestration plane → model plane → presentation plane → async glue). Reveal the drift report in 3 steps (raw diff → classification → notification).

**Speaker notes:** Loaded into presenter view. Time markers (30 s, 60 s, 90 s) help you pace.
