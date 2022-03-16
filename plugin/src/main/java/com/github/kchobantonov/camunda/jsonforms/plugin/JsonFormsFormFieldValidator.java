package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.form.validator.FormFieldConfigurationException;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidator;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.camunda.bpm.engine.impl.form.validator.FormFieldValidatorException;
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

public class JsonFormsFormFieldValidator implements FormFieldValidator {

    @Override
    public boolean validate(Object submittedValue,
            FormFieldValidatorContext validatorContext) {

        Map<String, Object> submittedValues = validatorContext.getSubmittedValues();

        String formKey = getFormKey(validatorContext.getVariableScope());
        String deploymentId = getDeploymentId(validatorContext.getVariableScope());
        String formFile = Utils.getFormFile(formKey);

        if (formFile != null) {

            ResourceEntity resource = Context.getCommandContext()
                    .getDeploymentManager()
                    .findDeploymentById(deploymentId)
                    .getResource(formFile + Utils.RESOURCE_SCHEMA_SUFFIX);

            if (resource != null) {
                JSONObject jsonSchema = new JSONObject(
                        new JSONTokener(new ByteArrayInputStream(resource.getBytes())));

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

                    return true;
                } catch (ValidationException e) {
                    throw new FormFieldValidatorException(
                            Utils.CUSTOM_FORM_FIELD_VALIDATOR_NAME,
                            Utils.CUSTOM_FORM_FIELD_VALIDATOR_NAME, null, submittedValue,
                            e.getErrorMessage(), e);
                }
            }
        }

        return true;
    }

    private String getDeploymentId(VariableScope variableScope) {
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

    private String getFormKey(VariableScope variableScope) {
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
}
