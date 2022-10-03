import {
  JsonFormsSubStates,
  JsonSchema,
  UISchemaElement,
  ValidationMode,
} from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { VuetifyPreset } from 'vuetify/types/services/presets';

export const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;

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
  config?: Record<string, any>;
  readonly?: boolean;
  validationMode?: ValidationMode;
  locale: string;
  style?: string;
  defaultPreset?: Partial<VuetifyPreset>;
}

export interface FormContext {
  config?: FormConfig;
  translations?: Record<string, any>;
  input: JsonFormInput;
  actions?: Record<string, Function>;
  uidata: Record<string, any>;
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

export interface Actions {
  [id: string]: Function;
}

export type ActionEvent = {
  jsonforms: JsonFormsSubStates;
  context: FormContext;
  $el: Element;
};
