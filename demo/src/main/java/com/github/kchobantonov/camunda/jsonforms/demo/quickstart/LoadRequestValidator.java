package com.github.kchobantonov.camunda.jsonforms.demo.quickstart;

import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFormFieldValidator;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFormFieldValidatorException;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsPathResourceResolver;

@Component
public class LoadRequestValidator extends JsonFormsFormFieldValidator {

  @Autowired
  public LoadRequestValidator(JsonFormsPathResourceResolver resolver) {
    super(resolver);
  }

  @Override
  public boolean validate(Object submittedValue,
      FormFieldValidatorContext validatorContext) {
    boolean result = super.validate(submittedValue, validatorContext);
    if (result) {
      if ("TestValidation".equals(validatorContext.getSubmittedValues().get("firstName"))) {
        throw new JsonFormsFormFieldValidatorException(
            "firstName",
            getClass().getSimpleName(), validatorContext.getSubmittedValues().get("firstName"),
            "Invalid First Name", null);
      }
    }
    return result;
  }
}
