<template>
  <v-app>
    <slot name="link"></slot>
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
    <resolved-json-forms
      v-else
      :input="input"
      :renderers="renderers"
      :cells="cells"
      :config="config"
      :validationMode="validationMode"
      :locale="locale"
      :translations="translations"
      :readonly="readonly"
      @change="onChange"
    />
  </v-app>
</template>

<script lang="ts">
import { ValidationMode } from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { commonRenderers, ResolvedJsonForms } from '@kchobantonov/common-jsonforms';
import { defineComponent, PropType, ref } from 'vue';
import { merge } from 'lodash';
import { VApp } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import { VuetifyFormConfig } from '../core';
import vuetify from '../plugins/vuetify';
import isPlainObject from 'lodash/isPlainObject';
import get from 'lodash/get';

const vuetifyFormWc = defineComponent({
  name: 'vuetify-json-forms-wc',
  vuetify,
  components: {
    ResolvedJsonForms,
    VApp,
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
      type: [String, Object],
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
    let config: any = undefined;
    let readonly: any = undefined;
    let validationMode: any = undefined;
    let translations: any = undefined;
    let defaultPreset: any = undefined;

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

      config =
        typeof props.config == 'string'
          ? JSON.parse(props.config)
          : props.config;
      readonly =
        typeof props.readonly == 'string'
          ? props.readonly == 'true'
          : props.readonly;
      validationMode =
        props.validationMode == 'ValidateAndShow' ||
        props.validationMode == 'ValidateAndHide' ||
        props.validationMode == 'NoValidation'
          ? props.validationMode
          : 'ValidateAndShow';
      translations =
        typeof props.translations == 'string'
          ? JSON.parse(props.translations)
          : props.translations;
      defaultPreset =
        typeof props.defaultPreset == 'string'
          ? JSON.parse(props.defaultPreset)
          : props.defaultPreset;
    } catch (e) {
      error = e;
    }

    return {
      error: error,
      renderers: commonRenderers,
      cells: commonRenderers,
      input: {
        schema: schema,
        uischema: uischema,
        data: data,
      },
      config: config,
      readonly: readonly,
      validationMode: validationMode,
      locale: props.locale,
      translations: translations,
      style: props.style,
      defaultPreset: defaultPreset,
    };
  },
  async mounted() {
    // apply any themes
    if (this.input?.uischema?.options) {
      const preset = this.vuetifyProps(
        this.input.uischema.options,
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
        this.defaultPreset.theme
      );
      this.$vuetify.icons = merge(
        this.$vuetify.icons,
        this.defaultPreset.icons
      );
    }
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

<style></style>
