"""
LangChain-based code example generator.

Stub v1 implementation: returns a syntactically valid example for the
target language. Real implementation will load the versioned prompt
template from prompts/code_example_v1.yaml, call the LLM, parse the
output, and validate it (Python AST, Java brace balance, curl prefix).
"""

from __future__ import annotations

import yaml
from dataclasses import dataclass
from pathlib import Path


@dataclass
class GeneratedCode:
    code: str
    language: str
    prompt_template_version: str
    confidence: float


_PROMPT_PATH = Path(__file__).resolve().parents[1] / "prompts" / "code_example_v1.yaml"


def _load_prompt() -> dict:
    with open(_PROMPT_PATH) as f:
        return yaml.safe_load(f)


async def generate_code_example(
    tenant_id: str,
    api_spec_id: str,
    endpoint: dict,
    language: str,
) -> GeneratedCode:
    prompt = _load_prompt()
    method = endpoint["method"].lower()
    path = endpoint["path"]
    summary = endpoint.get("summary") or ""

    # Template instantiation; LLM call stubbed in v1.
    if language == "curl":
        code = f"curl -X {method.upper()} 'https://api.example.com{path}'"
    elif language == "python":
        code = (
            "import requests\n\n"
            f"response = requests.{method}('https://api.example.com{path}')\n"
            "print(response.json())"
        )
    elif language == "java":
        code = (
            "var client = java.net.http.HttpClient.newHttpClient();\n"
            f"var request = java.net.http.HttpRequest.newBuilder()\n"
            f"    .uri(java.net.URI.create(\"https://api.example.com{path}\"))\n"
            f"    .method(\"{method.upper()}\", java.net.http.HttpRequest.BodyPublishers.noBody())\n"
            "    .build();\n"
            "client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());"
        )
    else:
        code = f"# Code example for {method.upper()} {path} (language={language})"

    return GeneratedCode(
        code=code,
        language=language,
        prompt_template_version=prompt.get("template_version", "v1.0"),
        confidence=0.85,
    )
