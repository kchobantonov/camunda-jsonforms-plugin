<template>
  <v-app>
    <slot name="link"></slot>
    <slot name="style"></slot>

    <camunda-json-forms
      :url="url"
      :processDefinitionId="processDefinitionId"
      :processDefinitionKey="processDefinitionKey"
      :taskId="taskId"
      :locale="locale"
      :defaultPreset="defaultPreset"
      @change="onChange"
      @load-request="onLoadRequest"
      @load-response="onLoadResponse"
      @load-error="onLoadError"
      @submit-request="onSubmitRequest"
      @submit-response="onSubmitResponse"
      @submit-error="onSubmitError"
    />
  </v-app>
</template>

<script lang="ts">
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { defineComponent } from '@vue/composition-api';
import { VApp } from 'vuetify/lib';
import { CamundaJsonForms } from '@kchobantonov/camunda-jsonforms';
import { VuetifyPreset } from 'vuetify/types/services/presets';
import vuetify, { preset as defaultPreset } from '../plugins/vuetify';
import { CompType } from '@jsonforms/vue2-vuetify/lib/vue';

const camundaFormWc = defineComponent({
  name: 'camunda-json-forms-wc',
  vuetify,
  components: {
    CamundaJsonForms,
    VApp,
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
      default: () => defaultPreset,
    },
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      this.$emit('change', event);
    },
    onLoadRequest(input: RequestInfo, init?: RequestInit): void {
      this.$emit('load-request', input, init);
    },
    onLoadResponse(response: Response): void {
      this.$emit('load-response', response);
    },
    onLoadError(error: any): void {
      this.$emit('load-error', error);
    },
    onSubmitRequest(input: RequestInfo, init?: RequestInit): void {
      this.$emit('submit-request', input, init);
    },
    onSubmitResponse(response: Response): void {
      this.$emit('submit-response', response);
    },
    onSubmitError(error: any): void {
      this.$emit('submit-error', error);
    },
  },
});

export default camundaFormWc;
</script>

<style></style>
