package com.docsynth.infrastructure.documentation;

import com.docsynth.domain.ingestion.EndpointDescriptor;
import com.docsynth.infrastructure.grpc.AiSidecarClient;
import com.docsynth.proto.documentation.v1.DocGeneratorGrpc;
import com.docsynth.proto.documentation.v1.DocGeneratorProto;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * CodeExampleClient — gRPC adapter for the AI sidecar's DocGenerator.
 */
@Component
public class CodeExampleClient {

    private final DocGeneratorGrpc.DocGeneratorBlockingStub stub;

    public CodeExampleClient(@GrpcClient("ai-sidecar") DocGeneratorGrpc.DocGeneratorBlockingStub stub) {
        this.stub = stub;
    }

    public DocGeneratorProto.GenerateCodeExampleResponse generate(
        String tenantId, String apiSpecId, EndpointDescriptor endpoint, String language
    ) {
        DocGeneratorProto.GenerateCodeExampleRequest req = DocGeneratorProto.GenerateCodeExampleRequest.newBuilder()
            .setTenantId(tenantId)
            .setApiSpecId(apiSpecId)
            .setMethod(endpoint.method())
            .setPath(endpoint.path())
            .setSummary(endpoint.summary() != null ? endpoint.summary() : "")
            .setLanguage(language)
            .build();
        return stub.generateCodeExample(req);
    }
}
