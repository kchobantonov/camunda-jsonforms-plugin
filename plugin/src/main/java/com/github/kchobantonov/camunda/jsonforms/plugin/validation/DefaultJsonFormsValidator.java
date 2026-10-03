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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsErrorObject;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsPathResourceResolver;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsValidatorException;
import com.github.kchobantonov.camunda.jsonforms.plugin.Utils;

public class DefaultJsonFormsValidator implements JsonFormsValidator, FormFieldValidator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultJsonFormsValidator.class);
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
            if (resource == null) {
                // Deliberately still a pass - refusing here would take down every form whose
                // schema has not been deployed yet, which is not this class's call to make. But
                // it is silent, and silence is the failure mode that matters: a schema resource
                // that was renamed, or left out of a deployment, disables every rule the form
                // declares - readOnly, const, additionalProperties, the lot - with nothing
                // anywhere to say so. One line is the difference between a misconfiguration
                // someone finds and one that holds until it is exploited.
                LOG.warn("no schema resource for form key {} in deployment {} - the submission "
                        + "was NOT validated", formKey, deploymentId);
            }
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

    /**
     * The failures a client can act on: the leaves of the violation tree.
     *
     * A combinator hides the real cause behind itself. Validating a form whose requirements
     * depend on which action was submitted - {@code allOf} of {@code if}/{@code then}, the
     * shape every conditional form takes - produces a tree like
     *
     * <pre>
     * allOf     #                   "only 1 subschema matches out of 2"
     *   allOf   #                   "only 1 subschema matches out of 2"
     *     then  #                   "input is invalid against the then schema"
     *       required #/attestation  "required key [phone] not found"
     * </pre>
     *
     * Only the last line names a field. Walking one level down reported the first two - the
     * same message twice - and dropped the one the person filling the form needed, so a
     * missing phone number came back as "only 1 subschema matches out of 2".
     *
     * The pointer is converted on the way out. everit writes a JSON pointer with a leading
     * {@code #}; an AJV {@code instancePath}, which is what a JsonForms client matches against
     * its controls, does not - so {@code #/attestation/phone} has to become
     * {@code /attestation/phone} for the error to land on the field rather than nowhere.
     */
    protected List<JsonFormsErrorObject> toJsonFormsErrorObjects(ValidationException exception) {
        List<JsonFormsErrorObject> result = new ArrayList<>();
        collectLeaves(exception, result);

        if (result.isEmpty()) {
            // a violation with no causes is its own leaf; this only guards against a future
            // everit reporting an empty tree, which would otherwise answer with no errors
            result.add(toErrorObject(exception));
        }
        return result;
    }

    private void collectLeaves(ValidationException exception, List<JsonFormsErrorObject> into) {
        List<ValidationException> causes = exception.getCausingExceptions();

        if (causes.isEmpty()) {
            into.add(toErrorObject(exception));
            return;
        }
        for (ValidationException cause : causes) {
            collectLeaves(cause, into);
        }
    }

    private JsonFormsErrorObject toErrorObject(ValidationException exception) {
        return new JsonFormsErrorObject(exception.getKeyword(), instancePath(exception.getPointerToViolation()),
                exception.getSchemaLocation(), message(exception));
    }

    /**
     * everit does not describe every violation it reports - a failed {@code const} comes back
     * with an empty message - and a client that renders what it is given then shows an error
     * marker with no text against the field. Naming the rule that was broken is little, but it
     * is more than nothing.
     */
    private static String message(ValidationException exception) {
        String message = exception.getErrorMessage();

        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        String keyword = exception.getKeyword();
        return keyword == null ? "is not valid" : "does not satisfy \"" + keyword + "\"";
    }

    /** everit's {@code #/a/b} as AJV's {@code /a/b}; the document root as the empty path. */
    private static String instancePath(String pointerToViolation) {
        if (pointerToViolation == null) {
            return null;
        }
        return pointerToViolation.startsWith("#") ? pointerToViolation.substring(1) : pointerToViolation;
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
            // the resolver takes a resource name, so the suffix has to be on it: asking for the
            // bare location finds nothing, and validate() then passes every submission
            return resolver.resolve(pathLocation + Utils.RESOURCE_SCHEMA_SUFFIX);
        }

        return null;
    }
}
