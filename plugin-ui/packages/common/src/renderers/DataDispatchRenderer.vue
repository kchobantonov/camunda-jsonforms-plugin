<template>
  <component v-bind:is="determinedRenderer" v-bind="renderer"></component>
</template>

<script lang="ts">
import { defineComponent, toRefs, ref } from '@vue/composition-api';
import { UnknownRenderer, useJsonFormsRenderer } from '@jsonforms/vue2';
import maxBy from 'lodash/maxBy';
import {
  JsonFormsCellRendererRegistryEntry,
  JsonFormsRendererRegistryEntry,
  JsonSchema,
  RendererProps,
  UISchemaElement,
} from '@jsonforms/core';
import { CompType } from '@jsonforms/vue2-vuetify/lib/vue';

export const rendererDataProps = <U = UISchemaElement>() => ({
  schema: {
    required: true as true,
    type: [Object, Boolean] as CompType<
      JsonSchema,
      [ObjectConstructor, BooleanConstructor]
    >,
  },
  uischema: {
    required: true as true,
    type: [Object] as CompType<U, [ObjectConstructor]>,
  },
  path: {
    required: true as true,
    type: String,
  },
  enabled: {
    required: false as false,
    type: Boolean,
    default: undefined,
  },
  renderers: {
    required: false,
    type: [Array] as CompType<
      JsonFormsRendererRegistryEntry[],
      [ArrayConstructor]
    >,
    default: undefined,
  },
  cells: {
    required: false,
    type: [Array] as CompType<
      JsonFormsCellRendererRegistryEntry[],
      [ArrayConstructor]
    >,
    default: undefined,
  },
  data: {
    required: true as true,
    type: [Object] as CompType<any, [ObjectConstructor]>,
  },
});

export default defineComponent({
  name: 'data-dispatch-renderer',
  props: {
    ...rendererDataProps(),
  },
  setup(props: RendererProps) {
    return { ...useJsonFormsRenderer(props), scopeData: ref(props.data) };
  },
  provide() {
    const { scopeData } = toRefs(this);

    return {
      scopeData: scopeData,
    };
  },
  watch: {
    data: {
      deep: true,
      handler(newData: any, _oldInput: any): void {
        this.scopeData = newData;
      },
    },
  },
  computed: {
    determinedRenderer(): any {
      const renderer = maxBy<JsonFormsRendererRegistryEntry>(
        this.renderer.renderers,
        (r) => r.tester(this.renderer.uischema, this.renderer.schema)
      );
      if (
        renderer === undefined ||
        renderer.tester(this.renderer.uischema, this.renderer.schema) === -1
      ) {
        return UnknownRenderer;
      } else {
        return renderer.renderer;
      }
    },
  },
});
</script>
