package com.github.kchobantonov.camunda.jsonforms.plugin;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;

public class JsonFormsFormServicePlugin extends AbstractProcessEnginePlugin {
    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setFormService(new JsonFormsFormService());
    }

}
