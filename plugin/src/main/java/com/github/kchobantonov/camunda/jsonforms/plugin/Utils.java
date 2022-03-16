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
    public static final String CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT = "deployment";
    public static final String CAMUNDA_FORM_KEY_QUERY_PARAM_PATH = "path";

    public static final String CAMUNDA_JSONFORMS_ENABLE_JS_CONSOLE_LOG = "CAMUNDA_JSONFORMS_ENABLE_JS_CONSOLE_LOG";
    public static final String CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH = "CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH";

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

    public static String getFormFile(String formKey) {
        int queryStart = formKey.indexOf("?");
        if (queryStart == -1 && queryStart < formKey.length() - 1) {
            return null;
        }

        Map<String, List<String>> parameters = Utils.parseQueryString(formKey.substring(queryStart + 1));
        List<String> deployment = parameters.get(Utils.CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT);
        if (deployment == null || deployment.isEmpty()) {
            return null;
        }

        return deployment.get(0);
    }
}
