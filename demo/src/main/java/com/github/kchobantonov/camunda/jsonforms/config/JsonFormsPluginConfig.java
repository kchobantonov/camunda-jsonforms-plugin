package com.github.kchobantonov.camunda.jsonforms.config;

import com.github.kchobantonov.camunda.jsonforms.JsonFormsFormFieldValidatorProcessEnginePlugin;
import com.github.kchobantonov.camunda.jsonforms.JsonFormsFormServicePlugin;
import com.github.kchobantonov.camunda.jsonforms.JsonFormsParseListenerProcessEnginePlugin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    // enable server side validation when custom validator jsonforms is set for any
    // form property - need to have at least one with that custom validator.
    @Bean
    public JsonFormsFormFieldValidatorProcessEnginePlugin jsonFormsFormFieldValidatorProcessEnginePlugin() {
        return new JsonFormsFormFieldValidatorProcessEnginePlugin();
    }

    // do not return process variables that are not defined in the jsonform schema.
    @Bean
    public JsonFormsFormServicePlugin jsonFormsFormServicePlugin() {
        return new JsonFormsFormServicePlugin();
    }

}