package br.com.nitra.ai.controller;

import br.com.nitra.ai.controller.error.ApiError;
import br.com.nitra.ai.service.BedrockLlmService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/llm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "LLM", description = "Operações para envio de prompts aos modelos configurados no Amazon Bedrock.")
@SecurityScheme(
        securitySchemeName = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class LlmResource {

    @Inject
    BedrockLlmService llmService;

    @Schema(name = "ChatRequest", description = "Payload de entrada para geração de texto.")
    public record ChatRequest(
            @NotBlank(message = "A mensagem é obrigatória.")
            @Schema(required = true, example = "Explique o que é o Amazon Bedrock.")
            String message
    ) {}

    @Schema(name = "ChatResponse", description = "Resposta textual retornada pelo modelo.")
    public record ChatResponse(
            @Schema(example = "Amazon Bedrock é um serviço gerenciado para consumo de modelos fundacionais.")
            String response
    ) {}

    @Schema(name = "ImageChatRequest", description = "Payload para análise de imagem com instrução textual.")
    public record ImageChatRequest(
            @NotBlank(message = "A mensagem é obrigatória.")
            @Schema(required = true, example = "Descreva os objetos presentes na imagem.")
            String message,
            @NotBlank(message = "A imagem em base64 é obrigatória.")
            @Schema(
                    required = true,
                    description = "Imagem em base64 puro ou em formato data URL.",
                    example = "iVBORw0KGgoAAAANSUhEUgAA..."
            )
            String imageBase64,
            @NotBlank(message = "O formato da imagem é obrigatório.")
            @Schema(required = true, example = "PNG", description = "Formatos aceitos: PNG, JPEG, GIF, WEBP.")
            String imageFormat
    ) {}

    @POST
    @Path("/chat")
    @Operation(
            summary = "Envia uma mensagem para um modelo de linguagem",
            description = "Aceita uma mensagem do usuário e encaminha para o modelo informado na query string."
    )
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Resposta gerada com sucesso.",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisição inválida.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Sem permissão para acessar o Amazon Bedrock.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "429",
                    description = "Limite de chamadas excedido no provedor.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "502",
                    description = "Falha de integração com o modelo.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "503",
                    description = "Modelo temporariamente indisponível.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "504",
                    description = "Tempo limite excedido ao aguardar resposta do modelo.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    @RolesAllowed("nitra-ai/read")
    public ChatResponse chat(
            @Valid ChatRequest request,
            @Parameter(
                    in = ParameterIn.QUERY,
                    name = "model",
                    description = "Modelo lógico a ser utilizado. Exemplos: ANTHROPIC, GPT, AMAZON_LITE, MAGISTRAL.",
                    example = "ANTHROPIC"
            )
            @QueryParam(value = "model") String model
    ) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório.");
        }

        String response = llmService.invokeLlmModel(request.message(), model);
        return new ChatResponse(response);
    }

    @POST
    @Path("/chat/image")
    @Operation(
            summary = "Envia texto com imagem para análise do modelo",
            description = "Aceita uma instrução textual junto com uma imagem em base64. Neste endpoint, use preferencialmente ANTHROPIC ou AMAZON_LITE."
    )
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Imagem analisada com sucesso.",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Requisição inválida ou modelo sem suporte a imagem.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "403",
                    description = "Sem permissão para acessar o Amazon Bedrock.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "429",
                    description = "Limite de chamadas excedido no provedor.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "502",
                    description = "Falha de integração com o modelo.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "503",
                    description = "Modelo temporariamente indisponível.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "504",
                    description = "Tempo limite excedido ao aguardar resposta do modelo.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    @RolesAllowed("nitra-ai/write")
    public ChatResponse chatWithImage(
            @Valid ImageChatRequest request,
            @Parameter(
                    in = ParameterIn.QUERY,
                    name = "model",
                    description = "Modelo lógico a ser utilizado. Para imagem, use ANTHROPIC ou AMAZON_LITE.",
                    example = "AMAZON_LITE"
            )
            @QueryParam(value = "model") String model
    ) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório.");
        }

        String response = llmService.invokeLlmModelWithImage(
                request.message(),
                request.imageBase64(),
                request.imageFormat(),
                model
        );
        return new ChatResponse(response);
    }
}
