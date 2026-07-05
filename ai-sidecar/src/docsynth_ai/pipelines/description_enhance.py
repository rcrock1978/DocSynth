"""
LangChain-based description enhancer.

Stub v1 implementation: returns a minimal expansion of the summary.
Real implementation loads prompts/description_enhance_v1.yaml and calls
the LLM.
"""

from __future__ import annotations

import yaml
from dataclasses import dataclass
from pathlib import Path


@dataclass
class EnhancedDescription:
    enhanced: str
    prompt_template_version: str
    confidence: float


_PROMPT_PATH = Path(__file__).resolve().parents[1] / "prompts" / "description_enhance_v1.yaml"


def _load_prompt() -> dict:
    with open(_PROMPT_PATH) as f:
        return yaml.safe_load(f)


async def enhance_description(tenant_id: str, summary: str) -> EnhancedDescription:
    prompt = _load_prompt()
    if not summary:
        enhanced = "No description provided."
    else:
        # v1 stub: append a period and capitalise. Real LLM call later.
        enhanced = f"{summary.rstrip('.')}."
    return EnhancedDescription(
        enhanced=enhanced,
        prompt_template_version=prompt.get("template_version", "v1.0"),
        confidence=0.7,
    )
