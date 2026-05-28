package br.com.nitra.ai.controller.error;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

@Provider
@Priority(Priorities.USER)
public class ValidationExceptionMapper implements ExceptionMapper<ResteasyReactiveViolationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ResteasyReactiveViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining("; "));

        ApiError body = new ApiError(
                "INVALID_REQUEST",
                message,
                uriInfo == null ? "" : uriInfo.getPath()
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(body)
                .build();
    }
}
