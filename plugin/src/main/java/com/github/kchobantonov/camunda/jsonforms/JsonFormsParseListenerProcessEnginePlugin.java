package com.github.kchobantonov.camunda.jsonforms;

import java.util.ArrayList;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;

public class JsonFormsParseListenerProcessEnginePlugin implements ProcessEnginePlugin {
    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (processEngineConfiguration.getCustomPreBPMNParseListeners() == null) {
            processEngineConfiguration.setCustomPreBPMNParseListeners(new ArrayList<>());
        }
        processEngineConfiguration.getCustomPreBPMNParseListeners().add(new JsonFormsParseListener());
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
    }
}
