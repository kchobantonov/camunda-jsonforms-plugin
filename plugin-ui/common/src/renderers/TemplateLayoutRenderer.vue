<template>
 <div v-if="layout.visible">
    <div v-if="templateError !== null" class="error">
      Template Error: {{ templateError }}
    </div>

    <template-compiler
      :template="template"
      :parent="parentComponent"
      :elements="elements"
      :componentComputed="componentComputed()"
      :componentMethods="componentMethods()"
      :componentFilters="componentFilters()"
      :componentComponents="componentComponents()"
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
      <template v-for="(element, index) in elements" v-slot:[`${index}`]>
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
  JsonFormsSubStates,
  UISchemaElement,
  uiTypeIs,
} from '@jsonforms/core';
import { defineComponent, inject, ref } from '@vue/composition-api';
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
import { FormConfig, FormContext } from '../core/types';

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
        "'jsonforms' couldn't be injected. Are you within JsonForms?"
      );
    }

    const formConfig = inject<FormConfig>('camundaFormConfig');

    if (!formConfig) {
      throw new Error(
        "'formConfig' couldn't be injected. Are you within JsonForms?"
      );
    }

    const formContext = inject<FormContext>('camundaFormContext');

    if (!formContext) {
      throw new Error(
        "'formContext' couldn't be injected. Are you within JsonForms?"
      );
    }

    let templateError = ref<string | null>(null);

    const scopeData = inject<any>('scopeData');

    return {
      ...layout,
      t,
      jsonforms,
      parentComponent: this,
      templateError,
      formConfig,
      formContext,
      scopeData,
    };
  },
  errorCaptured: function (err: Error, _vm: Vue, info: string) {
    if (info == 'render') {
      this.templateError = err.message;
    }
  },
  computed: {
    dataProvider(): any {
      const scopeData: any = this.scopeData;
      return scopeData;
    },
    data(): any {
      const jsonforms: JsonFormsSubStates = this.jsonforms;
      return jsonforms.core?.data;
    },
    config(): FormConfig {
      return this.formConfig;
    },
    context(): FormContext {
      return this.formContext;
    },
    errors(): ErrorObject[] | undefined {
      const jsonforms: JsonFormsSubStates = this.jsonforms;
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
    componentComputed() {
      const proxy = {} as any;
      const parentComponent = this as any;

      for (const key of [
        'dataProvider',
        'data',
        'config',
        'context',
        'errors',
      ]) {
        proxy[key] = function () {
          return parentComponent?.[key];
        };
      }

      return proxy;
    },
    componentMethods() {
      return {
        translate: this.translate.bind(this.parentComponent),
      };
    },
    translate(
      key: string,
      defaultMessage: string | undefined
    ): string | undefined {
      return this.t(key, defaultMessage ?? '');
    },
  },
});

export default templateLayoutRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: templateLayoutRenderer,
  tester: rankWith(1, uiTypeIs('TemplateLayout')),
};
</script>
