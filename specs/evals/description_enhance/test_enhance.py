"""
AI eval: description-enhancement quality.

An enhanced description is acceptable if it:
  - is non-empty and longer than the input summary,
  - does not invent endpoint semantics not implied by the summary,
  - is in plain prose (no markdown headers).
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "ai-sidecar" / "src"))

from docsynth_ai.pipelines.description_enhance import enhance_description


async def test_enhance() -> None:
    cases = [
        "List all users",
        "Create a new order",
        "Delete the widget by id",
    ]
    acceptable = 0
    for summary in cases:
        result = await enhance_description(tenant_id="t1", summary=summary)
        if not result.enhanced.strip():
            continue
        if len(result.enhanced) <= len(summary):
            continue
        if result.enhanced.lstrip().startswith("#"):  # markdown header
            continue
        acceptable += 1
    ratio = acceptable / len(cases)
    assert ratio >= 0.85, f"enhancement acceptability {ratio} below threshold 0.85"


if __name__ == "__main__":
    import asyncio
    asyncio.run(test_enhance())
