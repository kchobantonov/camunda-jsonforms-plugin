package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.form.StartFormData;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.impl.form.FormDataImpl;
import org.camunda.bpm.engine.impl.form.handler.StartFormHandler;
import org.camunda.bpm.engine.impl.form.handler.TaskFormHandler;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.util.StringUtil;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.variable.VariableMap;

import com.github.kchobantonov.camunda.jsonforms.plugin.validation.DelegateJsonFormsValidator;

public class JsonFormsFormHandler implements TaskFormHandler, StartFormHandler {
  public static final String JSON_FORMS_VALIDATOR_EXTENSION_PROPERTY = "jsonFormsValidator";

  private StartFormHandler startFormHandler;
  private TaskFormHandler taskFormHandler;
  private DelegateJsonFormsValidator validator;

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
    // no-op, not directly invoked
  }

  public void setElement(Element element) {
    String jsonFormsValidator = getJsonFormsValidator(element);
    if (jsonFormsValidator != null) {
      if (StringUtil.isExpression(jsonFormsValidator)) {
        // expression
        ExpressionManager expressionManager = Context
            .getProcessEngineConfiguration()
            .getExpressionManager();
        Expression validatorExpression = expressionManager.createExpression(jsonFormsValidator);
        validator = new DelegateJsonFormsValidator(validatorExpression);
      } else {
        // expecting fully qualified class name
        validator = new DelegateJsonFormsValidator(jsonFormsValidator);
      }
    }
  }

  @Override
  public void submitFormVariables(VariableMap properties,
      VariableScope variableScope) {
    if (validator != null) {
      validator.validate(properties, variableScope);
    }
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

      formKey = transformFormKey(formKey, deploymentName(processDefinition.getDeploymentId()));

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
      formKey = transformFormKey(formKey,
          deploymentName(task.getProcessDefinition().getDeploymentId()));
      ((FormDataImpl) data).setFormKey(formKey);
    }
    return data;
  }

  /**
   * Rewrites a {@code ?deployment=} key to the {@code ?path=} one that serves the same form off
   * disk, when {@link Utils#CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH} says to.
   *
   * The two parameters are alternatives, never both: the rewritten key carries {@code path}
   * alone, so everything downstream reads the form from the path and nothing falls back to a
   * deployment the developer is deliberately bypassing.
   *
   * A key that already names a {@code path} is left as it is - authored that way in the BPMN, it
   * always resolves from the path - and so is one this cannot build a unique path for; see
   * {@link Utils#toPathLocation}.
   *
   * @param deploymentName the deployment holding the form, for a mount that asks for it with
   *                       {@link Utils#CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER}
   */
  protected String transformFormKey(String formKey, String deploymentName) {
    String mount = System.getProperty(
        Utils.CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH);

    if (mount != null && mount.startsWith("/")) {

      int queryStart = formKey.indexOf("?");
      if (queryStart == -1 && queryStart < formKey.length() - 1) {
        return formKey;
      }

      Map<String, List<String>> parameters = Utils.parseQueryString(formKey.substring(queryStart + 1));
      List<String> deployment = parameters.get(Utils.CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT);
      if (deployment == null || deployment.isEmpty()) {
        return formKey;
      }

      String fullPath = Utils.toPathLocation(mount, deploymentName, deployment.get(0));
      if (fullPath != null) {
        parameters.remove(Utils.CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT);
        parameters.put(Utils.CAMUNDA_FORM_KEY_QUERY_PARAM_PATH,
            Collections.singletonList(fullPath));

        formKey = formKey.substring(0, queryStart) + "?" + Utils.toQueryString(parameters);
      }
    }

    if (debugLogEnabled()) {
      formKey = formKey + "&debug=true";
    }

    return formKey;
  }

  /**
   * The name of a deployment - which for a process application is the process archive's name.
   * Null when it cannot be read, which leaves the form key alone rather than building a path
   * that could address another archive's form.
   */
  protected String deploymentName(String deploymentId) {
    if (deploymentId == null) {
      return null;
    }

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext == null) {
      return null;
    }

    DeploymentEntity deployment = commandContext.getDeploymentManager().findDeploymentById(deploymentId);
    return deployment == null ? null : deployment.getName();
  }

  protected boolean debugLogEnabled() {
    return Boolean.valueOf(System.getProperty(
        Utils.CAMUNDA_JSONFORMS_ENABLE_JS_CONSOLE_LOG, "false"));
  }

  protected String getJsonFormsValidator(Element startEventEl) {
    // <bpmn2:extensionElements>
    Element extensionElements = startEventEl.element("extensionElements");
    if (extensionElements == null) {
      return null;
    }

    // <camunda:properties> in Camunda extensions namespace
    Element camundaProperties = extensionElements.elementNS(BpmnParse.CAMUNDA_BPMN_EXTENSIONS_NS, "properties");
    if (camundaProperties == null) {
      return null;
    }

    // Loop through <camunda:property>
    for (Element propertyEl : camundaProperties.elementsNS(BpmnParse.CAMUNDA_BPMN_EXTENSIONS_NS, "property")) {
      String nameAttr = propertyEl.attribute("name");
      if (JSON_FORMS_VALIDATOR_EXTENSION_PROPERTY.equals(nameAttr)) {
        return propertyEl.attribute("value");
      }
    }
    return null;
  }

}