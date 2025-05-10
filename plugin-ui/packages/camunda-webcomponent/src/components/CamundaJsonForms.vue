<template>
  <Suspense>
    <div>
      <custom-style type="text/css" id="vuetify-theme-stylesheet">
        {{ vuetifyThemeCss }}
      </custom-style>

      <custom-style type="text/css">
        {{ customStyleToUse }}
      </custom-style>

      <v-locale-provider :rtl="appStore.rtl" :locale="appStore.locale">
        <v-theme-provider :theme="theme">
          <v-defaults-provider :defaults="appStore.defaults">
            <div v-if="error !== undefined">
              <v-container style="height: 400px">
                <v-row
                  class="fill-height"
                  align-content="center"
                  justify="center"
                >
                  <v-col class="text-subtitle-1 text-center error" cols="12">
                    {{ error }}
                  </v-col>
                </v-row>
              </v-container>
            </div>
            <camunda-resolved-json-forms
              :url="urlToUse"
              :processDefinitionId="processDefinitionIdToUse"
              :processDefinitionKey="processDefinitionKeyToUse"
              :taskId="taskIdToUse"
              :locale="localeToUse"
              :vuetify-config="vuetifyConfig"
              :config="configToUse"
              :validationMode="validationModeToUse"
              :readonly="readonlyToUse"
              @change="onChange"
              @load-request="onLoadRequest"
              @load-response="onLoadResponse"
              @load-error="onLoadError"
              @submit-request="onSubmitRequest"
              @submit-response="onSubmitResponse"
              @submit-error="onSubmitError"
            ></camunda-resolved-json-forms>
          </v-defaults-provider>
        </v-theme-provider>
      </v-locale-provider>
    </div>
  </Suspense>
</template>

<script lang="ts">
import { useAppStore } from '@/store';
import { CamundaResolvedJsonForms } from '@chobantonov/camunda-jsonforms';
import {
  HandleActionEmitterKey,
  type VuetifyConfig
} from '@chobantonov/jsonforms-vuetify-renderers';
import {
  type ValidationMode
} from '@jsonforms/core';
import { type JsonFormsChangeEvent } from '@jsonforms/vue';
import {
  computed,
  defineComponent,
  h,
  inject,
  type InjectionKey,
  type PropType,
  ref,
  watch
} from 'vue';
import { type ThemeInstance } from 'vuetify';
import {
  VDefaultsProvider,
  VLocaleProvider,
  VThemeProvider,
} from 'vuetify/components';
import { extractAndInjectFonts } from '../util/inject-fonts';

const ThemeSymbol: InjectionKey<ThemeInstance> = Symbol.for('vuetify:theme');

const CustomStyle = defineComponent({
  name: 'custom-style',
  setup(_, { slots }) {
    return () => h('style', slots.default ? slots.default() : []);
  },
});

const camundaFormWc = defineComponent({
  components: {
    CamundaResolvedJsonForms,
    VThemeProvider,
    VLocaleProvider,
    VDefaultsProvider,
    CustomStyle,
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
    config: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const config = typeof value === 'string' ? JSON.parse(value) : value;

          return config !== undefined && config !== null;
        } catch (e) {
          return false;
        }
      },
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
      validator: function (value) {
        return (
          value === 'ValidateAndShow' ||
          value === 'ValidateAndHide' ||
          value === 'NoValidation'
        );
      },
    },
    customStyle: {
      required: false,
      type: String,
    },
  },
  async setup(props) {
    const appStore = useAppStore();

    const getJsonDataType = (value: any): string | null => {
      if (typeof value === 'string') {
        return 'string';
      } else if (typeof value === 'number') {
        return Number.isInteger(value) ? 'integer' : 'number';
      } else if (typeof value === 'boolean') {
        return 'boolean';
      } else if (Array.isArray(value)) {
        return 'array';
      } else if (value === null) {
        return 'null';
      } else if (typeof value === 'object') {
        return 'object';
      }

      return null;
    };

    const toJsonData = (
      prop: string,
      jsonType:
        | 'string'
        | 'integer'
        | 'number'
        | 'boolean'
        | 'array'
        | 'null'
        | 'object'
        | 'any',
      value: string | undefined,
    ) => {
      try {
        const result = typeof value == 'string' ? JSON.parse(value) : undefined;
        if (jsonType !== 'any' && result !== undefined) {
          const resultType = getJsonDataType(result);
          if (resultType !== jsonType) {
            throw new Error(
              `Invalid data type. Expected ${jsonType} but found ${resultType}`,
            );
          }
        }
        return result;
      } catch (e) {
        error.value = `${prop} error: ${e}`;
        console.log(e);

        return undefined;
      }
    };

    let error = ref<string | undefined>(undefined);
    let urlToUse = ref<string | undefined>(props.url);
    let processDefinitionIdToUse = ref<string | undefined>(
      props.processDefinitionId,
    );
    let processDefinitionKeyToUse = ref<string | undefined>(
      props.processDefinitionKey,
    );
    let taskIdToUse = ref<string | undefined>(props.taskId);
    let configToUse = ref<Record<string, any>>(toJsonData('config', 'object', props.config));
    let readonlyToUse = ref<boolean | undefined>(props.readonly);
    let validationModeToUse = ref(
      props.validationMode === 'ValidateAndShow' ||
        props.validationMode === 'ValidateAndHide' ||
        props.validationMode === 'NoValidation'
        ? props.validationMode
        : 'ValidateAndShow',
    );
    let customStyleToUse = ref(props.customStyle);
    let localeToUse = ref<string | undefined>(appStore.locale);

    watch(
      () => props.url,
      (value, oldValue) => {
        if (value !== oldValue) {
          urlToUse.value = value;
        }
      },
    );
    watch(
      () => props.processDefinitionId,
      (value, oldValue) => {
        if (value !== oldValue) {
          processDefinitionIdToUse.value = value;

          // reset other 2
          processDefinitionKeyToUse.value = undefined;
          taskIdToUse.value = undefined;
        }
      },
    );
    watch(
      () => props.processDefinitionKey,
      (value, oldValue) => {
        if (value !== oldValue) {
          processDefinitionKeyToUse.value = value;

          // reset other 2
          processDefinitionIdToUse.value = undefined;
          taskIdToUse.value = undefined;
        }
      },
    );
    watch(
      () => props.taskId,
      (value, oldValue) => {
        if (value !== oldValue) {
          taskIdToUse.value = value;

          // reset other 2
          processDefinitionIdToUse.value = undefined;
          processDefinitionKeyToUse.value = undefined;
        }
      },
    );
    watch(
      () => props.config,
      (value, oldValue) => {
        if (value !== oldValue) {
          configToUse.value = toJsonData('config', 'object', value);
        }
      },
    );
    watch(
      () => props.readonly,
      (value, oldValue) => {
        if (value !== oldValue) {
          readonlyToUse.value = value;
        }
      },
    );
    watch(
      () => props.validationMode,
      (value, oldValue) => {
        if (value !== oldValue) {
          validationModeToUse.value =
            value === 'ValidateAndShow' ||
            value === 'ValidateAndHide' ||
            value === 'NoValidation'
              ? value
              : 'ValidateAndShow';
        }
      },
    );
    watch(
      () => props.customStyle,
      (customStyle, oldCustomStyle) => {
        if (customStyle !== oldCustomStyle) {
          customStyleToUse.value = customStyle;
        }
      },
    );
    watch(
      () => appStore.locale,
      (value, oldValue) => {
        if (value !== oldValue) {
          localeToUse.value = value;
        }
      },
    );

    const theme = computed(() => {
      return appStore.dark ? 'dark' : 'light';
    });

    const vuetifyConfig = computed<VuetifyConfig>(() => ({
      theme: theme.value,
      rtl: appStore.rtl,
      defaults: appStore.defaults,
    }));

    return {
      error,
      urlToUse,
      processDefinitionIdToUse,
      processDefinitionKeyToUse,
      taskIdToUse,
      configToUse,
      readonlyToUse,
      validationModeToUse,
      customStyleToUse,
      localeToUse,
      appStore,
      theme,
      vuetifyConfig,
    };
  },
  provide() {
    return {
      // provide the this.$emit to be used as handleActionEmitter since this emitter is connected to the native web component
      [HandleActionEmitterKey]: this.$root!.$emit,
    };
  },
  async mounted() {
    extractAndInjectFonts(this.$el.getRootNode());
  },
  computed: {
    vuetifyThemeCss() {
      const theme = inject(ThemeSymbol);

      let css = theme?.styles.value;
      if (css && css.startsWith(':root {')) {
        // change to host if the variable generation is enabled
        css = ':host {' + css.substring(':root {'.length, css.length);
      }
      return css;
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
});

export default camundaFormWc;
</script>
