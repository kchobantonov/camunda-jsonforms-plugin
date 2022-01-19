<template>
  <v-label v-if="layout.visible" :class="styles.label.root">
    <runtime-template-compiler :template="template" :parent="parentComponent" />
  </v-label>
</template>

<script lang="ts">
import {
  JsonFormsRendererRegistryEntry,
  Layout,
  rankWith,
  uiTypeIs,
  LabelElement,
  JsonFormsSubStates,
  and,
  optionIs,
} from '@jsonforms/core';
import { defineComponent, inject } from '@vue/composition-api';
import {
  DispatchRenderer,
  rendererProps,
  useJsonFormsLayout,
  RendererProps,
} from '@jsonforms/vue2';
import { useVuetifyLayout, useTranslator } from '@jsonforms/vue2-vuetify';
import { VLabel } from 'vuetify/lib';
import { RuntimeTemplateCompiler } from 'vue-runtime-template-compiler';

const templateLabelRenderer = defineComponent({
  name: 'template-label-renderer',
  components: {
    DispatchRenderer,
    VLabel,
    RuntimeTemplateCompiler,
  },
  props: {
    ...rendererProps<Layout>(),
  },
  setup(props: RendererProps<Layout>) {
    const t = useTranslator();
    const layout = useVuetifyLayout(useJsonFormsLayout(props));

    const jsonforms = inject<JsonFormsSubStates>('jsonforms');
    if (!jsonforms) {
      throw new Error(
        "'jsonforms' couldn't be injected. Are you within JSON Forms?"
      );
    }

    return { ...layout, t, jsonforms, parentComponent: this };
  },
  computed: {
    data(): any {
      // this refers to the created element by RuntimeTemplateCompiler ( the span )
      // this.$parent.$parent refers to RuntimeTemplateCompiler
      // this.$parent.$parent refers to templateLabelRenderer
      return this.$parent.$parent.jsonforms.core?.data;
    },
    errors(): any {
      // this refers to the created element by RuntimeTemplateCompiler ( the span )
      // this.$parent.$parent refers to RuntimeTemplateCompiler
      // this.$parent.$parent refers to templateLabelRenderer
      return this.$parent.$parent.jsonforms.core?.errors;
    },
    template(): string {
      if (this.layout.uischema.options.dynamic) {
        return `<span>${this.translatedLabel}</span>`;
      }
      return `<span v-once>${this.translatedLabel}</span>`;
    },
    translatedLabel(): string | undefined {
      if (this.layout.uischema.options?.i18n) {
        return this.t(
          this.layout.uischema.options.i18n,
          (this.layout.uischema as LabelElement).text
        );
      }
      return this.t(
        (this.layout.uischema as LabelElement).text,
        (this.layout.uischema as LabelElement).text
      );
    },
  },
});

export default templateLabelRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: templateLabelRenderer,
  tester: rankWith(2, and(uiTypeIs('Label'), optionIs('template', true))),
};
</script>
