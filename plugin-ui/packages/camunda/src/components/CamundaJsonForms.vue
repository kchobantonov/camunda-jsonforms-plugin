<template>
  <div>
    <div v-if="loading">
      <v-container style="height: 400px">
        <v-row class="fill-height" align-content="center" justify="center">
          <v-col class="text-subtitle-1 text-center" cols="12">
            Loading...
          </v-col>
          <v-col cols="6">
            <v-progress-linear
              indeterminate
              rounded
              height="6"
            ></v-progress-linear>
          </v-col>
        </v-row>
      </v-container>
    </div>

    <resolved-json-forms
      v-else-if="context != null"
      :input="context.input"
      :renderers="renderers"
      :cells="cells"
      :config="config"
      :validationMode="validationMode"
      :locale="locale"
      :translations="context.translations"
      :readonly="readonly"
      @change="onChange"
    />
  </div>
</template>

<script lang="ts">
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import {
  Emitter,
  LoadEmitter,
  ResolvedJsonForms,
  RestClient,
} from '@kchobantonov/common-jsonforms';
import { defineComponent, PropType, ref, toRefs } from 'vue';
import get from 'lodash/get';
import isPlainObject from 'lodash/isPlainObject';
import merge from 'lodash/merge';
import { VCol, VContainer, VProgressLinear, VRow } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import { CamundaFormApi } from '../core/api';
import { CamundaFormContext } from '../core/types';
import { validateCamundaFormConfig } from '../core/validate';
import { camundaRenderers } from '../renderers';
import { ValidationMode } from '@jsonforms/core';

export const camundaJsonFormsProps = () => ({
  url: {
    required: true,
    type: String,
  },
  processDefinitionId: {
    required: false,
    type: String,
  },
  processDefinitionKey: {
    required: false,
    type: String,
  },
  taskId: {
    required: false,
    type: String,
  },
  config: {
    required: false,
    type: [Object] as PropType<Record<string, any>>,
  },
  readonly: {
    required: false,
    type: Boolean,
    default: false,
  },
  validationMode: {
    required: false,
    type: [String] as PropType<ValidationMode>,
    default: 'ValidateAndShow',
  },
  locale: {
    required: false,
    type: String,
    default: 'en',
  },
  defaultPreset: {
    required: false,
    type: [Object] as PropType<Partial<VuetifyPreset>>,
    default: () => {
      return {
        icons: {
          iconfont: 'mdi',
          values: {},
        },
        theme: {
          dark: false,
          default: 'light',
          disable: false,
          options: {
            cspNonce: undefined,
            customProperties: undefined,
            minifyTheme: undefined,
            themeCache: undefined,
          },
          themes: {
            light: {
              primary: '#1976D2',
              secondary: '#424242',
              accent: '#82B1FF',
              error: '#FF5252',
              info: '#2196F3',
              success: '#4CAF50',
              warning: '#FB8C00',
            },
            dark: {
              primary: '#2196F3',
              secondary: '#424242',
              accent: '#FF4081',
              error: '#FF5252',
              info: '#2196F3',
              success: '#4CAF50',
              warning: '#FB8C00',
            },
          },
        },
      };
    },
  },
});

const camundaJsonForms = defineComponent({
  name: 'camunda-json-forms',
  components: {
    ResolvedJsonForms,
    VContainer,
    VRow,
    VCol,
    VProgressLinear,
  },
  emits: [
    'change',
    'load-request',
    'load-response',
    'load-error',
    'submit-request',
    'submit-response',
    'submit-error',
  ],
  props: {
    ...camundaJsonFormsProps(),
  },
  setup(props) {
    const loading = ref(false);
    const context = ref<CamundaFormContext | null>(null);
    const api = ref<CamundaFormApi | null>(null);
    return {
      loading,
      context,
      api,
      props,
      renderers: camundaRenderers,
      cells: camundaRenderers,
    };
  },
  watch: {
    url: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.reload();
        }
      },
      deep: true,
    },
    processDefinitionId: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.reload();
        }
      },
      deep: true,
    },
    processDefinitionKey: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.reload();
        }
      },
      deep: true,
    },
    taskId: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.reload();
        }
      },
      deep: true,
    },
  },
  async mounted() {
    await this.reload();
  },
  provide() {
    // provide as a reactive property
    const { context, api } = toRefs(this);

    return {
      formContext: context,
      camundaFormApi: api,
      camundaFormEmitter: this.$emit.bind(this),
    };
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      this.$emit('change', event);
    },
    async reload() {
      await this.loadContext();

      // apply any themes
      if (this.context?.input?.uischema?.options) {
        const preset = this.vuetifyProps(
          this.context.input.uischema.options,
          'preset'
        ) as Partial<VuetifyPreset>;

        if (preset.theme) {
          this.$vuetify.theme = merge(this.$vuetify.theme, preset.theme);
        }
        if (preset.icons) {
          this.$vuetify.icons = merge(this.$vuetify.icons, preset.icons);
        }
      } else {
        // reset the theme if it was applied before in previous
        this.$vuetify.theme = merge(
          this.$vuetify.theme,
          this.props.defaultPreset.theme
        );
        this.$vuetify.icons = merge(
          this.$vuetify.icons,
          this.props.defaultPreset.icons
        );
      }
    },
    async loadContext() {
      this.loading = true;
      this.context = null;

      try {
        this.api = new CamundaFormApi();

        const restClient = new RestClient([
          new LoadEmitter(this.$emit.bind(this) as Emitter),
        ]);
        const config = validateCamundaFormConfig(this.props);
        const context = await this.api.loadForm(restClient, config);
        context.config  = config;
        
        this.context = context;
      } catch (e) {
        this.$emit('load-error', e);
      } finally {
        this.loading = false;
      }
    },
    vuetifyProps(
      options: Record<string, any>,
      path: string
    ): Record<string, any> {
      const props = get(options?.vuetify, path);

      return props && isPlainObject(props) ? props : {};
    },
  },
});

// cast to 'any' because of Typescript problems (ts(7056))
export default camundaJsonForms as any;
</script>

<style></style>
