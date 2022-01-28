<template>
  <v-file-input
    v-disabled-icon-focus
    :id="control.id + '-input'"
    :class="styles.control.input"
    :disabled="!control.enabled"
    :autofocus="appliedOptions.focus"
    :placeholder="appliedOptions.placeholder"
    :label="computedLabel"
    :hint="control.description"
    :persistent-hint="persistentHint()"
    :required="control.required"
    :error-messages="control.errors"
    :clearable="hover"
    :accept="accept"
    @change="selectFile"
    @focus="isFocused = true"
    @blur="isFocused = false"
  ></v-file-input>
</template>

<script lang="ts">
import {
  and,
  ControlElement,
  isStringControl,
  JsonFormsRendererRegistryEntry,
  JsonSchema,
  Layout,
  rankWith,
  schemaMatches,
  uiTypeIs,
} from '@jsonforms/core';
import {
  DispatchRenderer,
  rendererProps,
  RendererProps,
  useJsonFormsControl,
} from '@jsonforms/vue2';
import { DisabledIconFocus, useVuetifyControl } from '@jsonforms/vue2-vuetify';
import { defineComponent } from '@vue/composition-api';
import { VFileInput } from 'vuetify/lib';

const toBase64 = (file: File, schemaFormat: string) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      const dataurl = reader.result as string;
      if (schemaFormat === 'uri') {
        resolve(dataurl);
      } else if (schemaFormat === 'file') {
        //special handling to encode the filename
        const insertIndex = dataurl.indexOf(';base64,');
        resolve(
          dataurl.substring(0, insertIndex) +
            `;filename=${encodeURIComponent(file.name)}` +
            dataurl.substring(insertIndex)
        );
      } else {
        resolve(dataurl.substring(dataurl.indexOf(',') + 1));
      }
    };
    reader.onerror = (error) => reject(error);
  });

const fileRenderer = defineComponent({
  name: 'file-renderer',
  components: {
    DispatchRenderer,
    VFileInput,
  },
  directives: {
    DisabledIconFocus,
  },
  props: {
    ...rendererProps<Layout>(),
  },
  setup(props: RendererProps<ControlElement>) {
    let currentFile: File = undefined;

    return {
      ...useVuetifyControl(useJsonFormsControl(props)),
      currentFile,
    };
  },
  computed: {
    accept() {
      return this.control.schema.contentMediaType;
    },
  },
  methods: {
    async selectFile(value: File) {
      const schema: JsonSchema = this.control.schema;

      this.onChange(value ? await toBase64(value, schema.format) : undefined);
    },
  },
});

export default fileRenderer;

export const isBase64String = and(
  uiTypeIs('Control'),
  isStringControl,
  schemaMatches(
    (schema) =>
      (Object.prototype.hasOwnProperty.call(schema, 'contentEncoding') &&
        (schema as any).contentEncoding == 'base64') ||
      schema.format === 'file'
  )
);

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: fileRenderer,
  tester: rankWith(2, isBase64String),
};
</script>
