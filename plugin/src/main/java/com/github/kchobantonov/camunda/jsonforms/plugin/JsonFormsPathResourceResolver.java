package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.io.InputStream;

public interface JsonFormsPathResourceResolver {
  /**
   * Locates one JsonForms resource - schema, uischema, uischemas, uidata or i18n - when a form
   * key names a {@code path} rather than a {@code deployment}.
   *
   * @param path the resource's full path, suffix included: the {@code path} query parameter of
   *             the form key plus one of {@link Utils#RESOURCE_SCHEMA_SUFFIX},
   *             {@link Utils#RESOURCE_UISCHEMA_SUFFIX},
   *             {@link Utils#RESOURCE_UISCHEMAS_SUFFIX},
   *             {@link Utils#RESOURCE_UIDATA_SUFFIX} or
   *             {@link Utils#RESOURCE_I18N_SUFFIX}. It is the same path a browser would fetch,
   *             so it starts with the mount configured as
   *             {@link Utils#CAMUNDA_JSONFORMS_LOAD_RESOURCES_FROM_PATH} and an implementation
   *             serving files from a folder has to strip that prefix.
   * @return the InputStream of the resource if resolved and null if it can't be
   *         resolved
   *
   */
  InputStream resolve(String path);
}
