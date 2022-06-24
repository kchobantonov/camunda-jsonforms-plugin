import {
  UISchemaElement,
  JsonSchema,
  JsonFormsRendererRegistryEntry,
  JsonFormsCellRendererRegistryEntry,
  JsonFormsUISchemaRegistryEntry,
  ValidationMode,
} from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import Ajv from 'ajv';
import { VuetifyPreset } from 'vuetify/types/services/presets';

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
  input: JsonFormInput;
  renderers: JsonFormsRendererRegistryEntry[];
  cells: JsonFormsCellRendererRegistryEntry[];
  config?: Record<string, any>;
  readonly?: boolean;
  uischemas?: JsonFormsUISchemaRegistryEntry[];
  validationMode?: ValidationMode;
  ajv: Ajv;
  translations?: Record<string, any>;
  locale: string;
  style?: string;
  defaultPreset: Partial<VuetifyPreset>;
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
