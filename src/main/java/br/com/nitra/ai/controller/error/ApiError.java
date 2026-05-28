package br.com.nitra.ai.controller.error;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ApiError", description = "Estrutura padrão para respostas de erro da API.")
public record ApiError(
        @Schema(example = "INVALID_REQUEST")
        String code,
        @Schema(example = "A mensagem é obrigatória.")
        String message,
        @Schema(example = "/llm/chat")
        String path
) {
}
