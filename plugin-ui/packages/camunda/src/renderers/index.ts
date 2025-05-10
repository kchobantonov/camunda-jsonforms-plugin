export { default as CamundaButtonRenderer } from './CamundaButtonRenderer.vue';

import { entry as camundaButtonRendererEntry } from './CamundaButtonRenderer.entry';

import { extraVuetifyRenderers } from '@chobantonov/jsonforms-vuetify-renderers';
import { extendedVuetifyRenderers } from '@jsonforms/vue-vuetify';

export const camundaRenderers = [...extendedVuetifyRenderers, ...extraVuetifyRenderers, camundaButtonRendererEntry];
