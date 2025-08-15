package com.github.kchobantonov.camunda.jsonforms.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Enables Camunda JSON Forms plugin integration.
 * 
 * Add this annotation to a Spring Boot application
 * to register the JSON Forms beans.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(JsonFormsPluginConfig.class)
public @interface EnableJsonForms {
}
