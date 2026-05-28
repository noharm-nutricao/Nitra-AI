package br.com.nitra.ai.config.bedrock;

import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@ApplicationScoped
public class BedrockClientConfig {

    public BedrockRuntimeClient client() {
        return BedrockRuntimeClient.builder()
                .region(Region.US_EAST_2)
                .build();
    }
}
