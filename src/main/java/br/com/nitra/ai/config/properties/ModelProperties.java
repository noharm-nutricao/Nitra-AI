package br.com.nitra.ai.config.properties;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ModelProperties {

    @ConfigProperty(name = "app.bedrock.anthropic-id")
    String anthropicId;

    @ConfigProperty(name = "app.bedrock.gpt-id")
    String gptId;

    @ConfigProperty(name = "app.bedrock.amazon-id")
    String amazonId;

    @ConfigProperty(name = "app.bedrock.magistral.small-id")
    String magistralId;

    public String getAnthropicId() {
        return anthropicId;
    }

    public String getGptId() {
        return gptId;
    }

    public String getAmazonId() {
        return amazonId;
    }

    public String getMagistralId() {
        return magistralId;
    }
}
