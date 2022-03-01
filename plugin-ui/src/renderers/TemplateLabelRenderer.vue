<template>
  <v-label
    v-if="layout.visible"
    :class="styles.label.root"
    :v-once="once"
    v-bind="vuetifyProps('v-label')"
  >
    {{ translatedLabel }}
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
  rendererProps,
  useJsonFormsLayout,
  RendererProps,
} from '@jsonforms/vue2';
import { useVuetifyLayout, useTranslator } from '@jsonforms/vue2-vuetify';
import { VLabel } from 'vuetify/lib';
import { ErrorObject } from 'ajv';
import { CamundaFormConfig, CamundaFormContext } from '../core/types';
import { template as templateFn } from '../core/template';

const templateLabelRenderer = defineComponent({
  name: 'template-label-renderer',
  components: {
    VLabel,
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

    let camundaForm = {
      config: camundaFormConfig,
      context: camundaFormContext,
    };

    return {
      ...layout,
      t,
      jsonforms,
      parentComponent: this,
      camundaForm,
    };
  },
  computed: {
    once(): boolean {
      return (
        this.layout.uischema?.options?.['v-once'] === true ||
        this.layout.uischema?.options?.['v-once'] === 'true'
      );
    },
    data(): any {
      return this.jsonforms.core?.data;
    },
    config(): CamundaFormConfig {
      let form: {
        config: CamundaFormConfig;
        context: CamundaFormContext;
      } = this.camundaForm;

      return form?.config;
    },
    context(): CamundaFormContext {
      let form: {
        config: CamundaFormConfig;
        context: CamundaFormContext;
      } = this.camundaForm;

      return form?.context;
    },
    errors(): ErrorObject[] | undefined {
      return this.jsonforms.core?.errors;
    },
    template(): string | undefined {
      if (this.layout.uischema.options?.i18n) {
        // try to use i18n template if the template changes based on the language
        return this.t(
          this.layout.uischema.options.i18n,
          (this.layout.uischema as LabelElement).text
        );
      }
      return (this.layout.uischema as LabelElement).text;
    },
    translatedLabel(): string | undefined {
      const compile = templateFn(this.template, {
        imports: {
          data: this.data,
          context: this.context,
          config: this.config,
          errors: this.errors,
          translate: this.translate.bind(this),
        },
      });

      return compile();
    },
  },
  methods: {
    translate(
      key: string,
      defaultMessage: string | undefined
    ): string | undefined {
      return this.t(key, defaultMessage ?? '');
    },
  },
});

export default templateLabelRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: templateLabelRenderer,
  tester: rankWith(2, and(uiTypeIs('Label'), optionIs('template', true))),
};
</script>
