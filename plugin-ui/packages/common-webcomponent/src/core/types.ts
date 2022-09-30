import { ValidationMode } from '@jsonforms/core';
import { VuetifyPreset } from 'vuetify/types/services/presets';

export interface VuetifyFormConfig {
  schema: string | Record<string, any>;
  uischema?: string | Record<string, any>;
  uischemas?: string | Record<string, any>[];
  data?: string | Record<string, any>;
  config?: string | Record<string, any>;
  readonly?: string | boolean;
  validationMode?: ValidationMode;
  locale?: string;
  style?: string;
  translations?: string | Record<string, any>;
  defaultPreset?: string | Partial<VuetifyPreset>;
}
