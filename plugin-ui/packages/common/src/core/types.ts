import {
  JsonFormsSubStates,
  JsonSchema,
  Translator,
  UISchemaElement,
} from '@jsonforms/core';
import { ErrorObject } from 'ajv';

export const AsyncFunction = Object.getPrototypeOf(
  async function () {}
).constructor;

export type ResolvedSchema = {
  schema?: JsonSchema;
  resolved: boolean;
  error?: string;
};

export interface FormContext {
  schemaUrl?: string;
  actions?: Record<string, Function>;
}

export interface TemplateFormContext extends FormContext {
  jsonforms: JsonFormsSubStates;
  scopeData: any;

  // below are just the shortcuts for acessing the jsonforms.core
  locale?: string;
  translate?: Translator;
  data?: any;
  schema?: JsonSchema;
  uischema?: UISchemaElement;
  errors?: ErrorObject[];
  additionalErrors?: ErrorObject[];
}
export interface Actions {
  [id: string]: Function;
}

export type ActionEvent = {
  jsonforms: JsonFormsSubStates;
  context: TemplateFormContext;
  // the action parameters passes from the UI schema
  params: Record<string, any>;
  $el: Element;
};
