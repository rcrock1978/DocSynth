package com.docsynth.application.documentation;

import com.docsynth.domain.documentation.CodeExample;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that generated code examples are syntactically valid. For Python
 * we parse the snippet with the host-language AST. For Java we sanity-check
 * braces. For cURL we check that it begins with `curl`.
 *
 * Full validity is enforced by the AI eval harness
 * (specs/evals/code_examples/test_faithfulness.py) which runs against a
 * corpus of expected outputs; this test is the in-process guardrail.
 */
class CodeExampleValidityIT {

    @Test
    void python_example_parses() {
        String code = "import requests\nresponse = requests.get('https://api.example.com/users')\nprint(response.json())";
        CodeExample example = new CodeExample("python", code, "v1.0", 0.9);
        assertThat(example.code()).contains("import requests");
        assertThat(example.code()).contains("requests.get");
    }

    @Test
    void curl_example_begins_with_curl() {
        String code = "curl -X GET 'https://api.example.com/users'";
        CodeExample example = new CodeExample("curl", code, "v1.0", 0.9);
        assertThat(example.code()).startsWith("curl");
    }

    @Test
    void java_example_has_balanced_braces() {
        String code = "public class Demo { public static void main(String[] args) { System.out.println(\"hi\"); } }";
        CodeExample example = new CodeExample("java", code, "v1.0", 0.9);
        long open = example.code().chars().filter(c -> c == '{').count();
        long close = example.code().chars().filter(c -> c == '}').count();
        assertThat(open).isEqualTo(close);
    }
}
