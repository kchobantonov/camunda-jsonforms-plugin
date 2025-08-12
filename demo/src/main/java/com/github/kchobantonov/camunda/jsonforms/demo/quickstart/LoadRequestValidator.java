package com.github.kchobantonov.camunda.jsonforms.demo.quickstart;

import java.util.Collections;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsErrorObject;
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
      JsonFormsErrorObject error = JsonFormsErrorObject.builder()
          .message("Invalid First Name")
          .instancePath("/firstName")
          .schemaPath("#/properties/firstName")
          .build();

      throw new JsonFormsFormFieldValidatorException(Collections.singleton(error));
    }
  }

}
