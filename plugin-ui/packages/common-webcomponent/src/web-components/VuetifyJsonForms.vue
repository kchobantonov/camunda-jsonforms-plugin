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
      <resolved-json-forms
        :input="input"
        :uidata="dataUiData"
        :uischemas="dataUischemas"
        :renderers="renderers"
        :cells="cells"
        :config="dataConfig"
        :validationMode="dataValidationMode"
        :locale="dataLocale"
        :translations="dataTranslations"
        :readonly="dataReadonly"
        :actions="actions"
        @change="onChange"
      />
    </v-sheet>
  </v-app>
</template>

<script lang="ts">
import {
  JsonFormsUISchemaRegistryEntry,
  UISchemaElement,
  ValidationMode,
} from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import {
  commonRenderers,
  ResolvedJsonForms,
  Actions,
} from '@kchobantonov/common-jsonforms';
import { isArray, isPlainObject, merge } from 'lodash';
import get from 'lodash/get';
import Vue, { defineComponent, PropType, ref } from 'vue';
import LoadScript from 'vue-plugin-load-script';
import { VApp, VSheet } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import { VuetifyFormConfig } from '../core';
import vuetify, { preset as defaultPreset } from '../plugins/vuetify';

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

const vuetifyFormWc = defineComponent({
  vuetify,
  components: {
    ResolvedJsonForms,
    VApp,
    VSheet,
    CustomStyle,
  },
  emits: ['change', 'init'],
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
    uischemas: {
      required: false,
      type: [String],
      validator: function (value) {
        try {
          const uischemas =
            typeof value == 'string' ? JSON.parse(value) : value;

          return isArray(uischemas);
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
    uidata: {
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
  setup(props: VuetifyFormConfig, context) {
    let error: any = undefined;

    let schema: any = undefined;
    let uischema: any = undefined;
    let dataUischemas: JsonFormsUISchemaRegistryEntry[] = [];
    let data: any = undefined;
    let dataConfig: Record<string, any> | undefined = undefined;
    let dataReadonly: boolean | undefined = undefined;
    let dataLocale: string | undefined = undefined;
    let dataValidationMode: ValidationMode | undefined = undefined;
    let dataTranslations: Record<string, any> | undefined = undefined;
    let dataDefaultPreset: Partial<VuetifyPreset> | undefined = undefined;

    let actions: Record<string, any> = ref({}); // Record<string, Function>
    let dataUiData: Record<string, any> = ref({}); // Record<string, Function>

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
      dataLocale = typeof props.locale == 'string' ? props.locale : 'en';
      dataTranslations =
        typeof props.translations == 'string'
          ? JSON.parse(props.translations)
          : props.translations;
      dataDefaultPreset =
        typeof props.defaultPreset == 'string'
          ? merge({}, defaultPreset, JSON.parse(props.defaultPreset))
          : defaultPreset;
      dataUiData =
        typeof props.uidata == 'string'
          ? JSON.parse(props.uidata)
          : props.uidata;
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
      dataUischemas,
      dataConfig,
      dataReadonly,
      dataValidationMode,
      dataLocale,
      dataTranslations,
      dataDefaultPreset,
      vuetifyTheme: ref<{ generatedStyles: string }>(theme),
      actions,
      dataUiData,
    };
  },
  watch: {
    schema: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          const schema =
            typeof value == 'string' ? JSON.parse(value) : undefined;
          this.input = {
            schema: schema,
            uischema: this.input.uischema,
            data: this.input.data,
          };
        }
      },
      deep: true,
    },
    uischema: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          const uischema =
            typeof value == 'string' ? JSON.parse(value) : undefined;

          this.input = {
            schema: this.input.schema,
            uischema: uischema,
            data: this.input.data,
          };
        }
      },
      deep: true,
    },
    uischemas: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.updateUISchemas();
        }
      },
      deep: true,
    },
    data: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          const data = typeof value == 'string' ? JSON.parse(value) : undefined;

          this.input = {
            schema: this.input.schema,
            uischema: this.input.uischema,
            data: data,
          };
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
    translations: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataTranslations =
            typeof value == 'string' ? JSON.parse(value) : undefined;
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
    const shadowRoot = (this.$refs['root'] as any).$el as HTMLDivElement;

    // Monkey patch querySelector to properly find root element
    const { querySelector } = document;
    document.querySelector = function (selector: any) {
      if (selector === '[data-app]') return shadowRoot;
      return querySelector.call(this, selector);
    };

    this.applyTheme();

    this.$emit('init', {
      registerActions: this.registerActions.bind(this),
    });
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
    updateUISchemas(): void {
      const uischemasMap: {
        tester: string;
        uischema: UISchemaElement;
      }[] = typeof this.uischemas == 'string' ? JSON.parse(this.uischemas) : [];

      this.dataUischemas = uischemasMap
        .map((elem) => {
          const action = this.actions[elem.tester];
          if (action) {
            return {
              tester: action,
              uischema: elem.uischema,
            };
          }
          return null;
        })
        .filter((x) => !!x) as JsonFormsUISchemaRegistryEntry[];
    },
    registerActions(actions: Actions) {
      this.actions = actions || {};
      this.updateUISchemas();
    },
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

<style scoped>
@import url('//fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900');
@import url('//cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css');

@import '~vuetify/dist/vuetify.min.css';
</style>
