package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.util.HashMap;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * Plugin that will register a custom validation. If you are using the path
 * query parameter to locate schema, uischema, i18n resource then make sure that
 * you invoke
 * {@link JsonFormsFormFieldValidator.setJsonFormsPathResourceResolver}
 */
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
