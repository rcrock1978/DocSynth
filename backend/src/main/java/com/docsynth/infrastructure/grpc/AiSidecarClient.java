package com.docsynth.infrastructure.grpc;

import com.docsynth.infrastructure.security.TenantContextResolver;
import com.docsynth.infrastructure.security.TenantResolutionException;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Attaches a signed RS256 tenant-assertion JWT to every outbound gRPC call.
 * The token is minted by the Java side using its private key; the Python
 * sidecar validates the signature against the JWKS endpoint published by
 * this same service.
 */
@Component
public class AiSidecarClient implements ClientInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> TRACE_PARENT =
        Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER);

    private final TenantContextResolver tenantContext;
    private final TenantJwtMinter jwtMinter;
    private final String traceParentHeader;

    public AiSidecarClient(TenantContextResolver tenantContext, TenantJwtMinter jwtMinter,
                           @org.springframework.beans.factory.annotation.Value(
                               "${docsynth.tracing.traceparent:#{null}}") String traceParentHeader) {
        this.tenantContext = tenantContext;
        this.jwtMinter = jwtMinter;
        this.traceParentHeader = traceParentHeader;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        // Throws if no tenant context — fail-closed (FR-012).
        try {
            tenantContext.currentTenantId();
        } catch (TenantResolutionException ex) {
            throw ex;
        }
        String token = jwtMinter.mint();
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions.withDeadlineAfter(30, TimeUnit.SECONDS))) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(AUTHORIZATION, "Bearer " + token);
                if (traceParentHeader != null) {
                    headers.put(TRACE_PARENT, traceParentHeader);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
