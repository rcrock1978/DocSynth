package com.docsynth.domain.ingestion;

/** Port for parsing an OpenAPI spec text into structured form. */
public interface ParseSpecPort {
    /** @return a structured representation of the spec, including endpoint and schema counts. */
    ParsedSpec parse(String specText, SpecSource source);
}
