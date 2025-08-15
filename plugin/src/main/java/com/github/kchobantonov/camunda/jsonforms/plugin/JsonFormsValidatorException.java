package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.Collection;

import org.camunda.bpm.engine.impl.form.validator.FormFieldValidationException;

public class JsonFormsValidatorException extends FormFieldValidationException {
  private final Collection<JsonFormsErrorObject> errors;

  public JsonFormsValidatorException(Collection<JsonFormsErrorObject> errors) {
    this(errors, errors != null && !errors.isEmpty() ? errors.iterator().next().getMessage() : "Validation Error",
        null);
  }

  public JsonFormsValidatorException(Collection<JsonFormsErrorObject> errors, String message, Throwable cause) {
    super(message, cause);
    this.errors = errors;
  }

  public Collection<JsonFormsErrorObject> toJsonFormsErrors() {
    return this.errors;
  }

}
