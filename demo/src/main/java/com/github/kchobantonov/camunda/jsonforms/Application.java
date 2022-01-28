package com.github.kchobantonov.camunda.jsonforms;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@EnableProcessApplication
public class Application {

    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    class Config {

        // enable JS logs for all forms by using JVM option
        // -DENABLE_JSONFORMS_JS_CONSOLE_LOG=true
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

}
