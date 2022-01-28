package com.github.kchobantonov.camunda.jsonforms;

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

    public static final String ENABLE_JSONFORMS_JS_CONSOLE_LOG = "ENABLE_JSONFORMS_JS_CONSOLE_LOG";

    @Override
    public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
        TaskDefinition taskDefinition = ((UserTaskActivityBehavior) activity.getActivityBehavior()).getTaskDefinition();
        TaskFormHandler handler = taskDefinition.getTaskFormHandler();
        if (handler != null) {
            taskDefinition.setTaskFormHandler(new JsonFormsFormHandler(handler));
        }
    }

    @Override
    public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl startEventActivity) {
        if (scope instanceof ProcessDefinitionEntity) {
            ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity)scope;
            StartFormHandler handler = processDefinition.getStartFormHandler();
            if (handler != null) {
                processDefinition.setStartFormHandler(new JsonFormsFormHandler(handler));
            }
        }
    }

    protected boolean debugLogEnabled() {
        return Boolean.valueOf(System.getProperty(ENABLE_JSONFORMS_JS_CONSOLE_LOG, "false"));
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
        public void parseConfiguration(Element activityElement, DeploymentEntity deployment,
                ProcessDefinitionEntity processDefinition, BpmnParse bpmnParse) {
            if (startFormHandler != null) {
                startFormHandler.parseConfiguration(activityElement, deployment, processDefinition, bpmnParse);
            } else {
                taskFormHandler.parseConfiguration(activityElement, deployment, processDefinition, bpmnParse);
            }
        }

        @Override
        public void submitFormVariables(VariableMap properties, VariableScope variableScope) {
            if (startFormHandler != null) {
                startFormHandler.submitFormVariables(properties, variableScope);
            } else {
                taskFormHandler.submitFormVariables(properties, variableScope);
            }
        }

        @Override
        public StartFormData createStartFormData(ProcessDefinitionEntity processDefinition) {
            StartFormData data = startFormHandler.createStartFormData(processDefinition);
            if (JsonFormsParseListener.this.debugLogEnabled()) {
                String formKey = data.getFormKey();
                if (formKey != null && formKey.startsWith("embedded:app:webjars/forms/jsonforms.html?") && data instanceof FormDataImpl) {
                    formKey = formKey + "&debug=true";
                    ((FormDataImpl)data).setFormKey(formKey);
                }
            }
            return data;
        }

        @Override
        public TaskFormData createTaskForm(TaskEntity task) {
            TaskFormData  data = taskFormHandler.createTaskForm(task);
            if (JsonFormsParseListener.this.debugLogEnabled()) {
                String formKey = data.getFormKey();
                if (formKey != null && formKey.startsWith("embedded:app:webjars/forms/jsonforms.html?") && data instanceof FormDataImpl) {
                    formKey = formKey + "&debug=true";
                    ((FormDataImpl)data).setFormKey(formKey);
                }
            }
            return data;
        }
        
    }
}
