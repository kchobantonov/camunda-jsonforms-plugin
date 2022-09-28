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
      :additionalErrors="additionalErrors"
      :ajv="ajv"
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
  createAjv,
} from '@kchobantonov/common-jsonforms';
import { defineComponent, PropType, ref, toRefs } from 'vue';
import _get from 'lodash/get';
import _remove from 'lodash/remove';
import isPlainObject from 'lodash/isPlainObject';
import merge from 'lodash/merge';
import { VCol, VContainer, VProgressLinear, VRow } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import { CamundaFormApi } from '../core/api';
import { CamundaFormContext } from '../core/types';
import { validateCamundaFormConfig } from '../core/validate';
import { camundaRenderers } from '../renderers';
import { ValidationMode } from '@jsonforms/core';
import { ErrorObject } from 'ajv';

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
    const additionalErrors = ref<ErrorObject[]>([]);
    const previousData = ref({});

    const ajv = createAjv();

    return {
      loading,
      context,
      api,
      props,
      renderers: camundaRenderers,
      cells: camundaRenderers,
      additionalErrors,
      previousData,
      ajv,
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
    additionalErrors: {
      handler(_value?: ErrorObject[], _oldValue?: ErrorObject[]) {
        // reset ajv - the schema will be registered again - otherwise an ajv error for the same schema id will be generated
        this.ajv.removeSchema('/');
        // set the last data
        this.context!.input.data = this.previousData;
      },
    },
  },
  async mounted() {
    await this.reload();
  },
  provide() {
    // provide as a reactive property
    const { context, api, additionalErrors } = toRefs(this);

    return {
      additionalErrors: additionalErrors,
      formContext: context,
      camundaFormApi: api,
      camundaFormEmitter: this.$emit.bind(this),
    };
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      if (this.additionalErrors.length > 0) {
        // remove the error when we detect a change on the data
        _remove(this.additionalErrors, (error: ErrorObject) => {
          if (error.instancePath) {
            let controlPath: string = error.instancePath;

            // change '/' chars to '.'
            controlPath = controlPath.replace(/\//g, '.');

            // remove '.' chars at the beginning of paths
            controlPath = controlPath.replace(/^./, '');

            if (
              _get(event.data, controlPath) !==
              _get(this.previousData, controlPath)
            ) {
              return true;
            }

            return false;
          }
          return true;
        });
      }
      this.previousData = event.data;
      // for now JsonFormsChangeEvent does not include the additionalErrors
      this.$emit('change', {
        ...event,
        additionalErrors: this.additionalErrors,
      });
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
        context.config = config;

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
      const props = _get(options?.vuetify, path);

      return props && isPlainObject(props) ? props : {};
    },
  },
});

// cast to 'any' because of Typescript problems (ts(7056))
export default camundaJsonForms as any;
</script>

<style></style>
