<template>
  <div>
    <camunda-json-forms
      url="/engine-rest"
      process-definition-key="embeddedFormsQuickstart"
      locale="en"
      readonly="false"
      :custom-style="style"
      :config="JSON.stringify(config)"
      :default-preset="JSON.stringify(preset)"
      @load-request="onLoadRequest"
      @load-response="onLoadResponse"
      @load-error="onLoadError"
      @submit-request="onSubmitRequest"
      @submit-response="onSubmitResponse"
      @submit-error="onSubmitError"
      @change="onChange"
    >
    </camunda-json-forms>
    <div v-if="camundaError">
      <h2>Error</h2>
      <p><strong>Error message:</strong> {{ camundaError }}</p>
    </div>
    <div v-if="completed">
      <h2>Form completed</h2>
    </div>
  </div>
</template>

<script lang="ts">
import wrap from '@vue/web-component-wrapper';
import Vue, { defineComponent } from 'vue';

import config from './example/config.json';
import preset from './example/preset.json';

import {
  onChange as externalOnChange,
  onLoadError as externalOnLoadError,
  onLoadRequest as externalOnLoadRequest,
  onLoadResponse as externalOnLoadResponse,
  onSubmitError as externalOnSubmitError,
  onSubmitRequest as externalOnSubmitRequest,
  onSubmitResponse as externalOnSubmitResponse,
} from './example/listeners';

import CamundaJsonForms from './web-components/CamundaJsonForms.vue';

const CamundaJsonFormsElement = wrap(Vue, CamundaJsonForms);

window.customElements.define(
  'camunda-json-forms',
  CamundaJsonFormsElement as any
);

export default defineComponent({
  name: 'App',
  data() {
    // emulate the css since the VuetifyJsonForms are not build as actual web component that includes css during npm run serve -->
    const style = `
        @import url('//fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900');
        @import url('//cdn.jsdelivr.net/npm/@mdi/font@6.x/css/materialdesignicons.min.css');
        @import url('//cdn.jsdelivr.net/npm/vuetify@2.6.12/dist/vuetify.min.css');

        .v-application--wrap { min-height: 0px; }`;
    return {
      style,
      preset,
      config,
      camundaError: '',
      completed: false,
    };
  },
  methods: {
    onChange(customEvent: any): void {
      externalOnChange(customEvent);
    },
    onLoadRequest(customEvent: any): void {
      externalOnLoadRequest(customEvent);
    },
    onLoadResponse(customEvent: any): void {
      externalOnLoadResponse(customEvent);
    },
    onLoadError(customEvent: any): void {
      externalOnLoadError(customEvent);
    },
    onSubmitRequest(customEvent: any): void {
      externalOnSubmitRequest(customEvent);
    },
    onSubmitResponse(customEvent: any): void {
      externalOnSubmitResponse(customEvent);
    },
    onSubmitError(customEvent: any): void {
      externalOnSubmitError(customEvent);
    },
  },
});
</script>
