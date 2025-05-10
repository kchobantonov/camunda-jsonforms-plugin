import { type App, defineCustomElement } from 'vue';
import CamundaJsonForms from './web-components/CamundaJsonForms.ce.vue';

import LoadScript from 'vue-plugin-load-script';
import buildVuetify from './plugins/vuetify';

import { styles } from './styles';

const CamundaJsonFormsElement = defineCustomElement(CamundaJsonForms, {
  shadowRoot: true, // Ensure shadow DOM is used
  configureApp: (app: App) => {
    // provide dummy usehead to disable injection of theme css with id vuetify-theme-stylesheet
    app.provide('usehead', {
      push(getHead: () => {}) {
        return {
          patch(getHead: () => {}) {},
        };
      },
    });
    app.use(buildVuetify());
    app.use(LoadScript);
  },
  styles,
});

if (!customElements.get('camunda-json-forms')) {
  customElements.define('camunda-json-forms', CamundaJsonFormsElement);
}
