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

  /**
   * The form's validator - one, whichever way the task declares it.
   *
   * Either a {@code <camunda:property name="jsonFormsValidator">} on the task, or, failing
   * that, the {@code <camunda:constraint name="validator">} on one of its form fields - the way
   * every BPMN in practice declares one, and the reason
   * {@link com.github.kchobantonov.camunda.jsonforms.plugin.validation.DefaultJsonFormsValidator
   * DefaultJsonFormsValidator} implements {@code FormFieldValidator} at all. A form validates
   * its whole submission against its schema, so a second declaration would only ever repeat the
   * first; the first one found wins.
   */
  private DelegateJsonFormsValidator validator;

  /**
   * Whether {@link #validator} came from a form field constraint rather than from the property.
   *
   * It decides one thing: {@link #submitFormVariables} skips such a validator, because on that
   * path Camunda's own {@code DefaultFormHandler} runs the field constraints itself and running
   * it here as well would validate every submitted form twice. {@link #validate} runs it either
   * way, that being the path Camunda has no form handler on.
   */
  private boolean validatorFromFormField;

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
      validator = toValidator(jsonFormsValidator);
    }
    if (validator == null) {
      String fieldValidator = getFormFieldValidator(element);
      if (fieldValidator != null) {
        validator = toValidator(fieldValidator);
        validatorFromFormField = true;
      }
    }
  }

  private DelegateJsonFormsValidator toValidator(String configuration) {
    if (StringUtil.isExpression(configuration)) {
      // expression
      ExpressionManager expressionManager = Context
          .getProcessEngineConfiguration()
          .getExpressionManager();
      Expression validatorExpression = expressionManager.createExpression(configuration);
      return new DelegateJsonFormsValidator(validatorExpression);
    }
    // expecting fully qualified class name
    return new DelegateJsonFormsValidator(configuration);
  }

  @Override
  public void submitFormVariables(VariableMap properties,
      VariableScope variableScope) {
    if (!validatorFromFormField) {
      validate(properties, variableScope);
    }
    if (startFormHandler != null) {
      startFormHandler.submitFormVariables(properties, variableScope);
    } else {
      taskFormHandler.submitFormVariables(properties, variableScope);
    }
  }

  /**
   * Runs this form's validator against a submission, without submitting it.
   *
   * Split out of {@link #submitFormVariables} so a completion that does not go through the
   * form service can still be held to the form's schema. {@code TaskService.complete} runs no
   * form handler at all - that is Camunda's design, not an oversight - so every schema in the
   * application was enforced only on the {@code FormService.submitTaskForm} path, and any
   * caller reaching for the more obvious {@code complete} silently skipped all of it.
   * {@link JsonFormsTaskService} closes that by calling this first.
   *
   * A no-op when the task declares no {@code validator} constraint, so a process that never
   * opted into JsonForms validation is unaffected.
   */
  public void validate(VariableMap properties, VariableScope variableScope) {
    if (validator != null) {
      validator.validate(properties, variableScope);
    }
  }

  /** Whether this form has anything to validate against - see {@link #validate}. */
  public boolean hasValidator() {
    return validator != null;
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

  /**
   * The {@code validator} constraint declared on the task's form fields, or null if it declares
   * none.
   *
   * <pre>
   * &lt;camunda:formData&gt;
   *   &lt;camunda:formField id="..."&gt;
   *     &lt;camunda:validation&gt;
   *       &lt;camunda:constraint name="validator" config="${jsonFormsValidator}"/&gt;
   * </pre>
   *
   * Camunda parses these into its own form-field validators and runs them on
   * {@code submitTaskForm}. Reading one again here is what lets {@link #validate} apply the
   * same rule on a path where Camunda runs no form handler at all - without which this class
   * would only ever have validated the {@code camunda:property} form of the declaration, which
   * is not the one anybody writes.
   *
   * The first one found is the answer: the validator is handed the whole submission and checks
   * it against the form's schema, so it is a property of the form rather than of the field it
   * happens to hang off, and declaring it on a second field would ask for the identical check
   * twice.
   */
  protected String getFormFieldValidator(Element taskElement) {
    Element extensionElements = taskElement.element("extensionElements");
    if (extensionElements == null) {
      return null;
    }
    Element formData = extensionElements.elementNS(BpmnParse.CAMUNDA_BPMN_EXTENSIONS_NS, "formData");
    if (formData == null) {
      return null;
    }
    for (Element formField : formData.elementsNS(BpmnParse.CAMUNDA_BPMN_EXTENSIONS_NS, "formField")) {
      Element validation = formField.elementNS(BpmnParse.CAMUNDA_BPMN_EXTENSIONS_NS, "validation");
      if (validation == null) {
        continue;
      }
      for (Element constraint : validation.elementsNS(BpmnParse.CAMUNDA_BPMN_EXTENSIONS_NS, "constraint")) {
        if (Utils.CUSTOM_FORM_FIELD_VALIDATOR_CONSTRAINT.equals(constraint.attribute("name"))) {
          String configuration = constraint.attribute("config");
          if (configuration != null && !configuration.trim().isEmpty()) {
            return configuration;
          }
        }
      }
    }
    return null;
  }

}