import { ValidationMode } from '@jsonforms/core';

export interface VuetifyFormConfig {
  schema: string;
  uischema?: string;
  uischemas?: string;
  data?: string;
  uidata?: string;
  actions?: string;
  config?: string;
  readonly?: string | boolean;
  validationMode?: ValidationMode;
  locale?: string;
  style?: string;
  translations?: string;
  defaultPreset?: string;
}