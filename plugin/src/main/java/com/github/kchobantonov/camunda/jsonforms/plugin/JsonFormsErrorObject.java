package com.github.kchobantonov.camunda.jsonforms.plugin;

public class JsonFormsErrorObject {
  /**
   * validation keyword.
   */
  String keyword;
  /**
   * JSON Pointer to the location in the data instance (e.g.,
   * `"/prop/1/subProp"`).
   */
  String instancePath;
  /**
   * JSON Pointer to the location of the failing keyword in the schema
   */
  String schemaPath;

  /**
   * type is defined by keyword value, see below
   * params property is the object with the additional information about error
   * it can be used to generate error messages
   * (e.g., using [ajv-i18n](https://github.com/ajv-validator/ajv-i18n) package).
   * See below for parameters set by all keywords.
   */
  Object params;

  /**
   * set for errors in `propertyNames` keyword schema.
   * `instancePath` still points to the object in this case.
   */
  String propertyName;

  /**
   * the error message (can be excluded with option `messages: false`).
   */
  String message;

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

  /**
   * @return the keyword
   */
  public String getKeyword() {
    return keyword;
  }

  /**
   * @param keyword the keyword to set
   */
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  /**
   * @return the instancePath
   */
  public String getInstancePath() {
    return instancePath;
  }

  /**
   * @param instancePath the instancePath to set
   */
  public void setInstancePath(String instancePath) {
    this.instancePath = instancePath;
  }

  /**
   * @return the schemaPath
   */
  public String getSchemaPath() {
    return schemaPath;
  }

  /**
   * @param schemaPath the schemaPath to set
   */
  public void setSchemaPath(String schemaPath) {
    this.schemaPath = schemaPath;
  }

  /**
   * @return the params
   */
  public Object getParams() {
    return params;
  }

  /**
   * @param params the params to set
   */
  public void setParams(Object params) {
    this.params = params;
  }

  /**
   * @return the propertyName
   */
  public String getPropertyName() {
    return propertyName;
  }

  /**
   * @param propertyName the propertyName to set
   */
  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param message the message to set
   */
  public void setMessage(String message) {
    this.message = message;
  }
}
