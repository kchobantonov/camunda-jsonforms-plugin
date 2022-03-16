package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.ArrayList;

import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;

public class JsonFormsParseListenerProcessEnginePlugin extends AbstractProcessEnginePlugin  {
    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (processEngineConfiguration.getCustomPreBPMNParseListeners() == null) {
            processEngineConfiguration.setCustomPreBPMNParseListeners(new ArrayList<>());
        }
        processEngineConfiguration.getCustomPreBPMNParseListeners().add(new JsonFormsParseListener());
    }
}
