<template>
  <control-wrapper
    v-bind="controlWrapper"
    :styles="styles"
    :isFocused="isFocused"
    :appliedOptions="appliedOptions"
  >
    <v-hover v-slot="{ hover }">
      <v-select
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
        :value="control.data"
        :error="provider.error !== undefined"
        :loading="provider.loading"
        :items="provider.data"
        :item-text="getItemText"
        :item-value="getItemValue"
        attach
        v-bind="vuetifyProps('v-select')"
        @change="onChange"
        @focus="isFocused = true"
        @blur="isFocused = false"
      />
    </v-hover>
  </control-wrapper>
</template>

<script lang="ts">
import {
  ControlElement,
  JsonFormsRendererRegistryEntry,
  rankWith,
  isStringControl,
  and,
  optionIs,
} from '@jsonforms/core';
import { defineComponent, inject } from '@vue/composition-api';
import {
  rendererProps,
  useJsonFormsOneOfEnumControl,
  RendererProps,
} from '@jsonforms/vue2';
import {
  useVuetifyControl,
  ControlWrapper,
  DisabledIconFocus,
} from '@jsonforms/vue2-vuetify';

import { template as templateFn } from '../core/template';
import { VSelect, VHover, VAutocomplete } from 'vuetify/lib';

const controlRenderer = defineComponent({
  name: 'data-provider-select-renderer',
  components: {
    ControlWrapper,
    VSelect,
    VAutocomplete,
    VHover,
  },
  directives: {
    DisabledIconFocus,
  },
  props: {
    ...rendererProps<ControlElement>(),
  },
  setup(props: RendererProps<ControlElement>) {
    const provider = inject<any>('scopeData');

    return {
      ...useVuetifyControl(
        useJsonFormsOneOfEnumControl(props),
        (value) => value || undefined,
        300
      ),
      provider,
    };
  },
  methods: {
    getItemValue(item: any) {
      const compiled = templateFn(
        this.control.uischema.options?.['item-value']
      );

      return compiled(item);
    },
    getItemText(item: any) {
      const compiled = templateFn(this.control.uischema.options?.['item-text']);

      return compiled(item);
    },
  },
});

export default controlRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: controlRenderer,
  tester: rankWith(11, and(isStringControl, optionIs('variant', 'v-select'))),
};
</script>
