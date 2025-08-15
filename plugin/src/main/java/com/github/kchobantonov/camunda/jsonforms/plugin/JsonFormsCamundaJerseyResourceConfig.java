package com.github.kchobantonov.camunda.jsonforms.plugin;

import org.camunda.bpm.engine.rest.impl.CamundaRestResources;
import org.camunda.bpm.spring.boot.starter.rest.CamundaJerseyResourceConfig;

public class JsonFormsCamundaJerseyResourceConfig extends CamundaJerseyResourceConfig {

  static {
    CamundaRestResources.getConfigurationClasses().add(JsonFormsRestExceptionHandler.class);
  }

  @Override
  protected void registerAdditionalResources() {
    super.registerAdditionalResources();
    this.register(JsonFormsRestExceptionHandler.class);
  }
}