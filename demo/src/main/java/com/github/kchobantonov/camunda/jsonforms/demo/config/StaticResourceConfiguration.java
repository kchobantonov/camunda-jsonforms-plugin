package com.github.kchobantonov.camunda.jsonforms.demo.config;

import java.io.File;
import java.net.MalformedURLException;

import com.github.kchobantonov.camunda.jsonforms.plugin.Utils;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String sourceFolderPath = System.getProperty("CAMUNDA_JSONFORMS_RESOURCES_FOLDER");
        String path = System.getProperty(Utils.CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH);

        if (sourceFolderPath != null) {

            File sourceFolder = new File(sourceFolderPath);

            if (sourceFolder.exists() && sourceFolder.isDirectory() && path != null) {
                try {
                    registry
                            .addResourceHandler(path + "/**")
                            .addResourceLocations(sourceFolder.toURI().toURL().toExternalForm())
                            .setCachePeriod(0);
                } catch (MalformedURLException e) {
                }
            }
        }
    }
}
