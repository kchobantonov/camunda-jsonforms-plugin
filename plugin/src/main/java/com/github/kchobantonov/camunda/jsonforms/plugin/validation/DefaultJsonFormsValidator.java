package com.github.kchobantonov.camunda.jsonforms.plugin.validation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.form.validator.FormFieldConfigurationException;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidator;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.spin.json.SpinJsonNode;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.ReadWriteContext;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.Validator;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.util.Assert;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsErrorObject;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsPathResourceResolver;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsValidatorException;
import com.github.kchobantonov.camunda.jsonforms.plugin.Utils;

public class DefaultJsonFormsValidator implements JsonFormsValidator, FormFieldValidator {
    private JsonFormsPathResourceResolver resolver;

    public DefaultJsonFormsValidator(JsonFormsPathResourceResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean validate(Object submittedValue, FormFieldValidatorContext validatorContext) {
        Map<String, Object> submittedValues = validatorContext.getSubmittedValues();
        return validate(submittedValues, validatorContext.getVariableScope());
    }

    @Override
    public boolean validate(Map<String, Object> submittedValues, VariableScope variableScope) {

        String formKey = getFormKey(variableScope);
        if (formKey != null && formKey.startsWith(Utils.CAMUNDA_JSONFORMS_URL)) {
            String deploymentId = getDeploymentId(variableScope);

            InputStream resource = getSchema(formKey, deploymentId);
            if (resource != null) {
                JSONObject jsonSchema = new JSONObject(
                        new JSONTokener(new InputStreamReader(resource, StandardCharsets.UTF_8)));

                JSONObject object = new JSONObject();
                for (Map.Entry<String, Object> entry : submittedValues.entrySet()) {
                    if ((entry.getValue() instanceof SpinJsonNode)) {
                        SpinJsonNode node = (SpinJsonNode) entry.getValue();

                        if (node.isObject()) {
                            object.put(entry.getKey(),
                                    new JSONObject(new JSONTokener(node.toString())));
                        } else if (node.isArray()) {
                            object.put(entry.getKey(),
                                    new JSONArray(new JSONTokener(node.toString())));
                        } else {
                            object.put(entry.getKey(), entry.getValue());
                        }
                    } else {
                        Object value = entry.getValue();
                        if (value instanceof InputStream) {
                            JSONObject propertySchema = jsonSchema.getJSONObject("properties")
                                    .getJSONObject(entry.getKey());
                            if (propertySchema != null) {
                                if ("string".equals(propertySchema.getString("type")) &&
                                        "binary".equals(propertySchema.getString("format"))) {
                                    // remove the type so that the validator won't require that
                                    // the value of type InputStream be compatible with the type
                                    // StringF
                                    propertySchema.remove("type");
                                }
                            }
                        }
                        object.put(entry.getKey(), value);
                    }
                }

                SchemaLoader loader = SchemaLoader.builder()
                        .schemaJson(jsonSchema)
                        .addFormatValidator("password", FormatValidator.NONE)
                        .draftV7Support()
                        .build();

                Schema schema = loader.load().build();

                try {
                    Validator validator = Validator.builder()
                            .failEarly()
                            .readWriteContext(ReadWriteContext.WRITE)
                            .build();
                    validator.performValidation(schema, object);

                    additionalValidations(validator, schema, object);
                    return true;
                } catch (ValidationException e) {
                    throw new JsonFormsValidatorException(toJsonFormsErrorObjects(e), e.getMessage(), e);
                }
            }
        }

        return true;
    }

    protected List<JsonFormsErrorObject> toJsonFormsErrorObjects(ValidationException exception) {
        List<JsonFormsErrorObject> result = new ArrayList<>();
        result.add(new JsonFormsErrorObject(exception.getKeyword(), exception.getPointerToViolation(),
                exception.getSchemaLocation(),
                exception.getErrorMessage()));

        List<ValidationException> causingExceptions = exception.getCausingExceptions();
        for (ValidationException e : causingExceptions) {
            result.add(new JsonFormsErrorObject(e.getKeyword(), e.getPointerToViolation(), e.getSchemaLocation(),
                    e.getErrorMessage()));
        }
        return result;
    }

    protected void additionalValidations(Validator validator, Schema schema, JSONObject object)
            throws JsonFormsValidatorException {

    }

    protected String getDeploymentId(VariableScope variableScope) {
        if (variableScope instanceof TaskEntity) {
            return ((TaskEntity) variableScope)
                    .getProcessDefinition()
                    .getDeploymentId();
        }

        if (variableScope instanceof ExecutionEntity) {
            return ((ExecutionEntity) variableScope)
                    .getProcessDefinition()
                    .getDeploymentId();
        }
        throw new FormFieldConfigurationException("Could not get deployment id");
    }

    protected String getFormKey(VariableScope variableScope) {
        if (variableScope instanceof TaskEntity) {
            ((TaskEntity) variableScope).initializeFormKey();
            return ((TaskEntity) variableScope).getFormKey();
        }

        if (variableScope instanceof ExecutionEntity) {
            FlowElement element = ((ExecutionEntity) variableScope).getBpmnModelElementInstance();
            if (element instanceof StartEvent) {
                return ((StartEvent) element).getCamundaFormKey();
            }
        }

        throw new IllegalStateException(
                "Did not receive a expected variable scope.");
    }

    protected InputStream getSchema(String formKey, String deploymentId) {
        String deploymentLocation = Utils.getDeploymentLocation(formKey);
        if (deploymentLocation != null) {
            ResourceEntity schema = Context.getCommandContext()
                    .getDeploymentManager()
                    .findDeploymentById(deploymentId)
                    .getResource(deploymentLocation + Utils.RESOURCE_SCHEMA_SUFFIX);

            if (schema != null) {
                return new ByteArrayInputStream(schema.getBytes());
            }
        }

        String pathLocation = Utils.getPathLocation(formKey);
        if (pathLocation != null && pathLocation.startsWith("/")) {
            Assert.notNull(resolver, "Resolver not setup correctly");
            return resolver.resolve(pathLocation);
        }

        return null;
    }
}
