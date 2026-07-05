package com.docsynth.infrastructure.grpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC client configuration for the Java backend → Python AI sidecar.
 *
 * Outbound gRPC client lives in infrastructure/grpc/ (consistent with
 * A1 remediation: gRPC clients in infrastructure, gRPC servers in interfaces).
 * Tenant-assertion JWT is attached to every call via a ClientInterceptor
 * (AiSidecarClient).
 */
@Configuration
public class AiSidecarClientConfig {

    @Value("${docsynth.ai-sidecar.host:localhost}")
    private String host;

    @Value("${docsynth.ai-sidecar.port:50051}")
    private int port;

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel aiSidecarChannel() {
        return NettyChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
    }
}
