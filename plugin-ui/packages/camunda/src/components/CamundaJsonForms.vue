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
      v-if="context != null"
      :input="context.input"
      :renderers="renderers"
      :cells="cells"
      :config="jsonformsConfig"
      :validationMode="validationMode"
      :ajv="$ajv"
      :locale="locale"
      :translations="context.translations"
      :readonly="readonly"
      @change="onChange"
    />
  </div>
</template>

<script lang="ts">
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { CompType } from '@jsonforms/vue2-vuetify/lib/vue';
import {
  createAjv,
  LoadEmitter,
  ResolvedJsonForms,
  ResponseOkInterceptor,
  RestClient,
} from '@kchobantonov/common-jsonforms';
import { defineComponent, ref, toRefs } from '@vue/composition-api';
import get from 'lodash/get';
import isPlainObject from 'lodash/isPlainObject';
import merge from 'lodash/merge';
import { VCol, VContainer, VProgressLinear, VRow } from 'vuetify/lib';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import { CamundaFormApi } from '../core/api';
import { CamundaFormConfig, CamundaFormContext } from '../core/types';
import { validateCamundaFormConfig } from '../core/validate';
import { camundaRenderers } from '../renderers';

const camundaForm = defineComponent({
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
    locale: {
      required: false,
      type: String,
      default: 'en',
    },
    defaultPreset: {
      required: false,
      type: [Object] as CompType<Partial<VuetifyPreset>, [ObjectConstructor]>,
      default: {
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
      },
    },
  },
  setup(props: CamundaFormConfig) {
    const context = ref<CamundaFormContext | null>(null);
    const api = ref<CamundaFormApi | null>(null);

    return {
      loading: false,
      context: context,
      api: api,
      props: props,

      readonly: false,
      validationMode: 'ValidateAndShow',
      jsonformsConfig: {
        restrict: true,
        trim: false,
        showUnfocusedDescription: false,
        hideRequiredAsterisk: true,
      },
      renderers: camundaRenderers,
      cells: camundaRenderers,
    };
  },
  created() {
    this.$ajv = createAjv();
  },
  async mounted() {
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
  computed: {
    config(): CamundaFormConfig {
      return validateCamundaFormConfig(this.props);
    },
  },
  provide() {
    // provide as a reactive property
    const { context, api } = toRefs(this);

    return {
      formConfig: this.config,
      formContext: context,
      camundaFormApi: api,
      camundaFormEmitter: this.$emit.bind(this),
    };
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      this.$emit('change', event);
    },
    async loadContext() {
      this.loading = true;

      try {
        this.api = new CamundaFormApi(this.config);

        const restClient = new RestClient([
          new LoadEmitter(this.$emit.bind(this)),
          new ResponseOkInterceptor(),
        ]);
        const context = await this.api.loadForm(restClient);

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
      const props = get(options?.vuetify, path);

      return props && isPlainObject(props) ? props : {};
    },
  },
});

export default camundaForm;
</script>

<style></style>
