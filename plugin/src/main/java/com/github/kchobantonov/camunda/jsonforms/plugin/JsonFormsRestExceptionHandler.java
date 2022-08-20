package com.github.kchobantonov.camunda.jsonforms.plugin;

import javax.annotation.Priority;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.camunda.bpm.engine.rest.dto.ExceptionDto;
import org.camunda.bpm.engine.rest.exception.ExceptionHandlerHelper;
import org.camunda.bpm.engine.rest.exception.ExceptionLogger;
import org.camunda.bpm.engine.rest.exception.RestException;

@Provider
@Priority(1)
public class JsonFormsRestExceptionHandler implements ExceptionMapper<RestException> {
  protected static final ExceptionLogger LOGGER = ExceptionLogger.REST_LOGGER;

  @Override
  public Response toResponse(RestException exception) {
    if (exception.getCause() instanceof JsonFormsFormFieldValidatorException) {
      return getResponse((JsonFormsFormFieldValidatorException) exception.getCause());
    }
    return ExceptionHandlerHelper.getInstance().getResponse(exception);
  }

  protected Response getResponse(JsonFormsFormFieldValidatorException throwable) {
    LOGGER.log(throwable);

    Response.Status responseStatus = Response.Status.BAD_REQUEST;
    ExceptionDto exceptionDto = JsonFormsFormFieldValidatorExceptionDto.from(throwable);

    return Response
        .status(responseStatus)
        .entity(exceptionDto)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

}
