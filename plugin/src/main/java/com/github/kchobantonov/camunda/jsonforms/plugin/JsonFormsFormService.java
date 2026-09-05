package com.github.kchobantonov.camunda.jsonforms.plugin;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

        @Override
        protected InputStream getResourceForFormKey(FormData formData, String formKey) {
            if (formKey.startsWith(Utils.CAMUNDA_JSONFORMS_URL)) {
                return getJsonFormsResource(formKey, formData.getDeploymentId(), commandContext);
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

            List<ResourceEntity> resources = commandContext
                    .getResourceManager()
                    .findResourceByDeploymentIdAndResourceNames(deploymentId,
                            resourceName + Utils.RESOURCE_SCHEMA_SUFFIX,
                            resourceName + Utils.RESOURCE_UISCHEMA_SUFFIX,
                            resourceName + Utils.RESOURCE_I18N_SUFFIX,
                            resourceName + Utils.RESOURCE_UISCHEMAS_SUFFIX,
                            resourceName + Utils.RESOURCE_UIDATA_SUFFIX);

            return bundle(resourceName, name -> resources.stream()
                    .filter(entity -> entity.getName().equals(name))
                    .findFirst()
                    .map(entity -> (InputStream) new ByteArrayInputStream(entity.getBytes()))
                    .orElse(null),
                    "deployment '" + deploymentId + "'");
        }

    }

    protected class JsonFromsGetDeployedTaskFormCmd extends GetDeployedTaskFormCmd {
        public JsonFromsGetDeployedTaskFormCmd(String taskId) {
            super(taskId);
        }

        @Override
        protected InputStream getResourceForFormKey(FormData formData, String formKey) {
            if (formKey.startsWith(Utils.CAMUNDA_JSONFORMS_URL)) {
                return getJsonFormsResource(formKey, formData.getDeploymentId(), commandContext);
            }

            return super.getResourceForFormKey(formData, formKey);

        }

    }

    /**
     * The form the key names, as the bundle of sibling resources the renderer expects, read from
     * wherever the key says.
     *
     * {@code path} and {@code deployment} are alternatives rather than a preference order: a key
     * carries one or the other, and which one it carries is the whole statement of where the form
     * lives. A {@code path} key is either authored that way in the BPMN or was rewritten from a
     * {@code deployment} one by {@link JsonFormsFormHandler#transformFormKey} because the forms
     * are being served off disk - and in both cases the deployment is deliberately not the
     * source, so it is not consulted.
     *
     * That second case is why this has to handle {@code path} at all: the rewrite happens in the
     * form data this command reads its key from, so a service that only understood
     * {@code deployment} refused the very key the plugin had just produced.
     */
    protected InputStream getJsonFormsResource(String formKey, String deploymentId,
            CommandContext commandContext) {
        String pathLocation = Utils.getPathLocation(formKey);
        if (pathLocation != null && pathLocation.startsWith("/")) {
            return getJsonFormsPathResource(pathLocation);
        }

        String location = Utils.getDeploymentLocation(formKey);
        if (location == null) {
            throw new BadUserRequestException(
                    "The form key '" + formKey + "' is missing deployment or path query parameter.");
        }
        return getJsonFormsDeploymentResource(deploymentId, location, commandContext);
    }

    protected InputStream getJsonFormsDeploymentResource(String deploymentId, String resourceName,
            CommandContext commandContext) {
        JsonFormsGetDeploymentResourceCmd getDeploymentResourceCmd = new JsonFormsGetDeploymentResourceCmd(
                deploymentId, resourceName);
        try {
            return commandContext.runWithoutAuthorization(getDeploymentResourceCmd);
        } catch (DeploymentResourceNotFoundException e) {
            throw new NotFoundException("The form with the resource name '" + resourceName
                    + "' cannot be found in deployment with id " + deploymentId, e);
        }
    }

    /** The same bundle, assembled from the resources the path resolver hands back. */
    protected InputStream getJsonFormsPathResource(String pathLocation) {
        Assert.notNull(resolver, "Resolver not setup correctly");
        try {
            return bundle(pathLocation, resolver::resolve, "path '" + pathLocation + "'");
        } catch (DeploymentResourceNotFoundException e) {
            throw new NotFoundException("The form with the path '" + pathLocation + "' cannot be found", e);
        }
    }

    /**
     * The sibling resources of one form as a single JSON object keyed by resource name - what
     * {@code loadResourcesFromDeployedForm} in {@code @chobantonov/camunda-jsonforms} reads.
     *
     * The schema and the layout are required, because nothing can be rendered without them;
     * {@code i18n}, {@code uischemas} and {@code uidata} are optional and simply left out when
     * absent.
     *
     * @param location the form's location, which the resource names are suffixes of
     * @param lookup   opens one resource by name, returning null when it does not exist
     * @param source   where the resources are being read from, for the not-found message
     */
    protected InputStream bundle(String location, Function<String, InputStream> lookup, String source) {
        String schemaResourcePath = location + Utils.RESOURCE_SCHEMA_SUFFIX;
        String uischemaResourcePath = location + Utils.RESOURCE_UISCHEMA_SUFFIX;
        String i18nResourcePath = location + Utils.RESOURCE_I18N_SUFFIX;
        String uischemasResourcePath = location + Utils.RESOURCE_UISCHEMAS_SUFFIX;
        String uidataResourcePath = location + Utils.RESOURCE_UIDATA_SUFFIX;

        String schema = read(lookup, schemaResourcePath);
        String uischema = read(lookup, uischemaResourcePath);

        ensureNotNull(DeploymentResourceNotFoundException.class,
                "no resource found with name '" + schemaResourcePath + "' in " + source, "resource", schema);
        ensureNotNull(DeploymentResourceNotFoundException.class,
                "no resource found with name '" + uischemaResourcePath + "' in " + source, "resource", uischema);

        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append(JSONObject.quote(schemaResourcePath));
        result.append(":");
        result.append(schema);

        appendIfPresent(result, uischemaResourcePath, uischema);
        appendIfPresent(result, i18nResourcePath, read(lookup, i18nResourcePath));
        appendIfPresent(result, uischemasResourcePath, read(lookup, uischemasResourcePath));
        appendIfPresent(result, uidataResourcePath, read(lookup, uidataResourcePath));

        result.append("}");

        return new ByteArrayInputStream(result.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void appendIfPresent(StringBuilder result, String resourcePath, String content) {
        if (content == null) {
            return;
        }
        result.append(",");
        result.append(JSONObject.quote(resourcePath));
        result.append(":");
        result.append(content);
    }

    /** One resource as a string, or null when it is not there or cannot be read. */
    private static String read(Function<String, InputStream> lookup, String resourceName) {
        try (InputStream resource = lookup.apply(resourceName)) {
            return resource == null ? null : new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
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
            // the resolver takes a resource name, so the suffix has to be on it: asking for the
            // bare location finds nothing, and this method returning null silently switches off
            // the schema-scoping of form variables
            return resolver.resolve(pathLocation + Utils.RESOURCE_SCHEMA_SUFFIX);
        }

        return null;
    }
}