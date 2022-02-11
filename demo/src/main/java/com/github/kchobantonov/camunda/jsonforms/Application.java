package com.github.kchobantonov.camunda.jsonforms;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableProcessApplication
public class Application {

    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    public class StaticResourceConfiguration implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            String sourceFolder = System.getProperty("CAMUNDA_JSONFORMS_RESOURCES_FOLDER");
            String path = System.getProperty(Utils.CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH);

            if (sourceFolder != null && path != null) {
                registry
                        .addResourceHandler(path + "/**")
                        .addResourceLocations("file://" + sourceFolder + "/")
                        .setCachePeriod(0);
            }
        }
    }

    @Configuration
    class Config {

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

}
