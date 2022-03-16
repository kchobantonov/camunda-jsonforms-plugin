package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.camunda.bpm.engine.impl.cmd.GetTaskFormVariablesCmd;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskManager;
import org.camunda.bpm.engine.variable.VariableMap;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * JsonFormFormService restricts the access to process variables based on the
 * attached json forms schema.
 */
public class JsonFormsFormService extends org.camunda.bpm.engine.impl.FormServiceImpl {

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

    private class JsonFormsGetTaskFormVariablesCmd extends GetTaskFormVariablesCmd {
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
                    String formFile = Utils.getFormFile(task.getFormKey());

                    ResourceEntity schema = deploymentEntity.getResource(formFile + Utils.RESOURCE_SCHEMA_SUFFIX);
                    if (schema != null) {
                        JSONObject jsonSchema = new JSONObject(
                                new JSONTokener(new ByteArrayInputStream(schema.getBytes())));

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
                    }
                }
            }

            return result;
        }
    }
}
