export { default as CamundaButtonRenderer } from './CamundaButtonRenderer.vue';

import { entry as camundaButtonRendererEntry } from './CamundaButtonRenderer.vue';

import { commonRenderers } from '@chobantonov/common-jsonforms';

export const camundaRenderers = [camundaButtonRendererEntry, ...commonRenderers];
