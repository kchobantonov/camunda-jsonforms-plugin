import { UISchemaElement, JsonSchema } from '@jsonforms/core';

export type CamundaJsonFormInput = {
  schema?: JsonSchema;
  uischema?: UISchemaElement;
  data: Record<string, any>;
};

export type ResolvedSchema = {
  schema?: JsonSchema;
  resolved: boolean;
  error?: string;
};

export interface CamundaFormConfig {
  camundaUrl: string;
  processDefinitionId: string;
  formUrl: string;
  taskId?: string;
  locale?: string;
  style?: string;
};

export interface Task {
  formKey?: string;
  processDefinitionId?: string;
};

export interface ProcessDefinition {
  deploymentId?: string;
};


export interface CamundaFormContext {
  task?: Task;
  processDefinition?: ProcessDefinition;
  translations?: Record<string, any>;
  variables: Record<string, VariableValue>;
  input: CamundaJsonFormInput,
};

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
  valueInfo: Record<string, any>
}