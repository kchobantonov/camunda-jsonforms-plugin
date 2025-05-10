import type { FormContext } from '@chobantonov/jsonforms-vuetify-renderers';
import type { JsonFormsUISchemaRegistryEntry } from '@jsonforms/core';
import type { JsonFormsChangeEvent } from '@jsonforms/vue';
import type { ErrorObject } from 'ajv';
import type { InjectionKey, MaybeRefOrGetter, SetupContext } from 'vue';

export interface BaseCamundaFormConfig {
  url: string;
}
export interface CamundaFormTaskIdConfig extends BaseCamundaFormConfig {
  taskId: string;
}

export interface CamundaFormProcessDefinitionIdConfig
  extends BaseCamundaFormConfig {
  processDefinitionId: string;
}

export interface CamundaFormProcessDefinitionKeyConfig
  extends BaseCamundaFormConfig {
  processDefinitionKey: string;
  tenantId?: string;
}

export type CamundaFormConfig =
  | CamundaFormTaskIdConfig
  | CamundaFormProcessDefinitionIdConfig
  | CamundaFormProcessDefinitionKeyConfig;

export interface TaskForm {
  key?: string;
  contextPath?: string;
}
export interface Task {
  id: string;
  formKey?: string;
  processDefinitionId?: string;
  tenantId?: string;
}

export interface ProcessDefinition {
  id: string;
  deploymentId?: string;
}

export interface CamundaFormContext extends FormContext {
  camundaFormConfig: CamundaFormConfig;
  taskForm?: MaybeRefOrGetter<TaskForm>;
  variables: MaybeRefOrGetter<Record<string, VariableValue>>;
  translations: MaybeRefOrGetter<Record<string, any>>;
  uischemas?: MaybeRefOrGetter<JsonFormsUISchemaRegistryEntry[]>;
}

export interface Resource {
  /**
   * The id of the deployment.
   */
  id: string;
  /**
   * The name of the deployment.

   */
  name: string;
  /**
   * The source of the deployment.

   */
  source: string;

  /**
   * The tenant id of the deployment.

   */
  tenantId: string;

  /**
   * The date and time of the deployment.
   */
  deploymentTime: string;
}

export interface VariableValue {
  type: string;
  value: any;
  valueInfo: Record<string, any>;
}

export const isTaskIdConfig = (
  object: CamundaFormConfig,
): object is CamundaFormTaskIdConfig => {
  return (
    Object.prototype.hasOwnProperty.call(object, 'taskId') &&
    (object as any).taskId
  );
};

export const isProcessDefinitionIdConfig = (
  object: CamundaFormConfig,
): object is CamundaFormProcessDefinitionIdConfig => {
  return (
    Object.prototype.hasOwnProperty.call(object, 'processDefinitionId') &&
    (object as any).processDefinitionId
  );
};

export const isProcessDefinitionKeyConfig = (
  object: CamundaFormConfig,
): object is CamundaFormProcessDefinitionKeyConfig => {
  return (
    Object.prototype.hasOwnProperty.call(object, 'processDefinitionKey') &&
    (object as any).processDefinitionKey
  );
};

export interface ValueInfo {
  /**
   * A string representation of the object's type name.
   */
  objectTypeName?: string;
  /**
   * The serialization format used to store the variable.
   */
  serializationDataFormat?: string;

  /**
   * Mark the variables as transient
   */
  transient?: boolean;
}

export interface FileValueInfo extends ValueInfo {
  /**
   * The name of the file. This is not the variable name but the name that will be used when downloading the file again.
   */
  filename: string;

  /**
   * The mime type of the file that is being uploaded.
   */
  mimeType?: string;

  /**
   *  Identifies the file's encoding as specified on value creation.
   */
  encoding?: string;
}

export const CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT = 'deployment';
export const CAMUNDA_FORM_KEY_QUERY_PARAM_PATH = 'path';

const actions = [
  'camunda:submit',
  'camunda:submit-without-data',
  'camunda:complete',
  'camunda:complete-without-data',
  'camunda:resolve',
  'camunda:resolve-without-data',
  'camunda:error',
  'camunda:escalation',
] as const;

export type Action = (typeof actions)[number];

export const isAction = (value: string): value is Action => {
  return actions.includes(value as any);
};

export interface FormCallback {
  onChange: (event: JsonFormsChangeEvent) => void;

  onLoadRequest: (input: RequestInfo, init?: RequestInit) => void;
  onLoadResponse: (response: Response) => void;
  onLoadError: (error: any) => void;

  onSubmitRequest: (input: RequestInfo, init?: RequestInit) => void;
  onSubmitResponse: (response: Response) => void;
  onSubmitError: (error: any) => void;
}

export const RESOURCE_SCHEMA_SUFFIX = '.schema.json';
export const RESOURCE_UISCHEMA_SUFFIX = '.uischema.json';
export const RESOURCE_UISCHEMAS_SUFFIX = '.uischemas.json';
export const RESOURCE_I18N_SUFFIX = '.i18n.json';
export const RESOURCE_UIDATA_SUFFIX = '.uidata.json';

export type Emitter = (event: string, ...args: any[]) => void;

export class ResponseException extends Error {
  response: Response;
  code: number;

  constructor(response: Response) {
    super(response.statusText);
    this.name = 'ResponseException';
    this.code = response.status;
    this.response = response;
  }

  toString() {
    return this.message;
  }

  toJSON() {
    return {
      message: this.message,
      name: this.name,
      code: this.code,
    };
  }
}

export const CamundaAdditionalErrorsKey: InjectionKey<ErrorObject[]> =
  Symbol.for('camunda-jsonforms:camundaAdditionalErrors');

export const CamundaFormApiKey: InjectionKey<ErrorObject[]> = Symbol.for(
  'camunda-jsonforms:camundaFormApi',
);

export const CamundaFormEmitterKey: InjectionKey<SetupContext['emit']> =
  Symbol.for('camunda-jsonforms:camundaFormEmitter');
