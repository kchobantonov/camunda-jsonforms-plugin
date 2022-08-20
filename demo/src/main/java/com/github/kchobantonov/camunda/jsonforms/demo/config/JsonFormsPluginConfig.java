package com.github.kchobantonov.camunda.jsonforms.demo.config;

import org.camunda.bpm.spring.boot.starter.rest.CamundaJerseyResourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFomsCamundaJerseyResourceConfig;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFormFieldValidator;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFormServicePlugin;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsParseListenerProcessEnginePlugin;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsPathResourceResolver;

@Configuration
class JsonFormsPluginConfig {

    // enable JS logs for all forms by using JVM option
    // -DCAMUNDA_JSONFORMS_ENABLE_JS_CONSOLE_LOG=true
    // enable loading forms from application path instead from deployment using JVM
    // option (make sure that the form resources are available from that path
    // location)
    //
    // -DCAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH=/somepath
    @Bean
    public JsonFormsParseListenerProcessEnginePlugin jsonFormsParseListenerProcessEnginePlugin() {
        return new JsonFormsParseListenerProcessEnginePlugin();
    }

    // do not return process variables that are not defined in the jsonform schema.
    @Bean
    public JsonFormsFormServicePlugin jsonFormsFormServicePlugin(JsonFormsPathResourceResolver resolver) {
        return new JsonFormsFormServicePlugin(resolver);
    }

    @Bean
    public JsonFormsFormFieldValidator jsonFormsValidator(JsonFormsPathResourceResolver resolver) {
        return new JsonFormsFormFieldValidator(resolver);
    }

    @Bean
    public CamundaJerseyResourceConfig createRestConfig() {
        return new JsonFomsCamundaJerseyResourceConfig();
    }

}