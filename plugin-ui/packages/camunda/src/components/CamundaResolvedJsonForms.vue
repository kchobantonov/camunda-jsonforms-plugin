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
      :state="state"
      :vuetify-config="vuetifyConfig"
      @change="onChange"
    ></resolved-json-forms>
  </div>
</template>

<script lang="ts">
import {
  createTranslator,
  FormContextKey,
  ResolvedJsonForms,
  TemplateComponentsKey,
  type JsonFormsProps,
  type VuetifyConfig,
} from '@chobantonov/jsonforms-vuetify-renderers';
import {
  defaultMiddleware,
  type JsonFormsCellRendererRegistryEntry,
  type JsonFormsI18nState,
  type JsonFormsRendererRegistryEntry,
  type ValidationMode,
} from '@jsonforms/core';
import type { JsonFormsChangeEvent } from '@jsonforms/vue';
import { ValidationIcon } from '@jsonforms/vue-vuetify';
import { type ErrorObject } from 'ajv';
import _get from 'lodash/get';
import isPlainObject from 'lodash/isPlainObject';
import _remove from 'lodash/remove';
import {
  defineComponent,
  getCurrentInstance,
  markRaw,
  reactive,
  ref,
  toRef,
  toValue,
  watch,
  type PropType,
  type Ref,
} from 'vue';
import { VCol, VContainer, VProgressLinear, VRow } from 'vuetify/components';
import {
  CamundaAdditionalErrorsKey,
  CamundaFormApiKey,
  CamundaFormEmitterKey,
  LoadEmitter,
  RestClient,
} from '../core';
import { CamundaFormApi } from '../core/api';
import type { CamundaFormContext, Emitter } from '../core/types';
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
      type: Array as PropType<JsonFormsRendererRegistryEntry[]>,
      default: () => camundaRenderers,
    },
    cells: {
      required: false,
      type: Array as PropType<JsonFormsCellRendererRegistryEntry[]>,
      default: () => camundaRenderers,
    },
    config: {
      required: false,
      type: Object as PropType<Record<string, any>>,
      default: undefined,
    },
    readonly: {
      required: false,
      type: Boolean,
      default: false,
    },
    validationMode: {
      required: false,
      type: String as PropType<ValidationMode>,
      default: 'ValidateAndShow',
    },
    locale: {
      required: false,
      type: String,
      default: 'en',
    },
    vuetifyConfig: {
      required: false,
      type: Object as PropType<VuetifyConfig>,
    },
  },
  setup(props, { emit }) {
    const loading = ref(false);
    const context: Ref<CamundaFormContext | null> =
      ref<CamundaFormContext | null>(null);
    const api = new CamundaFormApi();
    const additionalErrors = ref<ErrorObject[]>([]);
    const previousData = ref<any>(undefined);
    const i18n = ref<JsonFormsI18nState | undefined>(undefined);
    const localeToUse = ref(props.locale ?? 'en');
    const currentInstance = getCurrentInstance();

    const state = reactive<JsonFormsProps>({
      data: context.value?.data,
      schema: toValue(context.value?.schema),
      schemaUrl: context.value?.schemaUrl,
      uischema: toValue(context.value?.uischema),
      renderers: markRaw(props.renderers),
      cells: undefined, // not defined
      config: props.config,
      readonly: props.readonly,
      uischemas: toValue(context.value?.uischemas),
      validationMode: props.validationMode,
      i18n: i18n.value,
      additionalErrors: additionalErrors.value,
      middleware: defaultMiddleware,
    });

    const reload = async () => {
      loading.value = true;
      context.value = null;

      try {
        const restClient = new RestClient([
          new LoadEmitter(emit.bind(this) as Emitter),
        ]);
        const camundaFormConfig = validateCamundaFormConfig(props);
        const newContext = await api.loadForm(restClient, camundaFormConfig);
        newContext.camundaFormConfig = camundaFormConfig;
        newContext.config = props.config;

        context.value = newContext;
      } catch (e) {
        emit('load-error', e);
      } finally {
        loading.value = false;
      }
    };

    watch(
      () => props.url,
      (value, oldValue) => {
        if (value !== oldValue) {
          reload();
        }
      },
    );
    watch(
      () => props.processDefinitionId,
      (value, oldValue) => {
        if (value !== oldValue) {
          reload();
        }
      },
    );
    watch(
      () => props.processDefinitionKey,
      (value, oldValue) => {
        if (value !== oldValue) {
          reload();
        }
      },
    );
    watch(
      () => props.taskId,
      (value, oldValue) => {
        if (value !== oldValue) {
          reload();
        }
      },
    );
    watch(
      () => additionalErrors.value,
      (value, oldValue) => {
        // set the last data
        if (context.value) {
          context.value.data = previousData.value;
          state.data = context.value.data;
          state.additionalErrors = value;
          currentInstance?.update();
        }
      },
      { deep: true },
    );
    watch(
      () => props.locale,
      (value, oldValue) => {
        if (value !== oldValue) {
          localeToUse.value = value ?? 'en';
          i18n.value = {
            locale: localeToUse.value,
            translate: createTranslator(
              localeToUse.value,
              context.value?.translations,
            ),
          };

          state.i18n = i18n.value;
        }
      },
    );
    watch(
      () => context.value,
      (value, oldValue) => {
        // apply presets

        i18n.value = {
          locale: localeToUse.value,
          translate: createTranslator(localeToUse.value, value?.translations),
        };
        state.data = value?.data;
        state.schema = toValue(value?.schema);
        state.schemaUrl = value?.schemaUrl;
        state.uischema = toValue(value?.uischema);
        state.uischemas = toValue(value?.uischemas);
        state.i18n = i18n.value;
        // apply any themes

        currentInstance?.update();
      },
    );

    return {
      localeToUse,
      i18n,
      props,
      loading,
      context,
      api,
      additionalErrors,
      previousData,
      reload,
      state,
    };
  },
  async mounted() {
    await this.reload();
  },
  provide() {
    return {
      [TemplateComponentsKey]: {
        ValidationIcon,
      },
      [FormContextKey]: toRef(this, 'context'),
      [CamundaAdditionalErrorsKey]: toRef(this, 'additionalErrors'),
      [CamundaFormApiKey]: this.api,
      [CamundaFormEmitterKey]: this.$emit.bind(this),
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
    vuetifyProps(
      options: Record<string, any>,
      path: string,
    ): Record<string, any> {
      const props = _get(options?.vuetify, path);

      return props && isPlainObject(props) ? props : {};
    },
  },
});

export default camundaResolvedJsonForms;
</script>
