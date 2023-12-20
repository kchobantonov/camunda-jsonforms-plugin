package com.github.kchobantonov.camunda.jsonforms.plugin;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.BadUserRequestException;
import org.camunda.bpm.engine.exception.DeploymentResourceNotFoundException;
import org.camunda.bpm.engine.exception.NotFoundException;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.impl.GetDeployedTaskFormCmd;
import org.camunda.bpm.engine.impl.cfg.CommandChecker;
import org.camunda.bpm.engine.impl.cmd.GetDeployedStartFormCmd;
import org.camunda.bpm.engine.impl.cmd.GetDeploymentResourceCmd;
import org.camunda.bpm.engine.impl.cmd.GetTaskFormVariablesCmd;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskManager;
import org.camunda.bpm.engine.variable.VariableMap;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.util.Assert;

/**
 * JsonFormFormService restricts the access to process variables based on the
 * attached json forms schema.
 */
public class JsonFormsFormService extends org.camunda.bpm.engine.impl.FormServiceImpl {
    private final JsonFormsPathResourceResolver resolver;

    public JsonFormsFormService(JsonFormsPathResourceResolver resolver) {
        Assert.notNull(resolver, "Path resolver shouldn't be null");
        this.resolver = resolver;
    }

    // TODO: other variable methods
    /*
     * @Override
     * public VariableResource getLocalVariables() {
     * return super.getLocalVariables();
     * }
     * 
     * @Override
     * public VariableResource getVariables() {
     * return super.getVariables();
     * }
     * 
     */

    @Override
    public VariableMap getTaskFormVariables(String taskId, Collection<String> formVariables,
            boolean deserializeObjectValues) {
        return commandExecutor
                .execute(new JsonFormsGetTaskFormVariablesCmd(taskId, formVariables, deserializeObjectValues));
    }

    @Override
    public InputStream getDeployedStartForm(String processDefinitionId) {
        return commandExecutor.execute(new JsonFormsGetDeployedStartFormCmd(processDefinitionId));
    }

    @Override
    public InputStream getDeployedTaskForm(String taskId) {
        return commandExecutor.execute(new JsonFromsGetDeployedTaskFormCmd(taskId));
    }

    protected class JsonFormsGetTaskFormVariablesCmd extends GetTaskFormVariablesCmd {
        public JsonFormsGetTaskFormVariablesCmd(String taskId, Collection<String> variableNames,
                boolean deserializeObjectValues) {
            super(taskId, variableNames, deserializeObjectValues);
        }

        @Override
        public VariableMap execute(CommandContext commandContext) {
            VariableMap result = super.execute(commandContext);

            final TaskManager taskManager = commandContext.getTaskManager();
            // check if this is going to be retrieved from the cache or will invoke select
            // from the db
            TaskEntity task = taskManager.findTaskById(resourceId);

            task.initializeFormKey();

            if (task.getFormKey() != null
                    && task.getFormKey().startsWith(Utils.CAMUNDA_JSONFORMS_URL)) {

                DeploymentEntity deploymentEntity = commandContext.getDeploymentManager()
                        .findDeploymentById(task.getProcessDefinition().getDeploymentId());
                if (deploymentEntity != null) {
                    InputStream schema = getSchema(task, deploymentEntity);
                    if (schema != null) {
                        try {
                            JSONObject jsonSchema = new JSONObject(
                                    new JSONTokener(new InputStreamReader(schema, StandardCharsets.UTF_8)));

                            JSONObject properties = jsonSchema.optJSONObject("properties");
                            if (properties != null) {
                                for (Iterator<Map.Entry<String, Object>> entryIterator = result.entrySet()
                                        .iterator(); entryIterator.hasNext();) {
                                    Map.Entry<String, Object> entry = entryIterator.next();
                                    JSONObject property = properties.optJSONObject(entry.getKey());

                                    if (property == null || property.optBoolean("writeOnly")) {
                                        entryIterator.remove();
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            // ignore invalid JSON schema
                        }
                    }
                }
            }

            return result;
        }
    }

    protected class JsonFormsGetDeployedStartFormCmd extends GetDeployedStartFormCmd {
        public JsonFormsGetDeployedStartFormCmd(String processDefinitionId) {
            super(processDefinitionId);
        }

        protected InputStream getJsonFormsDeploymentResource(String deploymentId, String resourceName) {
            JsonFormsGetDeploymentResourceCmd getDeploymentResourceCmd = new JsonFormsGetDeploymentResourceCmd(
                    deploymentId, resourceName);
            try {
                return commandContext.runWithoutAuthorization(getDeploymentResourceCmd);
            } catch (DeploymentResourceNotFoundException e) {
                throw new NotFoundException("The form with the resource name '" + resourceName
                        + "' cannot be found in deployment with id " + deploymentId, e);
            }
        }

        @Override
        protected InputStream getResourceForFormKey(FormData formData, String formKey) {

            if (formKey.startsWith(Utils.CAMUNDA_JSONFORMS_URL)) {
                String location = Utils.getDeploymentLocation(formKey);
                if (location == null) {
                    throw new BadUserRequestException(
                            "The form key '" + formKey + "' is missing deployment query parameter.");
                }

                return getJsonFormsDeploymentResource(formData.getDeploymentId(), location);
            }
            return super.getResourceForFormKey(formData, formKey);
        }

    }

    protected class JsonFormsGetDeploymentResourceCmd extends GetDeploymentResourceCmd {
        public JsonFormsGetDeploymentResourceCmd(String deploymentId, String resourceName) {
            super(deploymentId, resourceName);
        }

        @Override
        public InputStream execute(CommandContext commandContext) {
            ensureNotNull("deploymentId", deploymentId);
            ensureNotNull("resourceName", resourceName);

            for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
                checker.checkReadDeployment(deploymentId);
            }

            final String schemaResourcePath = resourceName + Utils.RESOURCE_SCHEMA_SUFFIX;
            final String uischemaResourcePath = resourceName + Utils.RESOURCE_UISCHEMA_SUFFIX;
            final String i18nResourcePath = resourceName + Utils.RESOURCE_I18N_SUFFIX;

            List<ResourceEntity> resources = commandContext
                    .getResourceManager()
                    .findResourceByDeploymentIdAndResourceNames(deploymentId,
                            schemaResourcePath, uischemaResourcePath, i18nResourcePath);

            ResourceEntity schema = resources.stream().filter(entity -> entity.getName().equals(schemaResourcePath))
                    .findFirst().orElse(null);
            ResourceEntity uischema = resources.stream().filter(entity -> entity.getName().equals(uischemaResourcePath))
                    .findFirst().orElse(null);
            ResourceEntity i18n = resources.stream().filter(entity -> entity.getName().equals(i18nResourcePath))
                    .findFirst().orElse(null);

            ensureNotNull(DeploymentResourceNotFoundException.class,
                    "no resource found with name '" + schemaResourcePath + "' in deployment '" + deploymentId + "'",
                    "resource", schema);
            ensureNotNull(DeploymentResourceNotFoundException.class,
                    "no resource found with name '" + uischemaResourcePath + "' in deployment '"
                            + deploymentId + "'",
                    "resource", uischema);

            StringBuilder result = new StringBuilder();
            result.append("{");

            result.append(JSONObject.quote(schemaResourcePath));
            result.append(":");
            result.append(new String(schema.getBytes(), StandardCharsets.UTF_8));

            if (uischema != null) {
                result.append(",");
                result.append(JSONObject.quote(uischemaResourcePath));
                result.append(":");
                result.append(new String(uischema.getBytes(), StandardCharsets.UTF_8));
            }

            if (i18n != null) {
                result.append(",");
                result.append(JSONObject.quote(i18nResourcePath));
                result.append(":");
                result.append(new String(i18n.getBytes(), StandardCharsets.UTF_8));
            }

            result.append("}");

            return new ByteArrayInputStream(result.toString().getBytes(StandardCharsets.UTF_8));
        }

    }

    protected class JsonFromsGetDeployedTaskFormCmd extends GetDeployedTaskFormCmd {
        public JsonFromsGetDeployedTaskFormCmd(String taskId) {
            super(taskId);
        }

        protected InputStream getJsonFormsDeploymentResource(String deploymentId, String resourceName) {
            JsonFormsGetDeploymentResourceCmd getDeploymentResourceCmd = new JsonFormsGetDeploymentResourceCmd(
                    deploymentId, resourceName);
            try {
                return commandContext.runWithoutAuthorization(getDeploymentResourceCmd);
            } catch (DeploymentResourceNotFoundException e) {
                throw new NotFoundException("The form with the resource name '" + resourceName
                        + "' cannot be found in deployment with id " + deploymentId, e);
            }
        }

        @Override
        protected InputStream getResourceForFormKey(FormData formData, String formKey) {
            if (formKey.startsWith(Utils.CAMUNDA_JSONFORMS_URL)) {
                String location = Utils.getDeploymentLocation(formKey);
                if (location == null) {
                    throw new BadUserRequestException(
                            "The form key '" + formKey + "' is missing deployment query parameter.");
                }

                return getJsonFormsDeploymentResource(formData.getDeploymentId(), location);
            }

            return super.getResourceForFormKey(formData, formKey);

        }

    }

    protected InputStream getSchema(TaskEntity task, DeploymentEntity deploymentEntity) {
        String deploymentLocation = Utils.getDeploymentLocation(task.getFormKey());
        if (deploymentLocation != null) {
            ResourceEntity schema = deploymentEntity.getResource(deploymentLocation + Utils.RESOURCE_SCHEMA_SUFFIX);
            if (schema != null) {
                return new ByteArrayInputStream(schema.getBytes());
            }
        }

        String pathLocation = Utils.getPathLocation(task.getFormKey());
        if (pathLocation != null && pathLocation.startsWith("/")) {
            return resolver.resolve(pathLocation);
        }

        return null;
    }
}