<template>
  <v-app>
    <slot name="link"></slot>
    <slot name="style"></slot>

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
      :ajv="ajv"
      :locale="locale"
      :translations="context.translations"
      :readonly="readonly"
      @change="onChange"
    />
  </v-app>
</template>

<script lang="ts">
import ResolvedJsonForms from './components/ResolvedJsonForms.vue';
import { createAjv } from '@jsonforms/vue2-vuetify';
import { allRenderers } from './renderers';
import { CamundaFormApi } from './core/api';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { CamundaFormConfig, CamundaFormContext } from './core/types';
import vuetify from './plugins/vuetify';
import { defineComponent, ref, toRefs } from '@vue/composition-api';
import { VApp, VContainer, VRow, VCol, VProgressLinear } from 'vuetify/lib';

const ajv = createAjv({ useDefaults: true });

interface CamundaFormProps {
  camundaUrl: string;
  processDefinitionId: string;
  formUrl: string;
  taskId?: string;
  locale?: string;
}

const camundaForm = defineComponent({
  name: 'camunda-json-forms',
  vuetify,
  components: {
    ResolvedJsonForms,
    VApp,
    VContainer,
    VRow,
    VCol,
    VProgressLinear,
  },
  emits: [
    'change',
    'load-error',
    'submit-headers-built',
    'submit-success-response',
    'submit-error-response',
    'submit-error',
  ],
  props: {
    camundaUrl: {
      required: true,
      type: String,
      validator: (value: string) => {
        return value.trim().length > 0;
      },
    },
    processDefinitionId: {
      required: true,
      type: String,
      validator: (value: string) => {
        return value.trim().length > 0;
      },
    },
    formUrl: {
      required: true,
      type: String,
      validator: (value: string) => {
        return value.trim().length > 0;
      },
    },
    taskId: {
      required: false,
      type: String,
      validator: (value: string) => {
        return value.trim().length > 0;
      },
    },
    locale: {
      required: false,
      type: String,
      default: 'en',
      validator: (value: string) => {
        return value.trim().length > 0;
      },
    },
  },
  setup(props: CamundaFormProps) {
    const context = ref<CamundaFormContext | null>(null);
    
    return {
      loading: false,
      context: context,

      readonly: false,
      validationMode: 'ValidateAndShow',
      jsonformsConfig: {
        restrict: true,
        trim: false,
        showUnfocusedDescription: false,
        hideRequiredAsterisk: true,
      },
      config: {
        locale: props.locale,
        camundaUrl: props.camundaUrl,
        processDefinitionId: props.processDefinitionId,
        formUrl: props.formUrl,
        taskId: props.taskId,
      } as CamundaFormConfig,
      renderers: allRenderers,
      cells: allRenderers,
      ajv,
    };
  },
  async created() {
    this.loading = true;

    try {
      const api = new CamundaFormApi(this.config);

      const context = await api.getCamundaFormContext();
      this.context = context;
    } catch (e) {
      console.log('load error: ' + e);
      this.$emit('load-error', e);
    } finally {
      this.loading = false;
    }
  },
  mounted() {
    this.$root.$on('submit-headers-built', (event: Event) => {
      this.$emit('submit-headers-built', event);
    });
    this.$root.$on('submit-success-response', (event: Event) => {
      this.$emit('submit-success-response', event);
    });
    this.$root.$on('submit-error-response', (event: Event) => {
      this.$emit('submit-error-response', event);
    });
    this.$root.$on('submit-error', (event: Event) => {
      this.$emit('submit-error', event);
    });
  },
  provide() {
    // provide as a reactive property
    const { context } = toRefs(this);

    return {
      camundaFormConfig: this.config,
      camundaFormContext: context,
    };
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      this.$emit('change', event);
    },
  },
});

export default camundaForm;
</script>

<style></style>
