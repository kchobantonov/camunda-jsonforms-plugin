<template>
  <vuetify-json-forms
    :schema="JSON.stringify(schema)"
    :uischema="JSON.stringify(uischema)"
    :data="JSON.stringify(data)"
    :config="JSON.stringify(config)"
    :default-preset="JSON.stringify(preset)"
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
  </vuetify-json-forms>
</template>

<script lang="ts">
import wrap from '@vue/web-component-wrapper';
import Vue, { defineComponent } from 'vue';

import data from './example/data.json';
import schema from './example/schema.json';
import uischema from './example/uischema.json';
import config from './example/config.json';
import preset from './example/preset.json';

import VuetifyJsonForms from './web-components/VuetifyJsonForms.vue';

const VuetifyJsonFormsElement = wrap(Vue, VuetifyJsonForms);

window.customElements.define(
  'vuetify-json-forms',
  VuetifyJsonFormsElement as any
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
      data,
      schema,
      uischema,
      preset,
      config,
    };
  },
});
</script>
