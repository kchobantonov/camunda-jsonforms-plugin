package com.github.kchobantonov.camunda.jsonforms.plugin;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * Installs {@link JsonFormsTaskService}, so a task completed without a form is still held to
 * the form's schema.
 *
 * Registered the same way {@link JsonFormsFormServicePlugin} registers its service, and it has
 * to be a separate plugin for the same reason that one is: an application should be able to
 * take the form rendering without taking a behaviour change to {@code TaskService.complete},
 * which - until this exists - was the documented way to complete a task without a form.
 */
public class JsonFormsTaskServicePlugin extends AbstractProcessEnginePlugin {

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    processEngineConfiguration.setTaskService(new JsonFormsTaskService());
  }
}
