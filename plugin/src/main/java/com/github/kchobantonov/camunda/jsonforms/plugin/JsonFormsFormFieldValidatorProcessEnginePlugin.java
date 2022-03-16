package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.HashMap;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

public class JsonFormsFormFieldValidatorProcessEnginePlugin extends AbstractProcessEnginePlugin {

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (processEngineConfiguration.getCustomFormFieldValidators() == null) {
            processEngineConfiguration.setCustomFormFieldValidators(new HashMap<>());
        }
        processEngineConfiguration.getCustomFormFieldValidators().put(Utils.CUSTOM_FORM_FIELD_VALIDATOR_NAME,
                JsonFormsFormFieldValidator.class);
    }

}
