package com.github.kchobantonov.camunda.jsonforms.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFolderResources;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsFolderResources.Mount;
import com.github.kchobantonov.camunda.jsonforms.plugin.JsonFormsPathResourceResolver;
import com.github.kchobantonov.camunda.jsonforms.plugin.Utils;

/**
 * Serves JsonForms resources from a folder on disk, so a form can be edited and reloaded without
 * redeploying its process archive.
 *
 * Switched on with two JVM options - see the launch configurations in {@code .vscode}:
 *
 * <pre>
 * -DCAMUNDA_JSONFORMS_RESOURCES_FOLDER=&lt;checkout&gt;/demo/src/main/resources
 * -DCAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH=/forms
 * </pre>
 *
 * {@link JsonFormsFolderResources} does the reading; this wires it up as the two things an
 * application has to provide. As the {@link JsonFormsPathResourceResolver} bean it is what the
 * form service and the schema validator read a {@code ?path=} form key with, and its
 * {@link JsonFormsFolderResources#mounts()} are registered as static resources so the renderer
 * in Tasklist fetches the very same files.
 *
 * The mount carries no {@code {deployment\}} segment because this demo deploys one process
 * archive with no {@code resourceRootPath}: its deployment locations are already full paths
 * under the folder, so they are unique on their own and the folder needs no per-archive
 * subdirectory.
 *
 * With one archive per process folder - each with its own {@code resourceRootPath} - two
 * archives could both hold a {@code forms/Start}, and a path built without the archive name
 * would address both. A mount carrying the placeholder keeps them apart, the folder then
 * holding one subdirectory per archive, named after it:
 *
 * <pre>
 * ?deployment=forms/Start  in Orders    -&gt;  /forms/Orders/forms/Start
 * ?deployment=forms/Start  in Invoices  -&gt;  /forms/Invoices/forms/Start
 * </pre>
 *
 * @see Utils#CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER
 */
@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    /**
     * Read once, at construction. The handlers below are registered once too, so a folder that
     * appeared later would be half-visible - and an archive folder added while the application
     * is running is a restart either way.
     */
    private final JsonFormsFolderResources localForms = JsonFormsFolderResources.fromSystemProperties();

    /** One handler per archive, so the browser reads the identical files the server side does. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (Mount mount : localForms.mounts()) {
            registry.addResourceHandler(mount.pattern() + "/**")
                    .addResourceLocations(mount.location())
                    // no caching, so an edit shows up on reload
                    .setCachePeriod(0);
        }
    }

    @Bean
    public JsonFormsPathResourceResolver jsonFormsPathResourceResolver() {
        return localForms;
    }
}
