export enum AppErrorCode {
  INVALID_CAMUNDA_FORM_CONFIG = 'INVALID_CAMUNDA_FORM_CONFIG',
  INVALID_CAMUNDA_FORM_KEY = 'INVALID_CAMUNDA_FORM_KEY',
  MISSING_JSONFORMS_SCHEMA = 'MISSING_JSONFORMS_SCHEMA',
  MISSING_JSONFORMS_UISCHEMA = 'MISSING_JSONFORMS_UISCHEMA',

  RETRIEVE_JSONFORMS_SCHEMA = 'RETRIEVE_JSONFORMS_SCHEMA',
  INVALID_JSONFORMS_SCHEMA = 'INVALID_JSONFORMS_SCHEMA',
  RETRIEVE_JSONFORMS_UISCHEMA = 'RETRIEVE_JSONFORMS_UISCHEMA',
  INVALID_JSONFORMS_UISCHEMA = 'INVALID_JSONFORMS_UISCHEMA',
  RETRIEVE_JSONFORMS_I18N = 'RETRIEVE_JSONFORMS_I18N',
  INVALID_JSONFORMS_I18N = 'INVALID_JSONFORMS_I18N',

  RETRIEVE_FORM_VARIABLES = 'RETRIEVE_FORM_VARIABLES',
  INVALID_FORM_VARIABLES_RESPONSE = 'INVALID_FORM_VARIABLES_RESPONSE',

  RETRIEVE_DEPLOYMENT_RESOURCES = 'RETRIEVE_DEPLOYMENT_RESOURCES',
  INVALID_DEPLOYMENT_RESOURCES_RESPONSE = 'INVALID_DEPLOYMENT_RESOURCES_RESPONSE',

  RETRIEVE_TASK = 'RETRIEVE_TASK',
  INVALID_TASK_RESPONSE = 'INVALID_TASK_RESPONSE',

  RETRIEVE_PROCESS_DEFINITION = 'RETRIEVE_PROCESS_DEFINITION',
  INVALID_PROCESS_DEFINITION_RESPONSE = 'INVALID_PROCESS_DEFINITION_RESPONSE',
  RETRIEVE_PROCESS_DEFINITION_START_FORM = 'RETRIEVE_PROCESS_DEFINITION_START_FORM',
  INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE = 'INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE',
}

const codeErrors: Record<
  AppErrorCode,
  (params?: Record<string, any>) => string
> = {
  INVALID_CAMUNDA_FORM_CONFIG: (params) => `Invalid configuration`,
  INVALID_CAMUNDA_FORM_KEY: (params) =>
    `Unsupported form key ${params?.formKey}`,
  MISSING_JSONFORMS_SCHEMA: (params) => `Unable to find form schema`,
  MISSING_JSONFORMS_UISCHEMA: (params) => `Unable to find form UI`,

  RETRIEVE_JSONFORMS_SCHEMA: (params) =>
    'Unable to retrieve form schema resource',
  INVALID_JSONFORMS_SCHEMA: (params) => 'Invalid form schema resource',
  RETRIEVE_JSONFORMS_UISCHEMA: (params) =>
    'Unable to retrieve form UI resource',
  INVALID_JSONFORMS_UISCHEMA: (params) => 'Invalid form UI resource',
  RETRIEVE_JSONFORMS_I18N: (params) =>
    'Unable to retrieve form internationalization resource',
  INVALID_JSONFORMS_I18N: (params) =>
    'Invalid form internationalization resource',

  RETRIEVE_FORM_VARIABLES: (params) => 'Unable to retrieve form variables',
  INVALID_FORM_VARIABLES_RESPONSE: (params) =>
    'Invalid form variables response',

  RETRIEVE_DEPLOYMENT_RESOURCES: (params) =>
    'Unable to retrieve deployment resources',
  INVALID_DEPLOYMENT_RESOURCES_RESPONSE: (params) =>
    'Invalid deployment resources response',

  RETRIEVE_TASK: (params) => 'Unable to retrieve task',
  INVALID_TASK_RESPONSE: (params) => 'Invalid task response',

  RETRIEVE_PROCESS_DEFINITION: (params) =>
    'Unable to retrieve process definition',
  INVALID_PROCESS_DEFINITION_RESPONSE: (params) =>
    'Invalid process definition response',
  RETRIEVE_PROCESS_DEFINITION_START_FORM: (params) =>
    'Unable to retrieve process definition start form',
  INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE: (params) =>
    'Invalid process definition start form response',
};
export class AppException extends Error {
  code: string;
  cause?: Error;

  constructor(
    code: AppErrorCode,
    errorParams?: Record<string, any>,
    cause?: Error
  ) {
    super(`${codeErrors[code](errorParams)}`);

    this.name = code;
    this.code = code;
    this.cause = cause;
  }

  toString() {
    return this.message;
  }

  toJSON() {
    const cause = this.cause
      ? Object.prototype.hasOwnProperty.call(this.cause, 'toJSON')
        ? (this.cause as any).toJSON()
        : this.cause.toString()
      : this.cause;
    return {
      message: this.message,
      code: this.code,
      cause: cause,
    };
  }
}
