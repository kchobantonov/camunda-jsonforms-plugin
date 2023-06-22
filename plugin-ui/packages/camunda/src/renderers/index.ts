export { default as CamundaButtonRenderer } from './CamundaButtonRenderer.vue';

import { entry as camundaButtonRendererEntry } from './CamundaButtonRenderer.vue';

import { vuetifyRenderers } from '@chobantonov/jsonforms-vuetify-renderers';

export const camundaRenderers = [camundaButtonRendererEntry, ...vuetifyRenderers];
