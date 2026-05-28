package br.com.nitra.ai.service;

import br.com.nitra.ai.config.properties.ModelProperties;
import br.com.nitra.ai.utils.ModelsEnum;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ImageBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ImageFormat;
import software.amazon.awssdk.services.bedrockruntime.model.ImageSource;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

import java.util.Base64;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class BedrockLlmService {

    private static final int MAX_TOKENS = 1024;

    @Inject
    BedrockRuntimeClient client;

    @Inject
    ModelProperties modelProperties;

    public String invokeLlmModel(String userMessage, String model) {
        String modelId = resolveModelId(model);
        ConverseRequest request = buildRequest(userMessage, modelId);
        return invoke(request, modelId, model);
    }

    public String invokeLlmModelWithImage(String userMessage, String imageBase64, String imageFormat, String model) {
        ModelsEnum selectedModel = resolveModel(model);
        validateImageSupport(selectedModel);

        String modelId = resolveModelId(selectedModel);
        ConverseRequest request = buildImageRequest(userMessage, imageBase64, imageFormat, modelId);
        return invoke(request, modelId, model);
    }

    private String invoke(ConverseRequest request, String modelId, String model) {
        try {
            ConverseResponse response = client.converse(request);
            return extractText(response);
        } catch (ValidationException exception) {
            throw new IllegalArgumentException("Model ID inválido para Bedrock: " + modelId + " (model=" + model + ")", exception);
        }
    }

    private ConverseRequest buildRequest(String userMessage, String modelId) {
        return ConverseRequest.builder()
                .modelId(modelId)
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText(userMessage))
                        .build())
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(MAX_TOKENS)
                        .build())
                .build();
    }

    private ConverseRequest buildImageRequest(String userMessage, String imageBase64, String imageFormat, String modelId) {
        return ConverseRequest.builder()
                .modelId(modelId)
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(
                                ContentBlock.fromText(userMessage),
                                ContentBlock.fromImage(ImageBlock.builder()
                                        .format(parseImageFormat(imageFormat))
                                        .source(ImageSource.fromBytes(SdkBytes.fromByteArray(decodeBase64Image(imageBase64))))
                                        .build())
                        )
                        .build())
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(MAX_TOKENS)
                        .build())
                .build();
    }

    private String extractText(ConverseResponse response) {
        if (response.output() == null || response.output().message() == null || !response.output().message().hasContent()) {
            throw new IllegalStateException("Resposta do Bedrock sem conteúdo textual.");
        }

        String text = response.output().message().content().stream()
                .map(ContentBlock::text)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .reduce((left, right) -> left + "\n" + right)
                .orElseThrow(() -> new IllegalStateException("Resposta do Bedrock não contém blocos de texto."));

        return text;
    }

    private String resolveModelId(String model) {
        return resolveModelId(resolveModel(model));
    }

    private String resolveModelId(ModelsEnum selectedModel) {
        final Map<ModelsEnum, String> modelsMap = createModelsMap();
        final String modelId = modelsMap.get(selectedModel);
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalStateException("Model ID não configurado para " + selectedModel + ".");
        }

        return modelId;
    }

    private ModelsEnum resolveModel(String model) {
        final String normalizedModel = model == null || model.isBlank()
                ? ModelsEnum.ANTHROPIC.name()
                : model.trim().toUpperCase(Locale.ROOT);

        try {
            return ModelsEnum.valueOf(normalizedModel);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Modelo inválido: " + model + ". Use um destes: " + ModelsEnum.valuesAsText());
        }
    }

    private Map<ModelsEnum, String> createModelsMap() {
        final var enumsMap = new EnumMap<ModelsEnum, String>(ModelsEnum.class);
        enumsMap.put(ModelsEnum.GPT, modelProperties.getGptId());
        enumsMap.put(ModelsEnum.ANTHROPIC, modelProperties.getAnthropicId());
        enumsMap.put(ModelsEnum.MAGISTRAL, modelProperties.getMagistralId());
        enumsMap.put(ModelsEnum.AMAZON_LITE, modelProperties.getAmazonId());
        return enumsMap;
    }

    private void validateImageSupport(ModelsEnum selectedModel) {
        if (selectedModel != ModelsEnum.ANTHROPIC && selectedModel != ModelsEnum.AMAZON_LITE) {
            throw new IllegalArgumentException("O modelo " + selectedModel + " não suporta análise de imagem neste endpoint. Use ANTHROPIC ou AMAZON_LITE.");
        }
    }

    private byte[] decodeBase64Image(String imageBase64) {
        try {
            String normalized = imageBase64.contains(",")
                    ? imageBase64.substring(imageBase64.indexOf(',') + 1)
                    : imageBase64;
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Imagem em base64 inválida.", exception);
        }
    }

    private ImageFormat parseImageFormat(String imageFormat) {
        try {
            return ImageFormat.fromValue(imageFormat.trim().toLowerCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Formato de imagem inválido: " + imageFormat + ". Use PNG, JPEG, GIF ou WEBP.");
        }
    }

}
