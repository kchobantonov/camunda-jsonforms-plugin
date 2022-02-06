<template>
  <div v-if="layout.visible">
    <div v-if="templateError !== null" class="error">
      Template Error: {{ templateError }}
    </div>

    <template-compiler
      :template="template"
      :parent="parentComponent"
      :elements="elements"
    >
      <template
        v-if="elements !== undefined && elements.length == 1"
        v-slot:default
      >
        <dispatch-renderer
          :key="`${layout.path}-${0}`"
          :schema="layout.schema"
          :uischema="elements[0]"
          :path="layout.path"
          :enabled="layout.enabled"
          :renderers="layout.renderers"
          :cells="layout.cells"
        />
      </template>
      <template
        v-for="(element, index) in elements"
        v-slot:[`${index}`]
      >
        <dispatch-renderer
          :key="`${layout.path}-${index}`"
          :schema="layout.schema"
          :uischema="element"
          :path="layout.path"
          :enabled="layout.enabled"
          :renderers="layout.renderers"
          :cells="layout.cells"
        />
      </template>
    </template-compiler>
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
import { defineComponent, inject, unref } from '@vue/composition-api';
import {
  DispatchRenderer,
  rendererProps,
  useJsonFormsLayout,
  RendererProps,
} from '@jsonforms/vue2';
import { useVuetifyLayout, useTranslator } from '@jsonforms/vue2-vuetify';
import { VLabel, VBtn, VSpacer } from 'vuetify/lib';
import TemplateCompiler from '../components/TemplateCompiler.vue';
import Vue from 'vue';
import { ErrorObject } from 'ajv';
import { CamundaFormConfig, CamundaFormContext } from '../core/types';

interface TemplateElement extends Layout {
  type: 'TemplateLayout';
  /**
   * The template string.
   */
  template: string;
}

// Register any components that are not yet provided by other jsonforms renderers and we want to use them in the template like VOtpInput
const templateLayoutRenderer = defineComponent({
  name: 'template-layout-renderer',
  components: {
    DispatchRenderer,
    VLabel,
    VBtn,
    VSpacer,
    TemplateCompiler,
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

      return unref(form?.context);
    },
    errors(): ErrorObject[] | undefined {
      const jsonforms: JsonFormsSubStates = this.$parent.$parent.jsonforms;
      return jsonforms.core?.errors;
    },
    template(): string | undefined {
      return (this.layout.uischema as TemplateElement).template;
    },
    elements(): UISchemaElement[] {
      return (this.layout.uischema as TemplateElement).elements;
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

export default templateLayoutRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: templateLayoutRenderer,
  tester: rankWith(1, uiTypeIs('TemplateLayout')),
};
</script>
