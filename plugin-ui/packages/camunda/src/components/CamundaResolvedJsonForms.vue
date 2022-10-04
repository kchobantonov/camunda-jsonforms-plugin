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
      v-else-if="context !== null"
      :data="context.data"
      :schema="context.schema"
      :uischema="context.uischema"
      :renderers="renderers"
      :cells="cells"
      :config="config"
      :readonly="readonly"
      :uischemas="uischemas"
      :validationMode="validationMode"
      :ajv="ajv"
      :i18n="i18n"
      :additionalErrors="additionalErrors"
      @change="onChange"
    />
  </div>
</template>

<script lang="ts">
import {
  JsonFormsCellRendererRegistryEntry,
  JsonFormsI18nState,
  JsonFormsRendererRegistryEntry,
  JsonFormsUISchemaRegistryEntry,
  ValidationMode
} from '@jsonforms/core';
import { JsonFormsChangeEvent, MaybeReadonly } from '@jsonforms/vue2';
import { createAjv, ResolvedJsonForms } from '@kchobantonov/common-jsonforms';
import { ErrorObject } from 'ajv';
import Ajv from 'ajv/dist/core';
import _get from 'lodash/get';
import isPlainObject from 'lodash/isPlainObject';
import merge from 'lodash/merge';
import _remove from 'lodash/remove';
import { defineComponent, PropType, ref, toRef } from 'vue';
import { VCol, VContainer, VProgressLinear, VRow } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import { LoadEmitter, RestClient } from '../core';
import { CamundaFormApi } from '../core/api';
import { CamundaFormContext, Emitter } from '../core/types';
import { validateCamundaFormConfig } from '../core/validate';
import { camundaRenderers } from '../renderers';

const camundaResolvedJsonForms = defineComponent({
  name: 'camunda-resolved-json-forms',
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
    renderers: {
      required: false,
      type: Array as PropType<MaybeReadonly<JsonFormsRendererRegistryEntry[]>>,
      default: () => camundaRenderers,
    },
    cells: {
      required: false,
      type: Array as PropType<
        MaybeReadonly<JsonFormsCellRendererRegistryEntry[]>
      >,
      default: () => camundaRenderers,
    },
    config: {
      required: false,
      type: Object as PropType<any>,
      default: undefined,
    },
    readonly: {
      required: false,
      type: Boolean,
      default: false,
    },
    uischemas: {
      required: false,
      type: Array as PropType<MaybeReadonly<JsonFormsUISchemaRegistryEntry[]>>,
      default: () => [],
    },
    validationMode: {
      required: false,
      type: String as PropType<ValidationMode>,
      default: 'ValidateAndShow',
    },
    ajv: {
      required: false,
      type: Object as PropType<Ajv>,
      default: () => createAjv(),
    },
    i18n: {
      required: false,
      type: Object as PropType<JsonFormsI18nState>,
      default: undefined,
    },
    additionalErrors: {
      required: false,
      type: Array as PropType<ErrorObject[]>,
      default: () => [],
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
    actions: {
      required: false,
      type: [Object] as PropType<Record<string, Function>>,
      default: () => {},
    },
  },
  setup(props) {
    const loading = ref(false);
    const context = ref<CamundaFormContext | null>(null);
    const api = new CamundaFormApi();
    const additionalErrors = ref<ErrorObject[]>([]);
    const previousData = ref({});

    return {
      props,
      loading,
      context,
      api,
      additionalErrors,
      previousData,
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
        // set the last data
        const context = this.context;
        if (context) {
          context.data = this.previousData;
        }
      },
      deep: true,
    },
  },
  async mounted() {
    await this.reload();
  },
  provide() {

    return {
      additionalErrors: toRef(this, 'additionalErrors'),
      formContext: toRef(this, 'context'),
      camundaFormApi: this.api,
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

      let preset: Partial<VuetifyPreset> | null = null;

      if (this.context?.uischema?.options) {
        preset = this.vuetifyProps(
          this.context.uischema.options,
          'preset'
        ) as Partial<VuetifyPreset>;
      }

      // apply any themes
      this.$vuetify.theme = merge(
        this.$vuetify.theme,
        preset && preset.theme ? preset.theme : this.props.defaultPreset.theme
      );
      this.$vuetify.icons = merge(
        this.$vuetify.icons,
        preset && preset.icons ? preset.icons : this.props.defaultPreset.icons
      );
    },
    async loadContext() {
      this.loading = true;
      this.context = null;

      try {
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

export default camundaResolvedJsonForms;
</script>

<style></style>
