package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.Collection;

import org.camunda.bpm.engine.rest.dto.ExceptionDto;

public class JsonFormsValidatorExceptionDto extends ExceptionDto {
  protected Collection<JsonFormsErrorObject> validationErrors;

  public static JsonFormsValidatorExceptionDto from(JsonFormsValidatorException exception) {
    JsonFormsValidatorExceptionDto dto = new JsonFormsValidatorExceptionDto();
    dto.message = exception.getMessage();
    dto.type = exception.getClass().getSimpleName();
    dto.validationErrors = exception.toJsonFormsErrors();

    return dto;
  }

  public Collection<JsonFormsErrorObject> getValidationErrors() {
    return validationErrors;
  }

  public void setValidationErrors(Collection<JsonFormsErrorObject> validationErrors) {
    this.validationErrors = validationErrors;
  }
}
