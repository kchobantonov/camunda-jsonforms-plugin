<template>
  <div>
    <camunda-json-forms
      url="/camunda"
      process-definition-key="embeddedFormsQuickstart"
      locale="en"
      readonly="false"
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
      <!-- emulate the css since the VuetifyJsonForms are not build as actual web component that includes css during npm run serve -->
      <custom-style slot="style" type="text/css">
        @import
        url('https://fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900');
        @import
        url('https://cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css');
        @import
        url("https://cdnjs.cloudflare.com/ajax/libs/vuetify/2.6.3/vuetify.min.css");
        .v-application--wrap { min-height: 0px; }
      </custom-style>
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

import CamundaJsonForms from './web-components/CamundaJsonForms.vue';

const CamundaJsonFormsElement = wrap(Vue, CamundaJsonForms);

window.customElements.define(
  'camunda-json-forms',
  CamundaJsonFormsElement as any
);

const CustomStyle = defineComponent({
  name: 'custom-style',
  render(createElement) {
    return createElement('style', this.$slots.default);
  },
});

export default defineComponent({
  name: 'App',
  components: {
    CustomStyle,
  },
  data() {
    return {
      preset,
      config,
      camundaError: '',
      completed: false,
    };
  },
  methods: {
    onChange(event: any) {
      console.log('Form state changed:' + JSON.stringify(event));
    },
    onLoadRequest(event: any) {
      console.log('onLoadRequest');
      let [requestInfo, requestInit] = event.detail;
      //requestInit.headers = requestInit.headers || {};
      //requestInit.headers['Authorization'] = ....;
    },
    onLoadResponse(event: any) {
      console.log('onLoadResponse');
      let [response] = event.detail;
    },
    onLoadError(event: any) {
      console.log('onLoadError');
      let [error] = event.detail;

      if (
        error.name === 'AppException' &&
        (error.code === 'RETRIEVE_TASK_DEPLOYED_FORM' ||
          error.code === 'INVALID_TASK_DEPLOYED_FORM_RESPONSE' ||
          error.code === 'RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM' ||
          error.code ===
            'INVALID_PROCESS_DEFINITION_DEPLOYED_START_FORM_RESPONSE')
      ) {
        // ignore loading from deployed forms - most likely JsonFormsFormServicePlugin was not installed - just log the error in the console
        return;
      }
      if (
        error.name === 'ResponseException' &&
        (error.response.request.url.endsWith('/deployed-start-form') ||
          error.response.request.url.endsWith('/deployed-form'))
      ) {
        // ignore loading from deployed forms - most likely JsonFormsFormServicePlugin was not installed - just log the error in the console
        return;
      }

      this.camundaError = error.message;
    },
    onSubmitRequest(event: any) {
      console.log('onSubmitRequest');
      let [requestInfo, requestInit] = event.detail;
      //requestInit.headers = requestInit.headers || {};
      //requestInit.headers['Authorization'] = ....;
    },
    onSubmitResponse(event: any) {
      console.log('onSubmitResponse');
      let [response] = event.detail;
      if (response.status >= 200 && response.status < 300) {
        this.completed = true;
      }
    },
    onSubmitError(event: any) {
      console.log('onSubmitError');
      let [error] = event.detail;
      if (error.name === 'AppException' && error.response) {
        let response = error.response;
        if (response.status == 401) {
          this.camundaError = `You are not authenticated`;
          return;
        } else if (response.status == 403) {
          this.camundaError = `You are not authorized`;
          return;
        }
      }

      this.camundaError = error.message;
    },
  },
});
</script>
