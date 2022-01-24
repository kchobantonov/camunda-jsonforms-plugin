export { default as ButtonRenderer } from './ButtonRenderer.vue';
export { default as TemplateLabelRenderer } from './TemplateLabelRenderer.vue';
export { default as TemplateRenderer } from './TemplateRenderer.vue';
export { default as FileRenderer } from './FileRenderer.vue';

import { entry as buttonRendererEntry } from './ButtonRenderer.vue';
import { entry as templateLabelRendererEntry } from './TemplateLabelRenderer.vue';
import { entry as templateRendererEntry } from './TemplateRenderer.vue';
import { entry as fileRendererEntry } from './FileRenderer.vue';

import { extendedVuetifyRenderers } from '@jsonforms/vue2-vuetify';

export const allRenderers = [buttonRendererEntry, templateLabelRendererEntry, templateRendererEntry, fileRendererEntry, ...extendedVuetifyRenderers];
