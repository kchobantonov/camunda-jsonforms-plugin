<template>
  <v-app ref="root">
    <custom-style type="text/css" id="vuetify-theme">
      {{ vuetifyThemeCss }}
    </custom-style>

    <slot
      name="style"
      v-if="!!$slots['style'] || !!$scopedSlots['style']"
    ></slot>
    <custom-style type="text/css" v-else>
      .v-application--wrap { min-height: 0px; }
    </custom-style>

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
        :url="url"
        :processDefinitionId="processDefinitionId"
        :processDefinitionKey="processDefinitionKey"
        :taskId="taskId"
        :locale="locale"
        :defaultPreset="dataDefaultPreset"
        :config="dataConfig"
        :validationMode="dataValidationMode"
        :readonly="readonly"
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
</template>

<script lang="ts">
import Vue from 'vue';
import { ValidationMode } from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { CamundaResolvedJsonForms } from '@kchobantonov/camunda-jsonforms';
import { merge } from 'lodash';
import { defineComponent, PropType, ref } from 'vue';
import { VApp, VSheet } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import vuetify, { preset as defaultPreset } from '../plugins/vuetify';
import LoadScript from 'vue-plugin-load-script';

Vue.use(LoadScript);
Vue.config.productionTip = false;

const CustomStyle = defineComponent({
  name: 'custom-style',
  render(createElement) {
    return createElement('style', this.$slots.default);
  },
});

const theme = vuetify.framework.theme as any;
// force vuetify to use checkOrCreateStyleElement
theme.vueMeta = null;
theme.checkOrCreateStyleElement = function () {
  // do not update any style elements
  return false;
};

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
      type: [String, Boolean],
      default: false,
    },
    validationMode: {
      required: false,
      type: [String] as PropType<ValidationMode>,
      default: 'ValidateAndShow',
      validator: function (value) {
        return value === 'ValidateAndShow' || value === 'ValidateAndHide' || value == 'NoValidation'
      }      
    },
    locale: {
      required: false,
      type: String,
      default: 'en',
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
    let dataConfig: any = undefined;
    let dataValidationMode: any = undefined;
    let dataDefaultPreset: Partial<VuetifyPreset> | undefined = undefined;

    try {
      dataConfig =
        typeof props.config == 'string'
          ? JSON.parse(props.config)
          : props.config;

      dataValidationMode =
        props.validationMode == 'ValidateAndShow' ||
        props.validationMode == 'ValidateAndHide' ||
        props.validationMode == 'NoValidation'
          ? props.validationMode
          : 'ValidateAndShow';

      dataDefaultPreset =
        typeof props.defaultPreset == 'string'
          ? merge({}, defaultPreset, JSON.parse(props.defaultPreset))
          : defaultPreset;
    } catch (e) {
      error = e;
    }

    return {
      error,
      dataConfig,
      dataValidationMode,
      dataDefaultPreset,
      vuetifyTheme: ref<{ generatedStyles: string }>(theme),
    };
  },
  async mounted() {
    let preset: Partial<VuetifyPreset> | null = null;
    if (this.input?.uischema?.options) {
      preset = this.vuetifyProps(
        this.input.uischema.options,
        'preset'
      ) as Partial<VuetifyPreset>;
    }

    const shadowRoot = (this.$refs['root'] as any).$el as HTMLDivElement;

    // Monkey patch querySelector to properly find root element
    const { querySelector } = document;
    document.querySelector = function (selector: any) {
      if (selector === '[data-app]') return shadowRoot;
      return querySelector.call(this, selector);
    };

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
  computed: {
    dark() {
      return this.dataDefaultPreset?.theme?.dark || false;
    },
    vuetifyThemeCss() {
      return this.vuetifyTheme?.generatedStyles;
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

<style scoped>
@import url('//fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900');
@import url('//cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css');

@import '~vuetify/dist/vuetify.min.css';
</style>
