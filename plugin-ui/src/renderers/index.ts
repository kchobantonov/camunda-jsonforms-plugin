export { default as ButtonRenderer } from './ButtonRenderer.vue';
export { default as TemplateLabelRenderer } from './TemplateLabelRenderer.vue';
export { default as TemplateLayoutRenderer } from './TemplateLayoutRenderer.vue';
export { default as FileRenderer } from './FileRenderer.vue';

import { entry as buttonRendererEntry } from './ButtonRenderer.vue';
import { entry as templateLabelRendererEntry } from './TemplateLabelRenderer.vue';
import { entry as templateLayoutRendererEntry } from './TemplateLayoutRenderer.vue';
import { entry as fileRendererEntry } from './FileRenderer.vue';

import { extendedVuetifyRenderers } from '@jsonforms/vue2-vuetify';

export const allRenderers = [
  buttonRendererEntry,
  templateLabelRendererEntry,
  templateLayoutRendererEntry,
  fileRendererEntry,
  ...extendedVuetifyRenderers,
];
