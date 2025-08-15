package com.github.kchobantonov.camunda.jsonforms.plugin;

import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.form.handler.StartFormHandler;
import org.camunda.bpm.engine.impl.form.handler.TaskFormHandler;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.util.xml.Element;

public class JsonFormsParseListener extends AbstractBpmnParseListener {

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope,
      ActivityImpl activity) {
    TaskDefinition taskDefinition = ((UserTaskActivityBehavior) activity.getActivityBehavior())
        .getTaskDefinition();
    TaskFormHandler handler = taskDefinition.getTaskFormHandler();
    if (handler != null) {
      JsonFormsFormHandler jsonFormsHandler = new JsonFormsFormHandler(handler);
      jsonFormsHandler.setElement(userTaskElement);
      taskDefinition.setTaskFormHandler(jsonFormsHandler);
    }
  }

  @Override
  public void parseStartEvent(Element startEventElement, ScopeImpl scope,
      ActivityImpl startEventActivity) {
    if (scope instanceof ProcessDefinitionEntity) {
      ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) scope;
      StartFormHandler handler = processDefinition.getStartFormHandler();
      if (handler != null) {
        JsonFormsFormHandler jsonFormsHandler = new JsonFormsFormHandler(handler);
        jsonFormsHandler.setElement(startEventElement);
        processDefinition.setStartFormHandler(jsonFormsHandler);
      }
    }
  }

}
