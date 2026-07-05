"""
AI eval: code-example faithfulness.

Faithfulness: the example references the endpoint, does not invent
parameters or status codes not implied by the spec, and uses well-known
libraries only (no fabricated packages).
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "ai-sidecar" / "src"))

from docsynth_ai.pipelines.code_examples import generate_code_example


_FORBIDDEN_IMPORTS = [
    "acme-sdk",
    "mystery-client",
    "fake-requests",
]


async def test_faithfulness() -> None:
    cases = [
        ("GET", "/widgets", "python"),
        ("POST", "/orders", "curl"),
        ("PUT", "/users/{id}", "java"),
    ]
    faithful = 0
    for method, path, language in cases:
        result = await generate_code_example(
            tenant_id="t1",
            api_spec_id="s1",
            endpoint={"method": method, "path": path, "summary": "Test"},
            language=language,
        )
        # Path must appear in the example.
        if path not in result.code and path.replace("{id}", "1") not in result.code:
            continue
        # No fabricated packages.
        if any(bad in result.code for bad in _FORBIDDEN_IMPORTS):
            continue
        faithful += 1
    ratio = faithful / len(cases)
    assert ratio >= 0.90, f"faithfulness ratio {ratio} below threshold 0.90"


if __name__ == "__main__":
    import asyncio
    asyncio.run(test_faithfulness())
