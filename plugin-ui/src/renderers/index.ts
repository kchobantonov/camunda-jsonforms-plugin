export { default as ButtonRenderer } from './ButtonRenderer.vue';
export { default as TemplateLabelRenderer } from './TemplateLabelRenderer.vue';

import { entry as buttonRendererEntry } from './ButtonRenderer.vue';
import { entry as templateLabelRendererEntry } from './TemplateLabelRenderer.vue';

import { extendedVuetifyRenderers } from '@jsonforms/vue2-vuetify';

export const allRenderers = [buttonRendererEntry, templateLabelRendererEntry, ...extendedVuetifyRenderers];
