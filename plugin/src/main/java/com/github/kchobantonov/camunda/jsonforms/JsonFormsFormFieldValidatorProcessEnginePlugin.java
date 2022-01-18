package com.github.kchobantonov.camunda.jsonforms;

import java.util.HashMap;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;

public class JsonFormsFormFieldValidatorProcessEnginePlugin implements ProcessEnginePlugin {
    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (processEngineConfiguration.getCustomFormFieldValidators() == null) {
            processEngineConfiguration.setCustomFormFieldValidators(new HashMap<>());
        }
        processEngineConfiguration.getCustomFormFieldValidators().put("jsonforms", JsonFormsFormFieldValidator.class);
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {

    }
}
