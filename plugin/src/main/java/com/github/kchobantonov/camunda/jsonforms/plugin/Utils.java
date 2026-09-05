package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public final class Utils {
    public static final String CAMUNDA_JSONFORMS_URL = "embedded:app:webjars/forms/jsonforms.html";
    public static final String CUSTOM_FORM_FIELD_VALIDATOR_NAME = "jsonforms";

    public static final String RESOURCE_SCHEMA_SUFFIX = ".schema.json";
    public static final String RESOURCE_UISCHEMA_SUFFIX = ".uischema.json";
    public static final String RESOURCE_I18N_SUFFIX = ".i18n.json";
    public static final String RESOURCE_UISCHEMAS_SUFFIX = ".uischemas.json";
    public static final String RESOURCE_UIDATA_SUFFIX = ".uidata.json";
    public static final String CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT = "deployment";
    public static final String CAMUNDA_FORM_KEY_QUERY_PARAM_PATH = "path";

    /**
     * Placeholder for the deployment name inside
     * {@link #CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH}, substituted when a
     * {@code ?deployment=} form key is rewritten to {@code ?path=}.
     *
     * A deployment location is only unique within its deployment. With one process archive per
     * process - each with its own {@code resourceRootPath} - two archives can hold the same
     * {@code forms/Start}, and the deployment id in a {@code ?deployment=} lookup tells them
     * apart. A URL has no such scope, so a path built as {@code mount + "/" + location} would
     * address both and serve whichever the folder happens to hold.
     *
     * Putting {@code {deployment\}} in the mount puts the deployment name - which is the
     * process archive's name - into the path, making it unique again:
     *
     * <pre>
     * -DCAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH=/forms/{deployment\}
     * ?deployment=forms/Start  -&gt;  ?path=/forms/Invoices/forms/Start
     * </pre>
     *
     * A mount without the placeholder behaves as it always has, which is what a single archive
     * with no {@code resourceRootPath} wants: its locations are already full paths.
     */
    public static final String CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER = "{deployment}";

    public static final String CAMUNDA_JSONFORMS_ENABLE_JS_CONSOLE_LOG = "CAMUNDA_JSONFORMS_ENABLE_JS_CONSOLE_LOG";
    public static final String CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH = "CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH";

    /**
     * The folder {@link JsonFormsFolderResources} reads form resources from, the other half of
     * {@link #CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH}: that one says what URL the resources
     * are served under, this one says where they are read from.
     */
    public static final String CAMUNDA_JSONFORMS_RESOURCES_FOLDER = "CAMUNDA_JSONFORMS_RESOURCES_FOLDER";

    private Utils() {
    }

    public static String toQueryString(Map<String, List<String>> queryParams) {
        StringBuilder result = new StringBuilder();
        for (final Map.Entry<String, List<String>> e : queryParams.entrySet()) {
            final String name = e.getKey();

            for (final String value : e.getValue()) {
                if (result.length() > 0) {
                    result.append('&');
                }
                result.append(name).append('=').append(value);
            }
        }
        return result.toString();
    }

    public static Map<String, List<String>> parseQueryString(String s) {
        Map<String, List<String>> ht = new HashMap<>();
        StringTokenizer st = new StringTokenizer(s, "&");
        while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            int pos = pair.indexOf('=');
            List<String> values = ht.computeIfAbsent(pair.substring(0, pos), (key) -> new ArrayList<>());
            if (pos == -1) {
                values.add("");
            } else {
                try {
                    values.add(URLDecoder.decode(pair.substring(pos + 1), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return ht;
    }

    /**
     * The URL path a {@code ?deployment=} location is served under, or null when the mount is
     * unusable - not an absolute path, or naming a deployment that cannot be identified.
     *
     * @param mount          the configured mount, which may contain
     *                       {@link #CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER}
     * @param deploymentName the deployment holding the form, needed only when the mount asks
     *                       for it
     * @param location       the deployment location from the form key, e.g. {@code forms/Start}
     */
    public static String toPathLocation(String mount, String deploymentName, String location) {
        if (mount == null || !mount.startsWith("/") || location == null) {
            return null;
        }

        String resolved = mount;
        if (resolved.contains(CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER)) {
            if (deploymentName == null || deploymentName.isEmpty()) {
                // the mount asks for a segment we cannot supply; serving the path without it
                // would address another archive's form of the same name
                return null;
            }
            resolved = resolved.replace(CAMUNDA_FORM_KEY_PATH_DEPLOYMENT_PLACEHOLDER, deploymentName);
        }

        return resolved + (resolved.endsWith("/") ? "" : "/") + location;
    }

    public static String getDeploymentLocation(String formKey) {
        return getQueryParamValue(formKey, Utils.CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT);
    }

    public static String getPathLocation(String formKey) {
        return getQueryParamValue(formKey, Utils.CAMUNDA_FORM_KEY_QUERY_PARAM_PATH);
    }

    public static String getQueryParamValue(String formKey, String queryParam) {
        int queryStart = formKey.indexOf("?");
        if (queryStart == -1 && queryStart < formKey.length() - 1) {
            return null;
        }

        Map<String, List<String>> parameters = Utils.parseQueryString(formKey.substring(queryStart + 1));
        List<String> value = parameters.get(queryParam);
        if (value == null || value.isEmpty()) {
            return null;
        }

        return value.get(0);
    }
}
