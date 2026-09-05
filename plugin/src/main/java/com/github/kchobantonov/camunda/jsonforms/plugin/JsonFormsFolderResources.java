package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A folder on disk that JsonForms resources are read from instead of from the Camunda
 * deployment, so a form can be edited and reloaded without redeploying its process.
 *
 * <h2>Switching it on</h2>
 *
 * Two JVM options:
 *
 * <pre>
 * -DCAMUNDA_JSONFORMS_RESOURCES_FOLDER=&lt;checkout&gt;/src/main/resources/processes
 * -DCAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH=/forms/{deployment\}
 * </pre>
 *
 * With neither set nothing is active and every read falls back to the deployment, which is what
 * a deployed environment does. They are JVM options rather than application properties on
 * purpose: this must not be switchable through configuration a deployed environment can supply.
 *
 * An application makes this the source of forms by publishing it as the
 * {@link JsonFormsPathResourceResolver} that {@link JsonFormsFormService} and
 * {@link com.github.kchobantonov.camunda.jsonforms.plugin.validation.DefaultJsonFormsValidator
 * DefaultJsonFormsValidator} read {@code ?path=} form keys with, and by serving
 * {@link #mounts()} as static resources so the renderer in the browser fetches the same files.
 *
 * <h2>Why the mount carries {@code {deployment\}}</h2>
 *
 * A form key names a location relative to its process archive's {@code resourceRootPath} -
 * {@code ?deployment=forms/Start} - so that location is unique only <em>within</em> its archive,
 * while the URL it is served under has no such scope.
 * {@link Utils#CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER} closes that gap by putting the
 * deployment name - for a process application, the archive's name - into the path:
 *
 * <pre>
 * ?deployment=forms/Start  in Orders    -&gt;  /forms/Orders/forms/Start
 * ?deployment=forms/Start  in Invoices  -&gt;  /forms/Invoices/forms/Start
 * </pre>
 *
 * That segment <em>is</em> the subdirectory the form is read from, so the folder must hold one
 * subdirectory per archive, named after it. A mount without the placeholder reads the folder
 * itself, which is what a single archive with no {@code resourceRootPath} wants: its locations
 * are already unique.
 *
 * <h2>Two shapes of name</h2>
 *
 * {@link #resolve(String)} is the {@link JsonFormsPathResourceResolver} contract and takes a URL
 * path off a {@code ?path=} form key, mount and suffix included -
 * {@code /forms/Orders/forms/Start.schema.json}.
 *
 * {@link #open(String, String)} takes the deployment name and the resource name as a deployment
 * holds them - {@code Orders} and {@code forms/Start.schema.json} - for a caller reading a
 * form itself rather than through {@code FormService}. It composes the path with
 * {@link Utils#toPathLocation} and hands it to {@link #resolve(String)}, so both callers resolve
 * a form by exactly one rule.
 */
public class JsonFormsFolderResources implements JsonFormsPathResourceResolver {

    /** The URL path the files are served under, or null when the override is off. */
    private final String mount;

    /** The configured folder, or null when the override is off. */
    private final Path root;

    protected JsonFormsFolderResources(String folder, String mount) {
        this.mount = normaliseMount(mount);
        this.root = this.mount == null ? null : root(folder);
    }

    /** The override as the JVM options describe it. */
    public static JsonFormsFolderResources fromSystemProperties() {
        return new JsonFormsFolderResources(System.getProperty(Utils.CAMUNDA_JSONFORMS_RESOURCES_FOLDER),
                System.getProperty(Utils.CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH));
    }

    /**
     * The override pointed at a given folder and mount, for a test or an application that
     * configures it itself.
     */
    public static JsonFormsFolderResources of(String folder, String mount) {
        return new JsonFormsFolderResources(folder, mount);
    }

    /**
     * The override switched off: every read falls through to the deployment. For a test, and for
     * an application wiring a resolver that is deliberately never consulted.
     */
    public static JsonFormsFolderResources off() {
        return new JsonFormsFolderResources(null, null);
    }

    /** Whether form resources should be looked for on disk at all. */
    public boolean isActive() {
        return root != null;
    }

    /**
     * Whether a read has to say which deployment it is for, because the mount asks for the
     * deployment name. A caller that cannot supply one - and would otherwise read another
     * archive's form of the same name - can then leave the resource to the deployment, which is
     * the choice {@link Utils#toPathLocation} makes too.
     */
    public boolean isDeploymentScoped() {
        return isActive() && mount.contains(Utils.CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER);
    }

    /**
     * One form resource by the name a deployment holds it under, e.g.
     * {@code forms/Start.schema.json}, or null when the override is off or the folder does not
     * hold it - which is not an error: a caller can fall back to the deployment, so only the
     * resources actually being edited need to be present.
     *
     * @param deploymentName the deployment the resource belongs to, which selects the
     *                       subdirectory when {@link #isDeploymentScoped()}; unused otherwise
     */
    public InputStream open(String deploymentName, String resourceName) {
        if (!isActive() || resourceName == null || resourceName.isBlank()) {
            return null;
        }
        return resolve(Utils.toPathLocation(mount, deploymentName, resourceName));
    }

    /**
     * The resolver contract: a URL path taken from a {@code ?path=} form key.
     *
     * The mount is a template, so it is matched as one - what precedes the placeholder is a
     * literal prefix, the segment where the placeholder sits is the deployment, and the rest is
     * the resource name. That is the exact inverse of the rewrite
     * {@link JsonFormsFormHandler#transformFormKey} performs to build such a path.
     */
    @Override
    public InputStream resolve(String path) {
        if (!isActive() || path == null) {
            return null;
        }

        int placeholder = mount.indexOf(Utils.CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER);
        // a mount with no placeholder is a whole path segment and has to match as one, so that
        // "/forms" does not answer for "/formsomething"
        String prefix = placeholder == -1 ? mount + "/" : mount.substring(0, placeholder);
        if (!path.startsWith(prefix)) {
            return null;
        }
        String remainder = strip(path.substring(prefix.length()));

        Path folder = root;
        if (placeholder != -1) {
            int end = remainder.indexOf('/');
            if (end <= 0) {
                return null;
            }
            folder = folder(remainder.substring(0, end));
            // whatever the mount puts after the placeholder is part of the path too, and part of
            // neither name
            remainder = strip(mount.substring(placeholder
                    + Utils.CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER.length())
                    + remainder.substring(end));
        }

        return folder == null || remainder.isEmpty() ? null : read(folder, remainder);
    }

    /**
     * The URL patterns and the folders behind them, for an application to serve as static
     * resources so the renderer in the browser reads the same files this does.
     *
     * One entry per archive folder when the mount asks for a deployment segment, rather than one
     * wildcard: with the placeholder substituted, pattern and folder differ by nothing, so
     * mapping a request onto a file needs no rule of its own.
     */
    public List<Mount> mounts() {
        if (!isActive()) {
            return List.of();
        }

        List<Mount> mounts = new ArrayList<>();
        if (!isDeploymentScoped()) {
            addMount(mounts, mount, root);
            return List.copyOf(mounts);
        }

        for (Path folder : archiveFolders()) {
            addMount(mounts, mount.replace(Utils.CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER,
                    folder.getFileName().toString()), folder);
        }
        return List.copyOf(mounts);
    }

    /**
     * A URL path prefix and the folder served under it.
     *
     * @param pattern  the prefix, with no deployment placeholder left in it
     * @param location the folder as a URL, which is what a static-resource handler wants
     */
    public record Mount(String pattern, String location) {
    }

    /** One resource under one folder, or null when it is not there. */
    protected InputStream read(Path folder, String resourceName) {
        try {
            Path resolved = folder.resolve(resourceName).normalize();
            // the name reaches us from a form key, so it must not be able to address anything
            // outside the folder - normalize() collapses ".." before this check, and an absolute
            // name makes resolve() return that name unchanged, which then fails it
            if (!resolved.startsWith(folder) || !Files.isRegularFile(resolved)) {
                return null;
            }
            return Files.newInputStream(resolved);
        } catch (InvalidPathException | IOException e) {
            return null;
        }
    }

    /** The folder of one deployment, or null when its name cannot name a folder here. */
    protected Path folder(String deploymentName) {
        try {
            Path folder = root.resolve(deploymentName).normalize();
            // a deployment name is one path segment, so it must not walk out of the folder
            return folder.startsWith(root) && Files.isDirectory(folder) ? folder : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** The folder's immediate subdirectories, one per process archive. */
    protected List<Path> archiveFolders() {
        List<Path> folders = new ArrayList<>();
        try (Stream<Path> children = Files.list(root)) {
            children.filter(Files::isDirectory).sorted(Comparator.naturalOrder()).forEach(folders::add);
        } catch (IOException e) {
            return List.of();
        }
        return folders;
    }

    private static void addMount(List<Mount> mounts, String pattern, Path folder) {
        try {
            mounts.add(new Mount(pattern, folder.toUri().toURL().toExternalForm()));
        } catch (MalformedURLException e) {
            // leave this one unserved; its forms come from the deployment
        }
    }

    private static String strip(String path) {
        String stripped = path;
        while (stripped.startsWith("/")) {
            stripped = stripped.substring(1);
        }
        return stripped;
    }

    /**
     * The mount as a URL path with no trailing slash, or null when it cannot be one.
     *
     * A mount that is not an absolute path switches the override off rather than half on:
     * {@link JsonFormsFormHandler#transformFormKey} and {@link Utils#toPathLocation} apply the
     * same test before rewriting a {@code ?deployment=} key to {@code ?path=}, so anything else
     * leaves the library reading the deployment and this has to do the same.
     */
    private static String normaliseMount(String mount) {
        if (mount == null || !mount.startsWith("/")) {
            return null;
        }
        String normalised = mount;
        while (normalised.length() > 1 && normalised.endsWith("/")) {
            normalised = normalised.substring(0, normalised.length() - 1);
        }
        return "/".equals(normalised) ? null : normalised;
    }

    /** The configured folder, or null if it is unusable. */
    private static Path root(String folder) {
        if (folder == null || folder.isBlank()) {
            return null;
        }

        try {
            Path root = Path.of(folder).toAbsolutePath().normalize();
            return Files.isDirectory(root) ? root : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** The folder and mount in force, or {@code "deployment"} when the override is off. */
    @Override
    public String toString() {
        return root == null ? "deployment" : root + " under " + mount;
    }
}
