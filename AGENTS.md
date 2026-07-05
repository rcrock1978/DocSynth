<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->

# DocSynth — Speckit SDD Scaffold

This repo is a **Spec-Driven Development scaffold** (Speckit v0.11.6 + OpenCode). No application code exists yet. All development follows the speckit workflow via `/speckit.xxx` commands.

## Managed Section (DO NOT EDIT)

The `<!-- SPECKIT START -->` / `<!-- SPECKIT END -->` block above is **auto-updated** by the `agent-context` extension during `/speckit.plan`. Edit anything outside these markers freely.

## Speckit Workflow

`/speckit.specify <description>` → `/speckit.plan` → `/speckit.tasks` → `/speckit.implement`

Each step has review gates (approve/reject) in the workflow. Sub-commands:
- `/speckit.analyze` — read-only cross-artifact consistency check
- `/speckit.clarify` — resolve spec ambiguities
- `/speckit.constitution` — manage `.specify/memory/constitution.md`

## Feature Artifacts

All live under `specs/<prefix>-<short-name>/`. Numbering is sequential (3-digit) by default; set `"timestamp"` in `.specify/init-options.json` to use `YYYYMMDD-HHMMSS`.

Generated files per feature: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, `tasks.md`, `checklists/`.

## Bootstrapping

Bash setup scripts under `.specify/scripts/bash/` are sourced by speckit commands:
- `check-prerequisites.sh --json --require-tasks --include-tasks`
- `setup-plan.sh --json`
- `setup-tasks.sh --json`

These resolve `FEATURE_DIR`, `AVAILABLE_DOCS`, etc. from `.specify/feature.json` (set by `/speckit.specify`). Feature context can also be overridden via `SPECIFY_FEATURE_DIRECTORY` env var.

## Conventions

- Command separator is `.` (configured in `.specify/integration.json`)
- AGENTS.md between SPECKIT markers is managed — don't write there
- Project constitution lives at `.specify/memory/constitution.md` (template, not yet filled)
- All speckit commands support hook pre/post checks via `.specify/extensions.yml`
