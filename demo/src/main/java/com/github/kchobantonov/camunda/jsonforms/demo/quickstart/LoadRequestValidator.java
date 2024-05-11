package com.github.kchobantonov.camunda.jsonforms.demo.quickstart;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFormFieldValidator;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFormFieldValidatorException;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsPathResourceResolver;

@Component
public class LoadRequestValidator extends JsonFormsFormFieldValidator {

  public LoadRequestValidator(JsonFormsPathResourceResolver resolver) {
    super(resolver);
  }

  @Override
  protected void additionalValidations(Validator validator, Schema schema, JSONObject object)
      throws JsonFormsFormFieldValidatorException {

    if (object.has("firstName") && "TestValidation".equals(object.getString("firstName"))) {
      throw new JsonFormsFormFieldValidatorException(
          "firstName",
          getClass().getSimpleName(), object.getString("firstName"),
          "Invalid First Name", null);
    }
  }

}
