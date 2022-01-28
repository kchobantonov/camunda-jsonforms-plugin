package com.github.kchobantonov.camunda.jsonforms;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
 * JsonFormFormService restricts the access to process variables based on the attached json forms schema.
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
                    && task.getFormKey().startsWith("embedded:app:webjars/forms/jsonforms.html")) {

                DeploymentEntity deploymentEntity = commandContext.getDeploymentManager()
                        .findDeploymentById(task.getProcessDefinition().getDeploymentId());
                if (deploymentEntity != null) {
                    String formFile = getFormFile(task.getFormKey());

                    ResourceEntity schema = deploymentEntity.getResource(formFile + ".schema.json");
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

        private Map<String, List<String>> parseQueryString(String s) {
            Map<String, List<String>> ht = new HashMap<>();
            StringTokenizer st = new StringTokenizer(s, "&");
            while (st.hasMoreTokens()) {
                String pair = st.nextToken();
                int pos = pair.indexOf('=');
                List<String> values = ht.computeIfAbsent(pair.substring(0, pos), (key) -> new ArrayList<>());
                if (pos == -1) {
                    values.add("");
                } else {
                    try {
                        values.add(URLDecoder.decode(pair.substring(pos + 1), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return ht;
        }

        private String getFormFile(String formKey) {
            int queryStart = formKey.indexOf("?");
            if (queryStart == -1 && queryStart < formKey.length() - 1) {
                return null;
            }

            Map<String, List<String>> parameters = parseQueryString(formKey.substring(queryStart + 1));
            List<String> deployment = parameters.get("deployment");
            if (deployment == null || deployment.isEmpty()) {
                return null;
            }

            return deployment.get(0);
        }
    }
}
