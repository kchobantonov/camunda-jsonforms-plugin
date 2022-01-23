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

        @Bean
        public JsonFormsParseListenerProcessEnginePlugin jsonFormsParseListenerProcessEnginePlugin() {
            return new JsonFormsParseListenerProcessEnginePlugin();
        }

        @Bean
        public JsonFormsFormFieldValidatorProcessEnginePlugin jsonFormsFormFieldValidatorProcessEnginePlugin() {
            return new JsonFormsFormFieldValidatorProcessEnginePlugin();
        }

        @Bean
        public JsonFormsFormServicePlugin jsonFormsFormServicePlugin() {
            return new JsonFormsFormServicePlugin();
        }
    }

}
