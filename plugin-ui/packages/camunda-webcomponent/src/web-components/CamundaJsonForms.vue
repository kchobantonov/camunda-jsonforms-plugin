<template>
  <camunda-json-forms
    :url="url"
    :processDefinitionId="processDefinitionId"
    :processDefinitionKey="processDefinitionKey"
    :taskId="taskId"
    :config="config"
    :readonly="readonly"
    :validationMode="validationMode"
    :locale="locale"
    :customStyle="customStyle"
    :defaultPreset="defaultPreset"
    @change="onChange"
    @load-request="onLoadRequest"
    @load-response="onLoadResponse"
    @load-error="onLoadError"
    @submit-request="onSubmitRequest"
    @submit-response="onSubmitResponse"
    @submit-error="onSubmitError"
  ></camunda-json-forms>
</template>

<script lang="ts">
const CamundaJsonForms = () => import('../components/CamundaJsonForms.vue');
import { ValidationMode } from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { defineComponent, PropType } from 'vue';

const camundaFormWc = defineComponent({
  components: {
    CamundaJsonForms,
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
      type: String,
      default: 'false',
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
    customStyle: {
      required: false,
      type: String,
      default: '.v-application--wrap { min-height: 0px; }',
    },
    defaultPreset: {
      required: false,
      type: [String],
      validator: function (value) {
        try {
          const config = typeof value == 'string' ? JSON.parse(value) : value;

          return config !== undefined && config !== null;
        } catch (e) {
          return false;
        }
      },
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
