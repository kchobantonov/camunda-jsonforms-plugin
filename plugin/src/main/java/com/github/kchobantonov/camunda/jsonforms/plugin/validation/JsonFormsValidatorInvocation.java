package com.github.kchobantonov.camunda.jsonforms.plugin.validation;

import java.util.Map;

import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.delegate.DelegateInvocation;

public class JsonFormsValidatorInvocation extends DelegateInvocation {

  protected JsonFormsValidator formFieldValidator;
  protected Map<String, Object> submittedValues;
  protected VariableScope variableScope;

  public JsonFormsValidatorInvocation(JsonFormsValidator formFieldValidator, final Map<String, Object> submittedValues,
      VariableScope variableScope) {
    super(null, null);
    this.formFieldValidator = formFieldValidator;
    this.submittedValues = submittedValues;
    this.variableScope = variableScope;
  }

  protected void invoke() throws Exception {
    invocationResult = formFieldValidator.validate(submittedValues, variableScope);
  }

  public Boolean getInvocationResult() {
    return (Boolean) super.getInvocationResult();
  }

}
