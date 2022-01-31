<template>
  <v-label
    v-if="layout.visible"
    :class="styles.label.root"
    v-bind="vuetifyProps('v-label')"
  >
    <div v-if="templateError !== null" class="error">
      Template Error: {{ templateError }}
    </div>

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
import Vue from 'vue';
import { ErrorObject } from 'ajv';
import { CamundaFormConfig, CamundaFormContext } from '@/core/types';

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

    const camundaFormConfig = inject<CamundaFormConfig>('camundaFormConfig');

    if (!camundaFormConfig) {
      throw new Error(
        "'camundaFormConfig' couldn't be injected. Are you within CamundaForm?"
      );
    }

    const camundaFormContext = inject<CamundaFormContext>('camundaFormContext');

    if (!camundaFormContext) {
      throw new Error(
        "'camundaFormContext' couldn't be injected. Are you within CamundaForm?"
      );
    }

    let templateError: string | null = null;

    let camundaForm = {
      config: camundaFormConfig,
      context: camundaFormContext,
    };

    return {
      ...layout,
      t,
      jsonforms,
      parentComponent: this,
      templateError,
      camundaForm,
    };
  },
  errorCaptured: function (err: Error, vm: Vue, info: string) {
    if (info == 'render') {
      this.templateError = err.message;
    }
  },
  computed: {
    data(): any {
      // this refers to the created element by RuntimeTemplateCompiler ( the span )
      // this.$parent.$parent refers to RuntimeTemplateCompiler
      // this.$parent.$parent refers to templateLabelRenderer
      const jsonforms: JsonFormsSubStates = this.$parent.$parent.jsonforms;

      return jsonforms.core?.data;
    },
    config(): CamundaFormConfig {
      let form: {
        config: CamundaFormConfig;
        context: CamundaFormContext;
      } = this.$parent.$parent.camundaForm;

      return form?.config;
    },
    context(): CamundaFormContext {
      let form: {
        config: CamundaFormConfig;
        context: CamundaFormContext;
      } = this.$parent.$parent.camundaForm;

      return form?.context;
    },
    errors(): ErrorObject[] | undefined {
      // this refers to the created element by RuntimeTemplateCompiler ( the span )
      // this.$parent.$parent refers to RuntimeTemplateCompiler
      // this.$parent.$parent refers to templateLabelRenderer
      const jsonforms: JsonFormsSubStates = this.$parent.$parent.jsonforms;

      return jsonforms.core?.errors;
    },
    template(): string {
      if (this.layout.uischema.options?.dynamic) {
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

      // do not try to translate the template text
      return (this.layout.uischema as LabelElement).text;
    },
  },
  methods: {
    translate(
      key: string,
      defaultMessage: string | undefined
    ): string | undefined {
      return this.$parent.$parent.t(key, defaultMessage);
    },
  },
});

export default templateLabelRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: templateLabelRenderer,
  tester: rankWith(2, and(uiTypeIs('Label'), optionIs('template', true))),
};
</script>
