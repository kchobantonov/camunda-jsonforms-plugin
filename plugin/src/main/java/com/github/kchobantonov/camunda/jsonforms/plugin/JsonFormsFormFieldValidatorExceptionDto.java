package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.List;

import org.camunda.bpm.engine.rest.dto.ExceptionDto;

public class JsonFormsFormFieldValidatorExceptionDto extends ExceptionDto {
  protected List<JsonFormsErrorObject> validationErrors;

  public static JsonFormsFormFieldValidatorExceptionDto from(JsonFormsFormFieldValidatorException exception) {
    JsonFormsFormFieldValidatorExceptionDto dto = new JsonFormsFormFieldValidatorExceptionDto();
    dto.message = exception.getMessage();
    dto.type = exception.getClass().getSimpleName();
    dto.validationErrors = exception.toJsonFormsErrors();

    return dto;
  }

  public List<JsonFormsErrorObject> getValidationErrors() {
    return validationErrors;
  }

  public void setValidationErrors(List<JsonFormsErrorObject> validationErrors) {
    this.validationErrors = validationErrors;
  }
}
