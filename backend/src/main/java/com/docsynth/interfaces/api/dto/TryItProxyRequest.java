package com.docsynth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TryItProxyRequest(
    @NotBlank String proxyToken,
    @NotBlank String targetHost,
    int targetPort,
    @NotBlank String method,
    @NotBlank String path,
    String body
) {}
