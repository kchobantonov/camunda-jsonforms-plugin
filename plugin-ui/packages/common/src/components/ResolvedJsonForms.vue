<template>
  <div>
    <json-forms
      v-if="resolved && error === undefined"
      :data="data"
      :schema="schema"
      :uischema="uischema"
      :renderers="renderers"
      :cells="cells"
      :config="config"
      :uischemas="uischemas"
      :validationMode="validationMode"
      :ajv="ajv"
      :readonly="readonly"
      :i18n="i18n"
      @change="onChange"
    />
    <v-container v-else style="height: 400px">
      <v-row
        v-if="!resolved"
        class="fill-height"
        align-content="center"
        justify="center"
      >
        <v-col class="text-subtitle-1 text-center" cols="12">
          Resolving Schema...
        </v-col>
        <v-col cols="6">
          <v-progress-linear
            indeterminate
            rounded
            height="6"
          ></v-progress-linear>
        </v-col>
      </v-row>
      <v-row
        v-else-if="error !== undefined"
        class="fill-height"
        align-content="center"
        justify="center"
      >
        <v-col class="text-subtitle-1 text-center" cols="12">
          <v-alert color="red" dark
            >Error resolving schema: {{ error }}</v-alert
          >
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import {
  JsonFormsCellRendererRegistryEntry,
  JsonFormsI18nState,
  JsonFormsRendererRegistryEntry,
  JsonFormsUISchemaRegistryEntry,
  JsonSchema,
  Translator,
  ValidationMode,
} from '@jsonforms/core';
import { JsonForms, JsonFormsChangeEvent } from '@jsonforms/vue2';
import Ajv from 'ajv';
import { defineComponent, PropType, reactive, ref } from 'vue';
import { VAlert, VCol, VContainer, VProgressLinear, VRow } from 'vuetify/lib';
import { resolveRefs } from '../core/json-refs';
import { JsonFormInput } from '../core/types';
import { createAjv } from '../core/validate';
import { createTranslator } from '../i18n';
import { commonRenderers } from '../renderers/index';

export const resolvedJsonFormsProps = () => ({
  input: {
    required: true,
    type: [Object] as PropType<JsonFormInput>,
  },
  renderers: {
    required: false,
    type: [Array] as PropType<JsonFormsRendererRegistryEntry[]>,
    default: () => commonRenderers,
  },
  cells: {
    required: false,
    type: [Array] as PropType<JsonFormsCellRendererRegistryEntry[]>,
    default: () => commonRenderers,
  },
  config: {
    required: false,
    type: [Object] as PropType<Record<string, any>>,
  },
  readonly: {
    required: false,
    type: Boolean,
    default: false,
  },
  uischemas: {
    required: false,
    type: [Array] as PropType<JsonFormsUISchemaRegistryEntry[]>,
    default: () => [],
  },
  validationMode: {
    required: false,
    type: [String] as PropType<ValidationMode>,
    default: 'ValidateAndShow',
  },
  ajv: {
    required: false,
    type: [Object] as PropType<Ajv>,
    default: () => createAjv()
  },
  locale: {
    required: false,
    type: String,
    default: 'en',
  },
  translations: {
    required: false,
    type: [Object] as PropType<Record<string, any>>,
  },
});

const resolvedJsonForms = defineComponent({
  name: 'resolved-json-forms',
  components: {
    JsonForms,
    VContainer,
    VRow,
    VCol,
    VAlert,
    VProgressLinear,
  },
  emits: ['change'],
  props: {
    ...resolvedJsonFormsProps(),
  },
  setup(props) {
    const resolved = ref(false);
    const error = ref<any>(undefined);
    const schema = ref<JsonSchema | undefined>(undefined);
    const uischema = ref(props.input?.uischema);
    const data = ref(props.input?.data);
    const i18n = reactive({
      locale: props.locale,
      translations: props.translations,
      translate: createTranslator(props.locale, props.translations),
    } as JsonFormsI18nState & {
      translations: Record<string, any>;
      locale: string;
    });

    return {
      resolved,
      error,
      schema,
      uischema,
      data,
      i18n,
    };
  },
  watch: {
    input: {
      deep: true,
      handler(newInput: JsonFormInput, _oldInput: JsonFormInput): void {
        this.resolved = false;
        this.uischema = newInput.uischema;
        this.data = newInput.data;
        this.resolveSchema(newInput.schema, newInput.schemaUrl);
      },
    },
    locale(newLocale: string): void {
      this.i18n.locale = newLocale;
      this.i18n.translate = createTranslator(
        newLocale,
        this.i18n.translations
      ) as Translator;
    },
    translations(newTranslations: Record<string, any>): void {
      this.i18n.translations = newTranslations;
      this.i18n.translate = createTranslator(
        this.i18n.locale,
        this.i18n.translations
      ) as Translator;
    },
  },
  mounted() {
    this.resolveSchema(this.input?.schema, this.input?.schemaUrl);
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      this.$emit('change', event);
    },
    async resolveSchema(
      schema?: JsonSchema,
      schemaUrl?: string
    ): Promise<void> {
      this.resolved = false;

      try {
        if (schema) {
          this.schema = (
            await resolveRefs(schema, {
              location: schemaUrl,
            })
          ).resolved;
        } else {
          this.schema = schema;
        }
      } catch (err) {
        this.error = (err as Error).message;
      } finally {
        this.resolved = true;
      }
    },
  },
});

// cast to 'any' because of Typescript problems (ts(7056))
export default resolvedJsonForms as any;
</script>
