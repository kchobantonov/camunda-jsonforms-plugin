<template>
  <v-btn
    v-if="button.visible"
    :disabled="!button.enabled"
    :loading="loading"
    v-bind="vuetifyProps('v-btn')"
    @click="click"
  >
    {{ button.label }}
  </v-btn>
</template>

<script lang="ts">
import {
  JsonFormsRendererRegistryEntry,
  JsonFormsSubStates,
  rankWith,
  uiTypeIs,
} from '@jsonforms/core';
import { rendererProps, RendererProps } from '@jsonforms/vue2';
import { useTranslator } from '@jsonforms/vue2-vuetify';
import isFunction from 'lodash/isFunction';
import { defineComponent, inject, ref } from 'vue';
import { VBtn } from 'vuetify/lib';
import { ActionEvent, Actions, FormContext, AsyncFunction } from '../core';
import { ButtonElement, useJsonFormsButton, useVuetifyButton } from '../util';

const buttonRenderer = defineComponent({
  name: 'button-renderer',
  components: {
    VBtn,
  },
  props: {
    ...rendererProps<ButtonElement>(),
  },
  setup(props: RendererProps<ButtonElement>) {
    const t = useTranslator();
    const button = useVuetifyButton(useJsonFormsButton(props));

    const actions = inject<Actions>('actions');
    if (!actions) {
      throw new Error(
        "'actions' couldn't be injected. Are you within JSON Forms?"
      );
    }
    const jsonforms = inject<JsonFormsSubStates>('jsonforms');
    if (!jsonforms) {
      throw new Error(
        "'jsonforms' couldn't be injected. Are you within JSON Forms?"
      );
    }

    const formContext = inject<FormContext>('formContext');

    if (!formContext) {
      throw new Error(
        "'formContext' couldn't be injected. Are you within JsonForms?"
      );
    }
    const loading = ref(false);

    return {
      ...button,
      t,
      actions,
      jsonforms,
      context: formContext,
      loading,
    };
  },
  methods: {
    async click() {
      this.loading = true;

      const source: ActionEvent = this;
      try {
        if (this.button.action) {
          const action = this.actions[this.button.action];
          if (isFunction(action)) {
            await action(source);
          } else {
            console.log('action [' + this.button.action + '] is missing');
          }
        } else if (this.button.script) {
          await new AsyncFunction(this.button.script).call(source);
        }
      } finally {
        this.loading = false;
      }
    },
  },
});

export default buttonRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: buttonRenderer,
  tester: rankWith(1, uiTypeIs('Button')),
};
</script>
