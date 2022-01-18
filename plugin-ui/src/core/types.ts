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
  submitHeaders?: Headers;
  locale?: string;
  onSubmitSuccessResponse: (response: Response) => void;
  onSubmitErrorResponse: (response: Response) => void;
  onSubmitError: (error: any) => void;
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
  debug?: boolean;
};