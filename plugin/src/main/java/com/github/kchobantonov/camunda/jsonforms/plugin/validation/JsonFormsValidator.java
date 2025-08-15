package com.github.kchobantonov.camunda.jsonforms.plugin.validation;

import java.util.Map;

import org.camunda.bpm.engine.delegate.VariableScope;

public interface JsonFormsValidator {
  /**
   * Validates the submitted form data against the JSON schema.
   *
   * @param submittedValues the submitted form data
   * @param variableScope   the variable scope
   * @return true if the form data is valid, false otherwise
   */
  public boolean validate(Map<String, Object> submittedValues, VariableScope variableScope);

}
