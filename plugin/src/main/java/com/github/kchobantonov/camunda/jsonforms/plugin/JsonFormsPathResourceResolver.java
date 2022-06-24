package com.github.kchobantonov.camunda.jsonforms.plugin;

import java.io.InputStream;

public interface JsonFormsPathResourceResolver {
  /**
   * Used to locate JsonForms schema, uischema. i18n resources when the path
   * parameter is used
   * 
   * @param path the full path to resolve
   * @return the InputStream of the resource if resolved and null if it can't be
   *         resolved
   * 
   */
  InputStream resolve(String path);
}
