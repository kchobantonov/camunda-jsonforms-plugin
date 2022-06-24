package com.github.kchobantonov.camunda.jsonforms.plugin;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.util.Assert;

public class JsonFormsFormServicePlugin extends AbstractProcessEnginePlugin {
    private final JsonFormsPathResourceResolver resolver;

    public JsonFormsFormServicePlugin(JsonFormsPathResourceResolver resolver) {
        Assert.notNull(resolver, "Path resolver shouldn't be null");
        this.resolver = resolver;
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setFormService(new JsonFormsFormService(resolver));
    }

}