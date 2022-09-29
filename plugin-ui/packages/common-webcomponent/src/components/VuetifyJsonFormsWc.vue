<template>
  <v-app ref="root">
    <slot name="link"></slot>

    <custom-style type="text/css" id="vuetify-theme">
      {{ vuetifyThemeCss }}
    </custom-style>
    <slot name="style"></slot>

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
      <resolved-json-forms
        :input="input"
        :renderers="renderers"
        :cells="cells"
        :config="dataConfig"
        :validationMode="dataValidationMode"
        :locale="locale"
        :translations="dataTranslations"
        :readonly="dataReadonly"
        @change="onChange"
      />
    </v-sheet>
  </v-app>
</template>

<script lang="ts">
import { ValidationMode } from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { commonRenderers, ResolvedJsonForms } from '@kchobantonov/common-jsonforms';
import { merge } from 'lodash';
import get from 'lodash/get';
import isPlainObject from 'lodash/isPlainObject';
import { defineComponent, PropType, ref } from 'vue';
import { VApp, VSheet } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import { VuetifyFormConfig } from '../core';
import vuetify, { preset as defaultPreset } from '../plugins/vuetify';

const CustomStyle = defineComponent({
  name: 'custom-style',
  render(createElement) {
    return createElement('style', this.$slots.default);
  },
});

const vuetifyFormWc = defineComponent({
  name: 'vuetify-json-forms-wc',
  vuetify,
  components: {
    ResolvedJsonForms,
    VApp,
    VSheet,
    CustomStyle,
  },
  emits: ['change'],
  props: {
    schema: {
      required: true,
      type: [String, Object],
      validator: function (value) {
        try {
          const schema = typeof value == 'string' ? JSON.parse(value) : value;

          return schema !== undefined && schema !== null;
        } catch (e) {
          return false;
        }
      },
    },
    uischema: {
      required: false,
      type: [String, Object],
      validator: function (value) {
        try {
          const uischema = typeof value == 'string' ? JSON.parse(value) : value;

          return uischema !== null;
        } catch (e) {
          return false;
        }
      },
    },
    data: {
      required: false,
      type: [String, Object],
      default: () => {
        return {};
      },
      validator: function (value) {
        try {
          const data = typeof value == 'string' ? JSON.parse(value) : value;

          return data !== undefined && data !== null;
        } catch (e) {
          return false;
        }
      },
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
    },
    locale: {
      required: false,
      type: String,
      default: 'en',
    },
    translations: {
      required: false,
      type: [String, Object],
      validator: function (value) {
        try {
          const translations =
            typeof value == 'string' ? JSON.parse(value) : value;

          return translations !== null;
        } catch (e) {
          return false;
        }
      },
    },
    defaultPreset: {
      required: false,
      type: [String],
      validator: function (value) {
        try {
          const preset = typeof value == 'string' ? JSON.parse(value) : value;

          return preset !== undefined && preset !== null;
        } catch (e) {
          return false;
        }
      },
    },
  },
  setup(props: VuetifyFormConfig) {
    let error: any = undefined;

    let schema: any = undefined;
    let uischema: any = undefined;
    let data: any = undefined;
    let dataConfig: any = undefined;
    let dataReadonly: any = undefined;
    let dataValidationMode: any = undefined;
    let dataTranslations: any = undefined;
    let dataDefaultPreset: Partial<VuetifyPreset> | undefined = undefined;

    try {
      schema =
        typeof props.schema == 'string'
          ? JSON.parse(props.schema)
          : props.schema;
      uischema =
        typeof props.uischema == 'string'
          ? JSON.parse(props.uischema)
          : props.uischema;
      data =
        typeof props.data == 'string' ? JSON.parse(props.data) : props.data;

      dataConfig =
        typeof props.config == 'string'
          ? JSON.parse(props.config)
          : props.config;
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
      dataTranslations =
        typeof props.translations == 'string'
          ? JSON.parse(props.translations)
          : props.translations;
      dataDefaultPreset =
        typeof props.defaultPreset == 'string'
          ? merge({}, defaultPreset, JSON.parse(props.defaultPreset))
          : defaultPreset;
    } catch (e) {
      error = `Config error: ${e}`;
    }

    return {
      error,
      renderers: commonRenderers,
      cells: commonRenderers,
      input: {
        schema: schema,
        uischema: uischema,
        data: data,
      },
      dataConfig,
      dataReadonly,
      dataValidationMode,
      dataTranslations,
      dataDefaultPreset,
      vuetifyTheme: ref<{ generatedStyles: string } | null>(null),
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

    const theme = vuetify.framework.theme as any;
    // force vutify to use checkOrCreateStyleElement
    theme.vueMeta = null;
    theme.checkOrCreateStyleElement = function () {
      // do not update any style elements
      return false;
    };   
    this.vuetifyTheme = theme;

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
    vuetifyProps(
      options: Record<string, any>,
      path: string
    ): Record<string, any> {
      const props = get(options?.vuetify, path);

      return props && isPlainObject(props) ? props : {};
    },
  },
});

export default vuetifyFormWc;
</script>
