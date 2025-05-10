import {
  and,
  rankWith,
  uiTypeIs,
  type JsonFormsRendererRegistryEntry,
  type Tester,
  type UISchemaElement,
} from '@jsonforms/core';
import { isAction } from '../core/types';

import camundaButtonRenderer from './CamundaButtonRenderer.vue';

export const isCamundaAction: Tester = (uischema: UISchemaElement): boolean => {
  return isAction((uischema as any).action);
};

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: camundaButtonRenderer,
  tester: rankWith(2, and(uiTypeIs('Button'), isCamundaAction)),
};
