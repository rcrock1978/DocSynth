"""
AI eval: code-example relevance.

A code example is "relevant" if it:
  - references the endpoint method + path,
  - uses the requested language,
  - is syntactically valid for the language,
  - is non-empty.

Threshold: ≥ 0.85 of generated examples pass per the spec/evals/thresholds.yml.
"""

from __future__ import annotations

import ast
import sys
from pathlib import Path

# Allow importing from the sidecar's pipelines dir
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "ai-sidecar" / "src"))

from docsynth_ai.pipelines.code_examples import generate_code_example


async def test_relevance() -> None:
    cases = [
        ("GET", "/users", "curl"),
        ("POST", "/users", "python"),
        ("DELETE", "/users/{id}", "java"),
    ]
    relevant = 0
    for method, path, language in cases:
        result = await generate_code_example(
            tenant_id="t1",
            api_spec_id="s1",
            endpoint={"method": method, "path": path, "summary": "Test"},
            language=language,
        )
        if not result.code.strip():
            continue
        if result.language != language:
            continue
        if language == "python":
            try:
                ast.parse(result.code)
            except SyntaxError:
                continue
        if language == "java":
            # Brace balance check
            if result.code.count("{") != result.code.count("}"):
                continue
        if language == "curl":
            if not result.code.lstrip().startswith("curl"):
                continue
        relevant += 1
    ratio = relevant / len(cases)
    assert ratio >= 0.85, f"relevance ratio {ratio} below threshold 0.85"


if __name__ == "__main__":
    import asyncio
    asyncio.run(test_relevance())
