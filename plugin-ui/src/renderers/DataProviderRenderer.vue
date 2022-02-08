<template>
  <data-provider :url="url" v-if="url">
    <div slot-scope="{ fetch }">
      <template v-for="(element, index) in elements">
        <data-dispatch-renderer
          :key="`${layout.path}-${index}`"
          :schema="layout.schema"
          :uischema="element"
          :path="layout.path"
          :enabled="layout.enabled"
          :renderers="layout.renderers"
          :cells="layout.cells"
          :data="fetch"
        />
      </template>
    </div>
  </data-provider>
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
  rendererProps,
  useJsonFormsLayout,
  RendererProps,
} from '@jsonforms/vue2';
import { useVuetifyLayout, useTranslator } from '@jsonforms/vue2-vuetify';
import { CamundaFormConfig, CamundaFormContext } from '@/core/types';
import DataProvider from '../components/DataProvider.vue';
import DataDispatchRenderer from './DataDispatchRenderer.vue';
import { template as templateFn } from '../core/template';

interface DataProviderElement extends Layout {
  type: 'DataProvider';
  /**
   * The remote url to fetch data.
   */
  url: string;
}

const dataProviderRenderer = defineComponent({
  name: 'data-provider-renderer',
  components: {
    DataDispatchRenderer,
    DataProvider,
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
  computed: {
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
    url(): string | undefined {
      return templateFn((this.layout.uischema as DataProviderElement).url, {
        imports: {
          data: this.data,
          context: this.context,
          config: this.config,
        },
      })();
    },
    elements(): UISchemaElement[] {
      return (this.layout.uischema as DataProviderElement).elements;
    },
  },
});

export default dataProviderRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: dataProviderRenderer,
  tester: rankWith(1, uiTypeIs('DataProvider')),
};
</script>
