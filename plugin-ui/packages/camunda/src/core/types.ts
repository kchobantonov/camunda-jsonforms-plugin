import { FormConfig, FormContext } from '@kchobantonov/common-jsonforms';

export interface CamundaFormTaskIdConfig extends FormConfig {
  taskId: string;
}

export interface CamundaFormProcessDefinitionIdConfig extends FormConfig {
  processDefinitionId: string;
}

export interface CamundaFormProcessDefinitionKeyConfig extends FormConfig {
  processDefinitionKey: string;
}

export type CamundaFormConfig =
  | CamundaFormTaskIdConfig
  | CamundaFormProcessDefinitionIdConfig
  | CamundaFormProcessDefinitionKeyConfig;

export interface Task {
  id: string;
  formKey?: string;
  processDefinitionId?: string;
}

export interface ProcessDefinition {
  id: string;
  deploymentId?: string;
}

export interface CamundaFormContext extends FormContext {
  task?: Task;
  processDefinition: ProcessDefinition;
  variables: Record<string, VariableValue>;
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
  object: CamundaFormConfig
): object is CamundaFormTaskIdConfig => {
  return (
    Object.prototype.hasOwnProperty.call(object, 'taskId') &&
    (object as any).taskId
  );
};

export const isProcessDefinitionIdConfig = (
  object: CamundaFormConfig
): object is CamundaFormProcessDefinitionIdConfig => {
  return (
    Object.prototype.hasOwnProperty.call(object, 'processDefinitionId') &&
    (object as any).processDefinitionId
  );
};

export const isProcessDefinitionKeyConfig = (
  object: CamundaFormConfig
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

export type Action =
  | 'submit'
  | 'submit-without-data'
  | 'complete'
  | 'complete-without-data'
  | 'resolve'
  | 'resolve-without-data'
  | 'error'
  | 'escalation';
