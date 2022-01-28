<template>
  <div>
    <div v-if="templateError !== null" class="error">
      Template Error: {{ templateError }}
    </div>

    <runtime-template-compiler :template="template" />
  </div>
</template>

<script lang="ts">
import {
  JsonFormsRendererRegistryEntry,
  Layout,
  rankWith,
  uiTypeIs,
  JsonFormsSubStates,
  UISchemaElement,
} from '@jsonforms/core';
import { defineComponent, inject } from '@vue/composition-api';
import {
  DispatchRenderer,
  rendererProps,
  useJsonFormsLayout,
  RendererProps,
} from '@jsonforms/vue2';
import { useVuetifyLayout, useTranslator } from '@jsonforms/vue2-vuetify';
import { VLabel, VBtn, VSpacer } from 'vuetify/lib';
import { RuntimeTemplateCompiler } from 'vue-runtime-template-compiler';
import Vue from 'vue';
import { ErrorObject } from 'ajv';
import { CamundaFormConfig, CamundaFormContext } from '@/core/types';

interface TemplateElement extends UISchemaElement {
  type: 'Template';
  /**
   * The template string.
   */
  template: string;
}

// Register any components that are not yet provided by other jsonforms renderers and we want to use them in the template like VOtpInput
const templateRenderer = defineComponent({
  name: 'template-renderer',
  components: {
    DispatchRenderer,
    VLabel,
    VBtn,
    VSpacer,
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

    return { ...layout, t, jsonforms, templateError, camundaForm };
  },
  errorCaptured: function (err: Error, vm: Vue, info: string) {
    if (info == 'render') {
      this.templateError = err.message;
    }
  },
  computed: {
    data(): any {
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
      const jsonforms: JsonFormsSubStates = this.$parent.$parent.jsonforms;
      return jsonforms.core?.errors;
    },
    template(): string | undefined {
      return (this.layout.uischema as TemplateElement).template;
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

export default templateRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: templateRenderer,
  tester: rankWith(1, uiTypeIs('Template')),
};
</script>
