<template>
  <div>
    <json-forms
      v-if="resolved && error === undefined"
      :data="input.data"
      :schema="schema"
      :uischema="input.uischema"
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
    <v-container v-else>
      <v-row
        v-if="!resolved"
        class="fill-height"
        align-content="center"
        justify="center"
      >
        <v-col class="text-subtitle-1 text-center" cols="12"
          >Resolving Schema...</v-col
        >
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
          <v-alert color="red" dark>{{ error }}</v-alert>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import { CamundaJsonFormInput, ResolvedSchema } from '../core/types';
import Ajv from 'ajv';
import {
  JsonFormsUISchemaRegistryEntry,
  JsonFormsRendererRegistryEntry,
  JsonFormsCellRendererRegistryEntry,
  JsonSchema,
  JsonFormsI18nState,
  ValidationMode,
  Translator,
} from '@jsonforms/core';
import { JsonForms, JsonFormsChangeEvent } from '@jsonforms/vue2';
import JsonRefs from 'json-refs';
import { createTranslator } from '../i18n';
import { defineComponent } from '@vue/composition-api';
import { CompType } from '../config/config';

export const resolvedJsonFormsProps = () => ({
  input: {
    required: true,
    type: [Object] as CompType<CamundaJsonFormInput, [ObjectConstructor]>,
  },
  renderers: {
    required: true,
    type: Array as CompType<JsonFormsRendererRegistryEntry, ArrayConstructor>,
  },
  cells: {
    required: false,
    type: Array as CompType<
      JsonFormsCellRendererRegistryEntry[],
      ArrayConstructor
    >,
    default: undefined,
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
    type: Array as CompType<JsonFormsUISchemaRegistryEntry, ArrayConstructor>,
    default: () => [],
  },
  validationMode: {
    required: false,
    type: String as CompType<ValidationMode, StringConstructor>,
    default: 'ValidateAndShow',
  },
  ajv: {
    required: true,
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

interface ResolvedJsonFormsProps {
  input: CamundaJsonFormInput;
  renderers: JsonFormsRendererRegistryEntry[];
  cells: JsonFormsCellRendererRegistryEntry[];
  config: Record<string, any>;
  readonly?: boolean;
  uischemas?: JsonFormsUISchemaRegistryEntry[];
  validationMode?: ValidationMode;
  ajv: Ajv;
  locale: string;
  translations: Record<string, any>;
}

const resolvedJsonForms = defineComponent({
  name: 'resolved-json-forms',
  components: {
    JsonForms,
  },
  emits: ['change'],
  props: {
    ...resolvedJsonFormsProps(),
  },
  setup(props: ResolvedJsonFormsProps) {
    return {
      resolved: false,
      error: undefined as any,
      schema: undefined as JsonSchema | undefined,
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
      handler(
        newInput: CamundaJsonFormInput,
        _oldInput: CamundaJsonFormInput
      ): void {
        this.resolveSchema(newInput.schema);
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
    this.resolveSchema(this.input.schema);
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      this.$emit('change', event);
    },
    async resolveSchema(schema?: JsonSchema): Promise<void> {
      this.resolved = false;

      try {
        if (schema) {
          this.schema = (await JsonRefs.resolveRefs(schema)).resolved;
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
