package com.github.kchobantonov.camunda.jsonforms;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.form.StartFormData;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.form.FormDataImpl;
import org.camunda.bpm.engine.impl.form.handler.StartFormHandler;
import org.camunda.bpm.engine.impl.form.handler.TaskFormHandler;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.variable.VariableMap;

public class JsonFormsParseListener extends AbstractBpmnParseListener {

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope,
      ActivityImpl activity) {
    TaskDefinition taskDefinition = ((UserTaskActivityBehavior) activity.getActivityBehavior())
        .getTaskDefinition();
    TaskFormHandler handler = taskDefinition.getTaskFormHandler();
    if (handler != null) {
      taskDefinition.setTaskFormHandler(new JsonFormsFormHandler(handler));
    }
  }

  @Override
  public void parseStartEvent(Element startEventElement, ScopeImpl scope,
      ActivityImpl startEventActivity) {
    if (scope instanceof ProcessDefinitionEntity) {
      ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) scope;
      StartFormHandler handler = processDefinition.getStartFormHandler();
      if (handler != null) {
        processDefinition.setStartFormHandler(
            new JsonFormsFormHandler(handler));
      }
    }
  }

  protected boolean debugLogEnabled() {
    return Boolean.valueOf(System.getProperty(
        Utils.CAMUNDA_JSONFORMS_ENABLE_JS_CONSOLE_LOG, "false"));
  }

  class JsonFormsFormHandler implements TaskFormHandler, StartFormHandler {
    private StartFormHandler startFormHandler;
    private TaskFormHandler taskFormHandler;

    public JsonFormsFormHandler(StartFormHandler startFormHandler) {
      this.startFormHandler = startFormHandler;
    }

    public JsonFormsFormHandler(TaskFormHandler taskFormHandler) {
      this.taskFormHandler = taskFormHandler;
    }

    @Override
    public void parseConfiguration(Element activityElement,
        DeploymentEntity deployment,
        ProcessDefinitionEntity processDefinition,
        BpmnParse bpmnParse) {
      if (startFormHandler != null) {
        startFormHandler.parseConfiguration(activityElement, deployment,
            processDefinition, bpmnParse);
      } else {
        taskFormHandler.parseConfiguration(activityElement, deployment,
            processDefinition, bpmnParse);
      }
    }

    @Override
    public void submitFormVariables(VariableMap properties,
        VariableScope variableScope) {
      if (startFormHandler != null) {
        startFormHandler.submitFormVariables(properties, variableScope);
      } else {
        taskFormHandler.submitFormVariables(properties, variableScope);
      }
    }

    @Override
    public StartFormData createStartFormData(ProcessDefinitionEntity processDefinition) {
      StartFormData data = startFormHandler.createStartFormData(processDefinition);
      String formKey = data.getFormKey();
      if (formKey != null &&
          formKey.startsWith(Utils.CAMUNDA_JSONFORMS_URL + "?") &&
          data instanceof FormDataImpl) {

        formKey = transformFormKey(formKey);

        ((FormDataImpl) data).setFormKey(formKey);
      }
      return data;
    }

    @Override
    public TaskFormData createTaskForm(TaskEntity task) {
      TaskFormData data = taskFormHandler.createTaskForm(task);
      String formKey = data.getFormKey();
      if (formKey != null &&
          formKey.startsWith(Utils.CAMUNDA_JSONFORMS_URL + "?") &&
          data instanceof FormDataImpl) {
        formKey = transformFormKey(formKey);
        ((FormDataImpl) data).setFormKey(formKey);
      }
      return data;
    }

    private String transformFormKey(String formKey) {
      String path = System.getProperty(
          Utils.CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH);

      if (path != null && path.startsWith("/")) {

        int queryStart = formKey.indexOf("?");
        if (queryStart == -1 && queryStart < formKey.length() - 1) {
          return formKey;
        }

        Map<String, List<String>> parameters = Utils.parseQueryString(formKey.substring(queryStart + 1));
        List<String> deployment = parameters.remove(Utils.CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT);
        if (deployment == null || deployment.isEmpty()) {
          return formKey;
        }

        String fullPath = path + (path.endsWith("/") ? "" : "/") + deployment.get(0);
        parameters.put(Utils.CAMUNDA_FORM_KEY_QUERY_PARAM_PATH,
            Collections.singletonList(fullPath));

        formKey = formKey.substring(0, queryStart) + "?" + Utils.toQueryString(parameters);
      }

      if (JsonFormsParseListener.this.debugLogEnabled()) {
        formKey = formKey + "&debug=true";
      }

      return formKey;
    }
  }
}
