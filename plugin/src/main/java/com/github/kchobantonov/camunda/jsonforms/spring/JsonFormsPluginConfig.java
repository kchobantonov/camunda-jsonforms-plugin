package com.github.kchobantonov.camunda.jsonforms.spring;

import org.camunda.bpm.spring.boot.starter.rest.CamundaJerseyResourceConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsCamundaJerseyResourceConfig;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFormServicePlugin;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsParseListenerProcessEnginePlugin;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsPathResourceResolver;
import com.github.kchobantonov.camunda.jsonforms.plugin.validation.DefaultJsonFormsValidator;
import com.github.kchobantonov.camunda.jsonforms.plugin.validation.JsonFormsValidator;

@Configuration(proxyBeanMethods = false)
public class JsonFormsPluginConfig {

    // enable JS logs for all forms by using JVM option
    // -DCAMUNDA_JSONFORMS_ENABLE_JS_CONSOLE_LOG=true
    // enable loading forms from application path instead from deployment using JVM
    // option (make sure that the form resources are available from that path
    // location)
    //
    // -DCAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH=/somepath
    
    /**
     * Registers the Camunda JSON Forms parse listener plugin.
     */
    @Bean
    @ConditionalOnMissingBean(JsonFormsParseListenerProcessEnginePlugin.class)
    public JsonFormsParseListenerProcessEnginePlugin jsonFormsParseListenerProcessEnginePlugin() {
        return new JsonFormsParseListenerProcessEnginePlugin();
    }

    /**
     * Registers the JSON Forms form service plugin.
     * Ensures only variables defined in the JSON schema are returned.
     */
    @Bean
    @ConditionalOnMissingBean(JsonFormsFormServicePlugin.class)
    public JsonFormsFormServicePlugin jsonFormsFormServicePlugin(JsonFormsPathResourceResolver resolver) {
        return new JsonFormsFormServicePlugin(resolver);
    }

    /**
     * Registers the JSON Forms validator unless one is already defined.
     */
    @Bean(name = "jsonFormsValidator")
    @ConditionalOnMissingBean(name = "jsonFormsValidator")
    public JsonFormsValidator jsonFormsValidator(JsonFormsPathResourceResolver resolver) {
        return new DefaultJsonFormsValidator(resolver);
    }

    /**
     * Registers the Camunda Jersey REST resource config for JSON Forms.
     */
    @Bean
    @ConditionalOnMissingBean(CamundaJerseyResourceConfig.class)
    public CamundaJerseyResourceConfig jsonFormsCamundaJerseyResourceConfig() {
        return new JsonFormsCamundaJerseyResourceConfig();
    }
}