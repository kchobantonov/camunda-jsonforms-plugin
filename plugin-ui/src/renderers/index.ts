export { default as ButtonRenderer } from './ButtonRenderer.vue';

import { entry as buttonRendererEntry } from './ButtonRenderer.vue';
import { extendedVuetifyRenderers } from '@jsonforms/vue2-vuetify';

export const allRenderers = [buttonRendererEntry, ...extendedVuetifyRenderers];
