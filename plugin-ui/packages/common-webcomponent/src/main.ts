import wrap from '@vue/web-component-wrapper';
import Vue from 'vue';
import { VuetifyJsonFormsWc } from './components';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { FormCallback } from '@kchobantonov/common-jsonforms';
import { VuetifyFormConfig } from './core';
import isFunction from 'lodash/isFunction';
import 'vuetify/dist/vuetify.min.css';

Vue.config.productionTip = false;

const VuetifyJsonFormsElement = wrap(Vue, VuetifyJsonFormsWc);

if (window.customElements.get('vuetify-json-forms') == undefined) {
  window.customElements.define(
    'vuetify-json-forms',
    VuetifyJsonFormsElement as any
  );
}

function htmlToElement(html: string): Element {
  const template = document.createElement('template');
  template.innerHTML = html.trim(); // Never return a text node of whitespace as the result;
  return template.content.cloneNode(true) as Element;
}

class VuetifyJsonFormsUtil {
  createVuetifyJsonFormsElement(
    config: VuetifyFormConfig & FormCallback,
    template?: string
  ): Node {
    const element = htmlToElement(
      template ??
        `<vuetify-json-forms>
            <link slot="link" rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900"/>
            <link slot="link" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css" integrity="sha512-Zw6ER2h5+Zjtrej6afEKgS8G5kehmDAHYp9M2xf38MPmpUWX39VrYmdGtCrDQbdLQrTnBVT8/gcNhgS4XPgvEg==" crossorigin="anonymous" referrerpolicy="no-referrer" />
            <link slot="link" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/vuetify/2.6.3/vuetify.min.css" integrity="sha512-yqxpsXY362HEPwSAOWC2FOd8ZCCuJVrTgZSd/0hPmuGUqG19+J2ULPQnc7p795j5mNYZMNHuq5CHPPHnOqapdw==" crossorigin="anonymous" referrerpolicy="no-referrer" />

            <!-- create vuetify theme style inside the webcomponent -->
            <style slot="style" type="text/css" id="vuetify-theme-stylesheet"></style>
            <style slot="style" type="text/css">${config.style ?? ''}</style>
        </vuetify-json-forms>`
    );

    const form = element.querySelector('vuetify-json-forms') as Element;

    // attributes
    if (config.schema !== undefined) {
      form.setAttribute('schema', JSON.stringify(config.schema));
    }
    if (config.uischema !== undefined) {
      form.setAttribute('uischema', JSON.stringify(config.uischema));
    }
    if (config.data !== undefined) {
      form.setAttribute('data', JSON.stringify(config.data));
    }
    if (config.config !== undefined) {
      form.setAttribute('config', JSON.stringify(config.config));
    }
    if (config.readonly !== undefined) {
      form.setAttribute(
        'readonly',
        config.readonly || config.readonly == 'true' ? 'true' : 'false'
      );
    }
    if (config.validationMode !== undefined) {
      form.setAttribute('validationMode', config.validationMode);
    }
    if (config.locale !== undefined) {
      form.setAttribute('locale', config.locale);
    }
    if (config.translations !== undefined) {
      form.setAttribute('translations', JSON.stringify(config.translations));
    }
    if (config.defaultPreset !== undefined) {
      form.setAttribute('defaultPreset', JSON.stringify(config.defaultPreset));
    }

    //event listeners
    if (isFunction(config.onChange)) {
      form.addEventListener('change', (event: Event) => {
        const detail = (event as CustomEvent).detail;
        config.onChange(detail[0] as JsonFormsChangeEvent);
      });
    }

    return element;
  }
}

declare global {
  interface Window {
    VuetifyJsonFormsUtil: VuetifyJsonFormsUtil;
  }
}
window.VuetifyJsonFormsUtil = new VuetifyJsonFormsUtil();
