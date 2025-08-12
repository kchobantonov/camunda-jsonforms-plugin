package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.Collection;

import org.camunda.bpm.engine.rest.dto.ExceptionDto;

public class JsonFormsFormFieldValidatorExceptionDto extends ExceptionDto {
  protected Collection<JsonFormsErrorObject> validationErrors;

  public static JsonFormsFormFieldValidatorExceptionDto from(JsonFormsFormFieldValidatorException exception) {
    JsonFormsFormFieldValidatorExceptionDto dto = new JsonFormsFormFieldValidatorExceptionDto();
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
