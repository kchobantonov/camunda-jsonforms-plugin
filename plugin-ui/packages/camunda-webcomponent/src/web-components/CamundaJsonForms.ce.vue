<template>
  <Suspense>
    <camunda-json-forms
      :url="url"
      :processDefinitionId="processDefinitionId"
      :processDefinitionKey="processDefinitionKey"
      :taskId="taskId"
      :config="config"
      :readonly="readonly"
      :validationMode="validationMode"
      :customStyle="customStyle"
      :locale="locale"
      @change="onChange"
      @load-request="onLoadRequest"
      @load-response="onLoadResponse"
      @load-error="onLoadError"
      @submit-request="onSubmitRequest"
      @submit-response="onSubmitResponse"
      @submit-error="onSubmitError"
    ></camunda-json-forms>
  </Suspense>
</template>

<script lang="ts">
import { useAppStore } from '@/store';
import { type ValidationMode } from '@jsonforms/core';
import { type JsonFormsChangeEvent } from '@jsonforms/vue';
import isPlainObject from 'lodash/isPlainObject';
import {
  defineAsyncComponent,
  defineComponent,
  watch,
  type PropType,
} from 'vue';

const CamundaJsonForms = defineAsyncComponent(
  () => import('../components/CamundaJsonForms.vue'),
);

const camundFormWc = defineComponent({
  components: {
    CamundaJsonForms,
  },
  emits: [
    'change',
    'load-request',
    'load-response',
    'load-error',
    'submit-request',
    'submit-response',
    'submit-error',
    'handle-action',
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
    config: {
      type: String,
      default: () => {
        return JSON.stringify({
          restrict: true,
          trim: false,
          showUnfocusedDescription: false,
          hideRequiredAsterisk: true,
        });
      },
      validator: function (value) {
        try {
          const config = typeof value == 'string' ? JSON.parse(value) : value;

          return config !== undefined && config !== null;
        } catch (e) {
          return false;
        }
      },
    },
    readonly: {
      required: false,
      type: Boolean,
      default: undefined,
    },
    validationMode: {
      type: [String] as PropType<ValidationMode>,
      default: 'ValidateAndShow',
      validator: function (value) {
        return (
          value === 'ValidateAndShow' ||
          value === 'ValidateAndHide' ||
          value === 'NoValidation'
        );
      },
    },
    locale: {
      required: false,
      type: String,
      default: 'en',
    },
    customStyle: {
      type: String,
      default: '.v-application__wrap { min-height: 0px; }',
    },
    dark: {
      required: false,
      type: Boolean,
      default: undefined,
    },
    rtl: {
      required: false,
      type: Boolean,
      default: undefined,
    },
    vuetifyOptions: {
      type: String,
      default: () => {
        return '{}';
      },
      validator: function (value) {
        try {
          const options = typeof value == 'string' ? JSON.parse(value) : value;

          return (
            options !== undefined && options !== null && isPlainObject(options)
          );
        } catch (e) {
          return false;
        }
      },
    },
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      this.$emit('change', event);
    },
    onLoadRequest(input: RequestInfo, init?: RequestInit): void {
      this.$emit('load-request', input, init);
    },
    onLoadResponse(response: Response): void {
      this.$emit('load-response', response);
    },
    onLoadError(error: any): void {
      this.$emit('load-error', error);
    },
    onSubmitRequest(input: RequestInfo, init?: RequestInit): void {
      this.$emit('submit-request', input, init);
    },
    onSubmitResponse(response: Response): void {
      this.$emit('submit-response', response);
    },
    onSubmitError(error: any): void {
      this.$emit('submit-error', error);
    },
  },
  setup(props) {
    const appStore = useAppStore();
    appStore.rtl = props.rtl !== undefined ? props.rtl : false;
    appStore.dark = props.dark !== undefined ? props.dark : false;
    appStore.locale = props.locale !== undefined ? props.locale : 'en';

    const updateAppStore = (vuetifyOptions: string | undefined | object) => {
      if (vuetifyOptions === undefined) {
        return;
      }
      try {
        const options =
          typeof vuetifyOptions === 'string'
            ? JSON.parse(vuetifyOptions)
            : vuetifyOptions;

        if (isPlainObject(options)) {
          appStore.defaults = options.defaults;
          appStore.blueprint = options.blueprint;

          if (options.icons?.defaultSet) {
            appStore.iconset = options.icons.defaultSet;
          }

          if (
            props.dark === undefined &&
            typeof options.theme?.dark === 'boolean'
          ) {
            appStore.dark = options.theme.dark;
          }
        }
      } catch (e) {
        console.log('vuetify-options error:', e);
      }
    };

    updateAppStore(props.vuetifyOptions);

    watch(
      () => props.rtl,
      (value, oldValue) => {
        if (value !== oldValue) {
          appStore.rtl = value !== undefined ? value : false;
        }
      },
    );
    watch(
      () => props.dark,
      (value, oldValue) => {
        if (value !== oldValue) {
          appStore.dark = value !== undefined ? value : false;
        }
      },
    );
    watch(
      () => props.locale,
      (value, oldValue) => {
        if (value !== oldValue) {
          appStore.locale = value !== undefined ? value : 'en';
        }
      },
    );
    watch(
      () => props.vuetifyOptions,
      (value, oldValue) => {
        if (value !== oldValue) {
          if (value) {
            updateAppStore(value);
          } else {
            updateAppStore({
              defaults: {},
              blueprint: 'md1',
              iconset: 'mdi',
            });
          }
        }
      },
    );

    return {
      appStore,
    };
  },
});

export default camundFormWc;
</script>
