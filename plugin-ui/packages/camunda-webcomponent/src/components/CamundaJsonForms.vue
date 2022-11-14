<template>
  <div>
    <custom-style type="text/css" id="vuetify-theme">
      {{ vuetifyThemeCss }}
    </custom-style>

    <custom-style type="text/css">
      {{ customStyle }}
    </custom-style>

    <v-app ref="root">
      <div v-if="error !== undefined">
        <v-container style="height: 400px">
          <v-row class="fill-height" align-content="center" justify="center">
            <v-col class="text-subtitle-1 text-center error" cols="12">
              {{ error }}
            </v-col>
          </v-row>
        </v-container>
      </div>
      <v-sheet v-else :dark="dark" tile>
        <camunda-resolved-json-forms
          :url="dataUrl"
          :processDefinitionId="dataProcessDefinitionId"
          :processDefinitionKey="dataProcessDefinitionKey"
          :taskId="dataTaskId"
          :locale="dataLocale"
          :defaultPreset="dataDefaultPreset"
          :config="dataConfig"
          :validationMode="dataValidationMode"
          :readonly="dataReadonly"
          @change="onChange"
          @load-request="onLoadRequest"
          @load-response="onLoadResponse"
          @load-error="onLoadError"
          @submit-request="onSubmitRequest"
          @submit-response="onSubmitResponse"
          @submit-error="onSubmitError"
        />
      </v-sheet>
    </v-app>
  </div>
</template>

<script lang="ts">
import { ValidationMode } from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { CamundaResolvedJsonForms } from '@kchobantonov/camunda-jsonforms';
import merge from 'lodash/merge';
import Vue, { defineComponent, PropType, ref } from 'vue';
import LoadScript from 'vue-plugin-load-script';
import { VApp, VSheet } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import vuetify, { preset as defaultPreset } from '../plugins/vuetify';

Vue.use(LoadScript);
Vue.config.productionTip = false;

const CustomStyle = defineComponent({
  name: 'custom-style',
  render(createElement) {
    return createElement('style', this.$slots.default);
  },
});

const camundaFormWc = defineComponent({
  vuetify,
  components: {
    CamundaResolvedJsonForms,
    VApp,
    VSheet,
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
      type: [String, Object],
      default: () => {
        return {
          restrict: true,
          trim: false,
          showUnfocusedDescription: false,
          hideRequiredAsterisk: true,
        };
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
      type: String,
      default: 'false',
    },
    validationMode: {
      required: false,
      type: [String] as PropType<ValidationMode>,
      default: 'ValidateAndShow',
      validator: function (value) {
        return (
          value === 'ValidateAndShow' ||
          value === 'ValidateAndHide' ||
          value == 'NoValidation'
        );
      },
    },
    locale: {
      required: false,
      type: String,
      default: 'en',
    },
    customStyle: {
      required: false,
      type: String,
      default: '.v-application--wrap { min-height: 0px; }',
    },
    defaultPreset: {
      required: false,
      type: [String],
      validator: function (value) {
        try {
          const config = typeof value == 'string' ? JSON.parse(value) : value;

          return config !== undefined && config !== null;
        } catch (e) {
          return false;
        }
      },
    },
  },
  setup(props) {
    let error: any = undefined;

    let dataUrl: string | undefined = undefined;
    let dataProcessDefinitionId: string | undefined = undefined;
    let dataProcessDefinitionKey: string | undefined = undefined;
    let dataTaskId: string | undefined = undefined;
    let dataConfig: Record<string, any> | undefined = undefined;
    let dataReadonly: boolean | undefined = undefined;
    let dataLocale: string | undefined = undefined;
    let dataValidationMode: ValidationMode | undefined = undefined;
    let dataDefaultPreset: Partial<VuetifyPreset> | undefined = undefined;

    try {
      dataConfig =
        typeof props.config == 'string'
          ? JSON.parse(props.config)
          : props.config;

      dataUrl = typeof props.url == 'string' ? props.url : undefined;
      dataProcessDefinitionId =
        typeof props.processDefinitionId == 'string'
          ? props.processDefinitionId
          : undefined;
      dataProcessDefinitionKey =
        typeof props.processDefinitionKey == 'string'
          ? props.processDefinitionKey
          : undefined;
      dataTaskId = typeof props.taskId == 'string' ? props.taskId : undefined;

      dataReadonly =
        typeof props.readonly == 'string'
          ? props.readonly == 'true'
          : props.readonly;
      dataValidationMode =
        props.validationMode == 'ValidateAndShow' ||
        props.validationMode == 'ValidateAndHide' ||
        props.validationMode == 'NoValidation'
          ? props.validationMode
          : 'ValidateAndShow';
      dataLocale = typeof props.locale == 'string' ? props.locale : 'en';
      dataDefaultPreset =
        typeof props.defaultPreset == 'string'
          ? merge({}, defaultPreset, JSON.parse(props.defaultPreset))
          : defaultPreset;
    } catch (e) {
      error = `Config error: ${e}`;
    }

    return {
      error,
      dataConfig,
      dataUrl,
      dataProcessDefinitionId,
      dataProcessDefinitionKey,
      dataTaskId,
      dataReadonly,
      dataValidationMode,
      dataLocale,
      dataDefaultPreset,
      vuetifyTheme: ref<{ generatedStyles: string }>(
        vuetify.framework.theme as any
      ),
    };
  },
  watch: {
    url: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataUrl = typeof value == 'string' ? value : undefined;
        }
      },
      deep: true,
    },
    processDefinitionId: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataProcessDefinitionId =
            typeof value == 'string' ? value : undefined;

          // reset other 2
          this.dataProcessDefinitionKey = undefined;
          this.dataTaskId = undefined;
        }
      },
      deep: true,
    },
    processDefinitionKey: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataProcessDefinitionKey =
            typeof value == 'string' ? value : undefined;

          // reset other 2
          this.dataProcessDefinitionId = undefined;
          this.dataTaskId = undefined;
        }
      },
      deep: true,
    },
    taskId: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataTaskId = typeof value == 'string' ? value : undefined;

          // reset other 2
          this.dataProcessDefinitionId = undefined;
          this.dataProcessDefinitionKey = undefined;
        }
      },
      deep: true,
    },
    config: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataConfig =
            typeof value == 'string' ? JSON.parse(value) : undefined;
        }
      },
      deep: true,
    },
    readonly: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataReadonly =
            typeof value == 'string' ? value == 'true' : value === true;
        }
      },
      deep: true,
    },
    validationMode: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataValidationMode =
            value == 'ValidateAndShow' ||
            value == 'ValidateAndHide' ||
            value == 'NoValidation'
              ? value
              : 'ValidateAndShow';
        }
      },
      deep: true,
    },
    locale: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataLocale = typeof value == 'string' ? value : 'en';
        }
      },
      deep: true,
    },
    defaultPreset: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataDefaultPreset =
            typeof value == 'string'
              ? merge({}, defaultPreset, JSON.parse(value))
              : undefined;

          this.applyTheme();
        }
      },
      deep: true,
    },
  },
  async mounted() {
    this.applyTheme();

    // include the fonts outside the webcomponent for now - https://github.com/google/material-design-icons/issues/1165
    if (this.$el.getRootNode() instanceof ShadowRoot) {
      let el = document.querySelector(
        'style[id="camunda-json-forms-materialdesignicons"]'
      );
      if (!el) {
        el = document.createElement('style');
        el.id = 'camunda-json-forms-materialdesignicons';

        const root = this.$el.getRootNode();
        if (root.hasChildNodes()) {
          let children = root.childNodes;
          for (const node of children) {
            if (
              node.nodeName.toLowerCase() === 'style' &&
              node.textContent?.startsWith(
                '@font-face{font-family:Material Design Icons;'
              )
            ) {
              el.textContent = node.textContent;
              break;
            }
          }
        }
        document.head.appendChild(el);
      }
    }
  },
  computed: {
    dark(): boolean {
      return this.dataDefaultPreset?.theme?.dark || false;
    },
    vuetifyThemeCss() {
      return this.vuetifyTheme?.generatedStyles;
    },
  },
  methods: {
    applyTheme(): void {
      let preset: Partial<VuetifyPreset> | null = null;
      if (this.input?.uischema?.options) {
        preset = this.vuetifyProps(
          this.input.uischema.options,
          'preset'
        ) as Partial<VuetifyPreset>;
      }

      // apply any themes
      this.$vuetify.theme = merge(
        this.$vuetify.theme,
        preset && preset.theme
          ? preset.theme
          : this.dataDefaultPreset?.theme || {}
      );

      this.$vuetify.icons = merge(
        this.$vuetify.icons,
        preset && preset.icons
          ? preset.icons
          : this.dataDefaultPreset?.icons || {}
      );
    },
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

<style scoped>
@import url('//fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900');
@import '~@mdi/font/css/materialdesignicons.min.css';
@import '~vuetify/dist/vuetify.min.css';
</style>
