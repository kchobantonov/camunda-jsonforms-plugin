export { default as ButtonRenderer } from './ButtonRenderer.vue';

import { entry as buttonRendererEntry } from './ButtonRenderer.vue';

import { commonRenderers } from '@kchobantonov/common-jsonforms';

export const allRenderers = [
  buttonRendererEntry,
  ...commonRenderers,
];
