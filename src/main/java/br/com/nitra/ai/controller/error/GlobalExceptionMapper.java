package br.com.nitra.ai.controller.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.InternalServerException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelErrorException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelTimeoutException;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceUnavailableException;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        ErrorMapping mapping = mapException(exception);
        logException(exception, mapping.status());

        ApiError body = new ApiError(
                mapping.code(),
                mapping.message(),
                uriInfo == null ? "" : uriInfo.getPath()
        );

        return Response.status(mapping.status())
                .entity(body)
                .build();
    }

    private ErrorMapping mapException(Throwable exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            Response.StatusType statusInfo = webApplicationException.getResponse().getStatusInfo();
            Response.Status status = Response.Status.fromStatusCode(statusInfo.getStatusCode());
            return new ErrorMapping(
                    status == null ? Response.Status.INTERNAL_SERVER_ERROR : status,
                    "HTTP_" + statusInfo.getStatusCode(),
                    statusInfo.getReasonPhrase()
            );
        }

        if (exception instanceof ConstraintViolationException violationException) {
            String message = violationException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .distinct()
                    .collect(Collectors.joining("; "));
            return new ErrorMapping(Response.Status.BAD_REQUEST, "INVALID_REQUEST", message);
        }

        if (exception instanceof IllegalArgumentException) {
            return new ErrorMapping(Response.Status.BAD_REQUEST, "INVALID_ARGUMENT", safeMessage(exception, "Parâmetros inválidos."));
        }

        if (exception instanceof AccessDeniedException) {
            return new ErrorMapping(Response.Status.FORBIDDEN, "BEDROCK_ACCESS_DENIED", "Acesso negado ao Amazon Bedrock.");
        }

        if (exception instanceof ValidationException) {
            return new ErrorMapping(Response.Status.BAD_REQUEST, "BEDROCK_VALIDATION_ERROR", "A requisição foi rejeitada pelo Amazon Bedrock.");
        }

        if (exception instanceof ThrottlingException) {
            return new ErrorMapping(Response.Status.TOO_MANY_REQUESTS, "BEDROCK_THROTTLED", "Limite de requisições do Amazon Bedrock excedido.");
        }

        if (exception instanceof ModelTimeoutException) {
            return new ErrorMapping(Response.Status.GATEWAY_TIMEOUT, "BEDROCK_TIMEOUT", "O modelo demorou mais do que o esperado para responder.");
        }

        if (exception instanceof ServiceUnavailableException || exception instanceof ModelNotReadyException) {
            return new ErrorMapping(Response.Status.SERVICE_UNAVAILABLE, "BEDROCK_UNAVAILABLE", "O Amazon Bedrock está temporariamente indisponível.");
        }

        if (exception instanceof IllegalStateException) {
            return new ErrorMapping(Response.Status.BAD_GATEWAY, "INVALID_PROVIDER_RESPONSE", safeMessage(exception, "O modelo retornou uma resposta inválida."));
        }

        if (exception instanceof ModelErrorException
                || exception instanceof InternalServerException
                || exception instanceof BedrockRuntimeException
                || exception instanceof SdkClientException) {
            return new ErrorMapping(Response.Status.BAD_GATEWAY, "BEDROCK_UPSTREAM_ERROR", "Falha ao processar a requisição no Amazon Bedrock.");
        }

        return new ErrorMapping(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Erro interno inesperado.");
    }

    private void logException(Throwable exception, Response.Status status) {
        if (status.getStatusCode() >= 500) {
            LOG.error("Erro ao processar requisição", exception);
            return;
        }

        LOG.warnf("%s: %s", exception.getClass().getSimpleName(), safeMessage(exception, "Sem detalhes."));
    }

    private String safeMessage(Throwable exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }

    private record ErrorMapping(Response.Status status, String code, String message) {
    }
}
