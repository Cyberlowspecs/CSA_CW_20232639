package com.smartcampus.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable exception) {
        // If it's already a WebApplicationException (like 404), don't override it with 500
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }

        // Return a generic 500 Internal Server Error JSON response to avoid leaking stack traces
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"An unexpected internal server error occurred\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
