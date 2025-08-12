package com.github.kchobantonov.camunda.jsonforms.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JsonFormsErrorObject {
  /**
   * validation keyword.
   */
  private String keyword;
  /**
   * JSON Pointer to the location in the data instance (e.g.,
   * `"/prop/1/subProp"`).
   */
  private String instancePath;
  /**
   * JSON Pointer to the location of the failing keyword in the schema
   */
  private String schemaPath;

  /**
   * type is defined by keyword value, see below
   * params property is the object with the additional information about error
   * it can be used to generate error messages
   * (e.g., using [ajv-i18n](https://github.com/ajv-validator/ajv-i18n) package).
   * See below for parameters set by all keywords.
   */
  private Object params;

  /**
   * set for errors in `propertyNames` keyword schema.
   * `instancePath` still points to the object in this case.
   */
  private String propertyName;

  /**
   * the error message (can be excluded with option `messages: false`).
   */
  private String message;

  public JsonFormsErrorObject(String keyword, String instancePath, String schemaPath, String message) {
    this.keyword = keyword;
    this.instancePath = instancePath;
    this.schemaPath = schemaPath;
    this.message = message;
  }

  public JsonFormsErrorObject(String instancePath, String message) {
    this.instancePath = instancePath;
    this.message = message;
  }

}
