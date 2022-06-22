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
      :ajv="ajvProvider()"
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
ValidationMode
} from '@jsonforms/core';
import { JsonForms, JsonFormsChangeEvent } from '@jsonforms/vue2';
import { CompType } from '@jsonforms/vue2-vuetify/lib/vue';
import { defineComponent } from '@vue/composition-api';
import Ajv from 'ajv';
import { resolveRefs } from '../core/json-refs';
import { FormConfig, JsonFormInput } from '../core/types';
import { createAjv } from '../core/validate';
import { createTranslator } from '../i18n';
import { commonRenderers } from '../renderers/index';
import { VAlert, VCol, VContainer, VProgressLinear, VRow } from 'vuetify/lib';

export const resolvedJsonFormsProps = () => ({
  input: {
    required: true,
    type: [Object] as CompType<JsonFormInput, [ObjectConstructor]>,
  },
  renderers: {
    required: false,
    type: [Array] as CompType<
      JsonFormsRendererRegistryEntry,
      [ArrayConstructor]
    >,
    default: () => commonRenderers,
  },
  cells: {
    required: false,
    type: [Array] as CompType<
      JsonFormsCellRendererRegistryEntry[],
      [ArrayConstructor]
    >,
    default: () => commonRenderers,
  },
  config: {
    required: false,
    type: [Object] as CompType<Record<string, any>, [ObjectConstructor]>,
  },
  readonly: {
    required: false,
    type: Boolean,
    default: false,
  },
  uischemas: {
    required: false,
    type: [Array] as CompType<
      JsonFormsUISchemaRegistryEntry,
      [ArrayConstructor]
    >,
    default: () => [],
  },
  validationMode: {
    required: false,
    type: [String] as CompType<ValidationMode, [StringConstructor]>,
    default: 'ValidateAndShow',
  },
  ajv: {
    required: false,
    type: [Object] as CompType<Ajv, [ObjectConstructor]>,
  },
  locale: {
    required: false,
    type: String,
    default: 'en',
  },
  translations: {
    required: false,
    type: [Object] as CompType<Record<string, any>, [ObjectConstructor]>,
  },
});

const resolvedJsonForms = defineComponent({
  name: 'resolved-json-forms',
  components: {
    JsonForms,
    VContainer,
    VRow,
    VCol,
    VProgressLinear,
    VAlert,
  },
  emits: ['change'],
  props: {
    ...resolvedJsonFormsProps(),
  },
  setup(props: FormConfig) {
    let ajv = props.ajv;

    const ajvProvider = () => {
      if (!ajv) {
        ajv = createAjv();
      }
      return ajv;
    };

    return {
      ajvProvider: ajvProvider,
      resolved: false,
      error: undefined as any,
      schema: undefined as JsonSchema | undefined,
      uischema: props.input.uischema,
      data: props.input.data,
      i18n: {
        locale: props.locale,
        translations: props.translations,
        translate: createTranslator(props.locale, props.translations),
      } as JsonFormsI18nState & {
        translations: Record<string, any>;
        locale: string;
      },
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
    this.resolveSchema(this.input.schema, this.input.schemaUrl);
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
        }
      } catch (err) {
        this.error = (err as Error).message;
      } finally {
        this.resolved = true;
      }
    },
  },
});

export default resolvedJsonForms;
</script>
