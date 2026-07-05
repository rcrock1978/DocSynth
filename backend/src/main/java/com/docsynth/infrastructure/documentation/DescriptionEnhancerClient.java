package com.docsynth.infrastructure.documentation;

import com.docsynth.proto.documentation.v1.DocGeneratorGrpc;
import com.docsynth.proto.documentation.v1.DocGeneratorProto;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class DescriptionEnhancerClient {

    private final DocGeneratorGrpc.DocGeneratorBlockingStub stub;

    public DescriptionEnhancerClient(@GrpcClient("ai-sidecar") DocGeneratorGrpc.DocGeneratorBlockingStub stub) {
        this.stub = stub;
    }

    public DocGeneratorProto.EnhanceDescriptionResponse enhance(String tenantId, String summary) {
        DocGeneratorProto.EnhanceDescriptionRequest req = DocGeneratorProto.EnhanceDescriptionRequest.newBuilder()
            .setTenantId(tenantId)
            .setSummary(summary)
            .build();
        return stub.enhanceDescription(req);
    }
}
