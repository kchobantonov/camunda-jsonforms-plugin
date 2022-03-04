import { UISchemaElement, JsonSchema } from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';

export type JsonFormInput = {
  schema: JsonSchema;
  schemaUrl?: string;
  uischema?: UISchemaElement;
  data: Record<string, any>;
};

export type ResolvedSchema = {
  schema?: JsonSchema;
  resolved: boolean;
  error?: string;
};

export interface FormConfig {
  url: string;
  locale?: string;
  style?: string;
}

export interface FormContext {
  translations?: Record<string, any>;
  input: JsonFormInput;
}

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
export const RESOURCE_I18N_SUFFIX = '.i18n.json';

export type Emitter = (event: string, ...args: any[]) => void;

