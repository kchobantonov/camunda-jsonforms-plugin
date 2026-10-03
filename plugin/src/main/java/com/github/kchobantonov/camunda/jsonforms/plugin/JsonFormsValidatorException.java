package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.Collection;

import org.camunda.bpm.engine.impl.form.FormException;

/**
 * A schema violation, carrying the per-field errors a JsonForms client renders.
 *
 * Extends {@code FormException} rather than {@code FormFieldValidationException}, which is its
 * subclass, for two reasons - both of which only show once a form declares its validator as a
 * {@code <camunda:constraint name="validator">} on a form field, the way BPMNs in practice do:
 *
 * <ul>
 * <li>{@code FormFieldValidationConstraintHandler.validate} catches
 * {@code FormFieldValidationException} and rethrows it wrapped in a
 * {@code FormFieldValidatorException}. The wrapper is what the caller then sees, so
 * {@link JsonFormsRestExceptionHandler} - which looks for this type as the cause of a
 * {@code RestException} - finds the wrapper instead and the field-level errors never reach the
 * client.</li>
 * <li>{@code FormFieldValidationException} has no {@code (String, Throwable)} constructor. Its
 * closest match is {@code (Object detail, Throwable cause)}, so the message was being stored as
 * the {@code detail} and {@code getMessage()} answered null.</li>
 * </ul>
 *
 * {@code FormException} extends {@code ProcessEngineException}, so nothing downstream of the
 * REST layer changes.
 */
public class JsonFormsValidatorException extends FormException {
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
