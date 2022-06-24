package com.github.kchobantonov.camunda.jsonforms.demo.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsPathResourceResolver;
import com.github.kchobantonov.camunda.jsonforms.plugin.Utils;

@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {
    private String resourceRoot = null;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String sourceFolderPath = System.getProperty("CAMUNDA_JSONFORMS_RESOURCES_FOLDER");
        String path = System.getProperty(Utils.CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH);

        if (sourceFolderPath != null) {

            File sourceFolder = new File(sourceFolderPath);

            if (sourceFolder.exists() && sourceFolder.isDirectory() && path != null) {
                try {
                    resourceRoot = sourceFolder.toURI().toURL().toExternalForm();
                    registry
                            .addResourceHandler(path + "/**")
                            .addResourceLocations(resourceRoot)
                            .setCachePeriod(0);
                } catch (MalformedURLException e) {
                    resourceRoot = null;
                }
            }
        }
    }

    @Bean
    public JsonFormsPathResourceResolver jsonFormsPathResourceResolver() {
        return new StaticResourceResolver();
    }

    private class StaticResourceResolver implements JsonFormsPathResourceResolver {

        @Override
        public InputStream resolve(String path) {
            if (resourceRoot != null) {
                try {
                    return new FileInputStream(Paths.get(resourceRoot, path).toAbsolutePath().toFile());
                } catch (FileNotFoundException e) {
                    return null;
                }
            }

            return null;
        }
    }
}