package com.github.kchobantonov.camunda.jsonforms.config;

import com.github.kchobantonov.camunda.jsonforms.Utils;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
