package com.docsynth.domain.documentation;

public record CodeExample(String language, String code, String promptTemplateVersion, double confidence) {}
