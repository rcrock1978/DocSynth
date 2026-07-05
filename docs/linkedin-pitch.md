# DocSynth — 120-Second LinkedIn Pitch

---

**(0:00–0:08) — The hook**

Every API team has the same problem: the documentation is always wrong. It's stale on arrival, drift is invisible, and the only signal that something broke is a customer support ticket.

**(0:08–0:25) — The insight**

We built DocSynth because we finally could. OpenAPI is the standard, LLMs can generate code examples in any language, and static-site generation lets us publish a separate, immutable doc site for every spec version.

DocSynth ingests your OpenAPI 3.x spec — from a URL, a file, or a GitHub repo — and within 30 seconds, you have a browsable reference site with working code examples. Push a change, and within two minutes, DocSynth compares the live spec against the last published docs, classifies what's added, removed, and changed, and tells you which version of your API just broke compatibility. Every endpoint has a "Try It" button that proxies a real request through a tenant-scoped, SSRF-hardened security perimeter — so your API consumers can test the live API without leaving the docs.

**(0:25–0:50) — The architecture, in one breath**

Under the hood: Java 21 with Spring Boot runs the orchestration plane — multi-tenant data model, row-level security, transactional outbox, OpenTelemetry traces. Python 3.12 is the model plane — a gRPC sidecar that runs LangChain and LlamaIndex for code generation. Vue 3 is built twice from one codebase: once as a static site for the published docs, and once as the dynamic operator console. PostgreSQL with pgvector carries the operational data and the embedding store. Azure Service Bus carries the async work. Front Door is the CDN.

Every line of code maps back to a spec entry. Every architectural decision is documented in the research file. Every commit passes the constitution check: clean architecture, observability by default, test-first, AI-evaluation-gated. The AI eval harness blocks the build if code-example relevance drops below 85% or faithfulness below 90%.

**(0:50–1:15) — The differentiators**

Three things set DocSynth apart from SwaggerHub, ReadMe, and Mintlify. **One:** the system is spec-driven end-to-end. The spec is the source of truth; the docs, the code examples, the drift report, the lifecycle state — all of it is derived. **Two:** the architecture is multi-tenant SaaS by design, not by retrofit — row-level security at the database layer, OIDC + RBAC, tenant isolation enforced at every boundary. **Three:** the interactive console is a real, working proxy with an SSRF guard, a per-tenant allowlist, and an audit log — not a static example. We don't tell you what the API would return; we actually call it.

And the cost: each user story is an independently deliverable vertical slice. MVP — submit a spec, see it parsed — is twelve tasks in the Setup phase plus thirty-one in Foundational plus twenty for the first user story. Three weeks of work for a working product.

**(1:15–1:55) — The ask**

We're shipping DocSynth to the developer-tools market. API teams that publish to enterprise developers. Platform teams that maintain internal APIs. SaaS companies whose docs are part of the product.

If you're building or maintaining an API and you recognize the pain — if you've ever been on a call where someone says "the docs are wrong" — talk to us. DocSynth is open for design partners; the first ten teams get white-glove onboarding, custom domain hosting, and a direct line to the engineering team.

**(1:55–2:00) — The closer**

DocSynth. AI-powered API documentation that stays in sync with your code. Thank you.

---

## Hashtags

`#API` `#Documentation` `#OpenAPI` `#DeveloperTools` `#DevTools` `#SaaS` `#SpringBoot` `#Python` `#VueJS` `#LLM` `#RAG` `#MultiTenancy` `#APIDocumentation` `#PlatformEngineering` `#DeveloperExperience` `#DevRel`
