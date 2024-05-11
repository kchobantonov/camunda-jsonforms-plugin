package com.github.kchobantonov.camunda.jsonforms.demo.trace;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.interceptor.CommandInterceptor;
import org.camunda.bpm.spring.boot.starter.configuration.Ordering;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordering.DEFAULT_ORDER + 1)
public class OpenTelemetryPlugin extends AbstractProcessEnginePlugin {

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    // add command interceptor to configuration
    if (OpenTelemetryUtil.isTracingEnabled()) {
      List<CommandInterceptor> customPreCommandInterceptorsTxRequired = processEngineConfiguration
          .getCustomPreCommandInterceptorsTxRequired();
      if (customPreCommandInterceptorsTxRequired == null) {
        customPreCommandInterceptorsTxRequired = new ArrayList<>();
        processEngineConfiguration
            .setCustomPreCommandInterceptorsTxRequired(customPreCommandInterceptorsTxRequired);
      }
      customPreCommandInterceptorsTxRequired.add(new OpenTelemetryCommandInterceptor());

      List<CommandInterceptor> customPreCommandInterceptorsTxRequiresNew = processEngineConfiguration
          .getCustomPreCommandInterceptorsTxRequiresNew();
      if (customPreCommandInterceptorsTxRequiresNew == null) {
        customPreCommandInterceptorsTxRequiresNew = new ArrayList<>();
        processEngineConfiguration
            .setCustomPreCommandInterceptorsTxRequiresNew(customPreCommandInterceptorsTxRequiresNew);
      }
      customPreCommandInterceptorsTxRequiresNew.add(new OpenTelemetryCommandInterceptor());
    }
  }

}
