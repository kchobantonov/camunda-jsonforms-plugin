export enum AppErrorCode {
  INVALID_CAMUNDA_FORM_CONFIG = 'INVALID_CAMUNDA_FORM_CONFIG',
  INVALID_CAMUNDA_FORM_KEY = 'INVALID_CAMUNDA_FORM_KEY',
  INVALID_CAMUNDA_FORM_KEY_PATH_PARAM = 'INVALID_CAMUNDA_FORM_KEY_PATH_PARAM',
  INVALID_CAMUNDA_FORM_KEY_DEPLOYMENT_PARAM = 'INVALID_CAMUNDA_FORM_KEY_DEPLOYMENT_PARAM',
  MISSING_JSONFORMS_SCHEMA = 'MISSING_JSONFORMS_SCHEMA',
  MISSING_JSONFORMS_UISCHEMA = 'MISSING_JSONFORMS_UISCHEMA',

  RETRIEVE_JSONFORMS_SCHEMA = 'RETRIEVE_JSONFORMS_SCHEMA',
  INVALID_JSONFORMS_SCHEMA = 'INVALID_JSONFORMS_SCHEMA',
  RETRIEVE_JSONFORMS_UISCHEMA = 'RETRIEVE_JSONFORMS_UISCHEMA',
  INVALID_JSONFORMS_UISCHEMA = 'INVALID_JSONFORMS_UISCHEMA',
  RETRIEVE_JSONFORMS_I18N = 'RETRIEVE_JSONFORMS_I18N',
  INVALID_JSONFORMS_I18N = 'INVALID_JSONFORMS_I18N',

  RETRIEVE_TASK_FORM_VARIABLES = 'RETRIEVE_TASK_FORM_VARIABLES',
  INVALID_TASK_FORM_VARIABLES_RESPONSE = 'INVALID_TASK_FORM_VARIABLES_RESPONSE',

  RETRIEVE_PROCESS_DEFINITION_FORM_VARIABLES = 'RETRIEVE_PROCESS_DEFINITION_FORM_VARIABLES',
  INVALID_PROCESS_DEFINITION_FORM_VARIABLES_RESPONSE = 'INVALID_PROCESS_DEFINITION_FORM_VARIABLES_RESPONSE',

  RETRIEVE_DEPLOYMENT_RESOURCES = 'RETRIEVE_DEPLOYMENT_RESOURCES',
  INVALID_DEPLOYMENT_RESOURCES_RESPONSE = 'INVALID_DEPLOYMENT_RESOURCES_RESPONSE',

  RETRIEVE_TASK = 'RETRIEVE_TASK',
  INVALID_TASK_RESPONSE = 'INVALID_TASK_RESPONSE',

  RETRIEVE_TASK_FORM = 'RETRIEVE_TASK_FORM',
  INVALID_TASK_FORM_RESPONSE = 'INVALID_TASK_FORM_RESPONSE',

  RETRIEVE_TASK_DEPLOYED_FORM = 'RETRIEVE_TASK_DEPLOYED_FORM',
  INVALID_TASK_DEPLOYED_FORM_RESPONSE = 'INVALID_TASK_DEPLOYED_FORM_RESPONSE',

  RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM = 'RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM',
  INVALID_PROCESS_DEFINITION_DEPLOYED_START_FORM_RESPONSE = 'INVALID_PROCESS_DEFINITION_DEPLOYED_START_FORM_RESPONSE',

  RETRIEVE_PROCESS_DEFINITION = 'RETRIEVE_PROCESS_DEFINITION',
  INVALID_PROCESS_DEFINITION_RESPONSE = 'INVALID_PROCESS_DEFINITION_RESPONSE',
  RETRIEVE_PROCESS_DEFINITION_START_FORM = 'RETRIEVE_PROCESS_DEFINITION_START_FORM',
  INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE = 'INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE',
  UNSUPPORTED_ACTION = 'UNSUPPORTED_ACTION',
  SUBMIT_FORM = 'SUBMIT_FORM',
}

const codeErrors: Record<
  AppErrorCode,
  (params?: Record<string, string | undefined>) => string
> = {
  INVALID_CAMUNDA_FORM_CONFIG: (_params) => `Invalid configuration`,
  INVALID_CAMUNDA_FORM_KEY: (params) =>
    `Unsupported form key ${params?.formKey}. Missing path or deployment query parameters.`,
  INVALID_CAMUNDA_FORM_KEY_PATH_PARAM: (params) =>
    `Unsupported form key ${params?.formKey}. Path parameter ${params?.path} is invalid`,
  INVALID_CAMUNDA_FORM_KEY_DEPLOYMENT_PARAM: (params) =>
    `Unsupported form key ${params?.formKey}. Deployment parameter ${params?.deployment} is invalid`,
  MISSING_JSONFORMS_SCHEMA: (_params) => `Unable to find form schema`,
  MISSING_JSONFORMS_UISCHEMA: (_params) => `Unable to find form UI`,

  RETRIEVE_JSONFORMS_SCHEMA: (_params) =>
    'Unable to retrieve form schema resource',
  INVALID_JSONFORMS_SCHEMA: (_params) => 'Invalid form schema resource',
  RETRIEVE_JSONFORMS_UISCHEMA: (_params) =>
    'Unable to retrieve form UI resource',
  INVALID_JSONFORMS_UISCHEMA: (_params) => 'Invalid form UI resource',
  RETRIEVE_JSONFORMS_I18N: (_params) =>
    'Unable to retrieve form internationalization resource',
  INVALID_JSONFORMS_I18N: (_params) =>
    'Invalid form internationalization resource',

  RETRIEVE_TASK_FORM_VARIABLES: (_params) =>
    'Unable to retrieve form variables',
  INVALID_TASK_FORM_VARIABLES_RESPONSE: (_params) =>
    'Invalid form variables response',

  RETRIEVE_PROCESS_DEFINITION_FORM_VARIABLES: (_params) =>
    'Unable to retrieve form variables',
  INVALID_PROCESS_DEFINITION_FORM_VARIABLES_RESPONSE: (_params) =>
    'Invalid form variables response',

  RETRIEVE_DEPLOYMENT_RESOURCES: (_params) =>
    'Unable to retrieve deployment resources',
  INVALID_DEPLOYMENT_RESOURCES_RESPONSE: (_params) =>
    'Invalid deployment resources response',

  RETRIEVE_TASK: (_params) => 'Unable to retrieve task',
  INVALID_TASK_RESPONSE: (_params) => 'Invalid task response',

  RETRIEVE_TASK_FORM: (_params) => 'Unable to retrieve task form',
  INVALID_TASK_FORM_RESPONSE: (_params) => 'Invalid task form response',

  RETRIEVE_TASK_DEPLOYED_FORM: (_params) =>
    'Unable to retrieve task deployed form',
  INVALID_TASK_DEPLOYED_FORM_RESPONSE: (_params) =>
    'Invalid task deployed form response',

  RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM: (_params) =>
    'Unable to retrieve process definition deployed start form',
  INVALID_PROCESS_DEFINITION_DEPLOYED_START_FORM_RESPONSE: (_params) =>
    'Invalid process definition deployed start form response',

  RETRIEVE_PROCESS_DEFINITION: (_params) =>
    'Unable to retrieve process definition',
  INVALID_PROCESS_DEFINITION_RESPONSE: (_params) =>
    'Invalid process definition response',
  RETRIEVE_PROCESS_DEFINITION_START_FORM: (_params) =>
    'Unable to retrieve process definition start form',
  INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE: (_params) =>
    'Invalid process definition start form response',

  UNSUPPORTED_ACTION: (_params) => `Unsupported action ${_params?.action}`,
  SUBMIT_FORM: (_params) => `Submit Error ${_params?.message}`,
};
export class AppException extends Error {
  code: string;
  cause?: Error;

  constructor(
    code: AppErrorCode,
    cause?: Error,
    errorParams?: Record<string, string | undefined>
  ) {
    super(`${codeErrors[code](errorParams)}`);

    this.name = 'AppException';
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
      name: this.name,
      code: this.code,
      cause: cause,
    };
  }
}
