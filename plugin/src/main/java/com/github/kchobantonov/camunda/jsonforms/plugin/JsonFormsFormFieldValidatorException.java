package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorException;
import org.everit.json.schema.ValidationException;

public class JsonFormsFormFieldValidatorException extends FormFieldValidatorException {
  public JsonFormsFormFieldValidatorException(String id, String config, Object value, String message,
      Throwable cause) {
    super(id, "validator", config, value, message, cause);
  }

  public JsonFormsFormFieldValidatorException(String id, String config, Object value, String message) {
    super(id, "validator", config, value, message);
  }

  public List<JsonFormsErrorObject> toJsonFormsErrors() {
    List<JsonFormsErrorObject> result = new ArrayList<>();
    if (getCause() instanceof ValidationException) {
      ValidationException cause = (ValidationException) getCause();
      result.add(new JsonFormsErrorObject(cause.getKeyword(), cause.getPointerToViolation(), cause.getSchemaLocation(),
          cause.getErrorMessage()));

      List<ValidationException> causingExceptions = ((ValidationException) getCause()).getCausingExceptions();
      for (ValidationException e : causingExceptions) {
        result.add(new JsonFormsErrorObject(e.getKeyword(), e.getPointerToViolation(), e.getSchemaLocation(),
            e.getErrorMessage()));
      }
    } else {
      String path = getId();
      if (!path.startsWith("/")) {
        path = "/" + path;
      }
      result.add(new JsonFormsErrorObject(path, getMessage()));
    }
    return result;
  }

}

