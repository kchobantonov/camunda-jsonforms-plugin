import wrap from '@vue/web-component-wrapper';
import Vue from 'vue';
import {
  CamundaFormConfig,
  isTaskIdConfig,
  isProcessDefinitionIdConfig,
  isProcessDefinitionKeyConfig,
} from '@kchobantonov/camunda-jsonforms';
import { CamundaJsonFormsWc } from './components';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { FormCallback } from '@kchobantonov/common-jsonforms';
import isFunction from 'lodash/isFunction';
import LoadScript from "vue-plugin-load-script";

import 'vuetify/dist/vuetify.min.css';

Vue.use(LoadScript);

Vue.config.productionTip = false;

const CamundaJsonFormsElement = wrap(Vue, CamundaJsonFormsWc);

window.customElements.define(
  'camunda-json-forms',
  CamundaJsonFormsElement as any
);

function htmlToElement(html: string): Element {
  const template = document.createElement('template');
  template.innerHTML = html.trim(); // Never return a text node of whitespace as the result;
  return template.content.cloneNode(true) as Element;
}

class CamundaJsonFormsUtil {
  createCamundaJsonFormsElement(
    config: CamundaFormConfig & FormCallback,
    template?: string
  ): Node {
    const element = htmlToElement(
      template ??
        `<camunda-json-forms>
            <link slot="link" rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900"/>
            <link slot="link" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css" integrity="sha512-Zw6ER2h5+Zjtrej6afEKgS8G5kehmDAHYp9M2xf38MPmpUWX39VrYmdGtCrDQbdLQrTnBVT8/gcNhgS4XPgvEg==" crossorigin="anonymous" referrerpolicy="no-referrer" />
            <link slot="link" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/vuetify/2.6.3/vuetify.min.css" integrity="sha512-yqxpsXY362HEPwSAOWC2FOd8ZCCuJVrTgZSd/0hPmuGUqG19+J2ULPQnc7p795j5mNYZMNHuq5CHPPHnOqapdw==" crossorigin="anonymous" referrerpolicy="no-referrer" />

            <style slot="style" type="text/css">${config.style ?? ''}</style>
        </camunda-json-forms>`
    );

    const form = element.querySelector('camunda-json-forms') as Element;

    // attributes
    if (config.url) {
      form.setAttribute('url', config.url);
    }
    if (isProcessDefinitionIdConfig(config) && config.processDefinitionId) {
      form.setAttribute('process-definition-id', config.processDefinitionId);
    }
    if (isProcessDefinitionKeyConfig(config) && config.processDefinitionKey) {
      form.setAttribute('process-definition-key', config.processDefinitionKey);
    }
    if (isTaskIdConfig(config) && config.taskId) {
      form.setAttribute('task-id', config.taskId);
    }
    if (config.locale) {
      form.setAttribute('locale', config.locale);
    }
    if (config.config) {
      form.setAttribute('config', JSON.stringify(config.config));
    }

    //event listeners
    if (isFunction(config.onChange)) {
      form.addEventListener('change', (event: Event) => {
        const detail = (event as CustomEvent).detail;
        config.onChange(detail[0] as JsonFormsChangeEvent);
      });
    }

    if (isFunction(config.onLoadRequest)) {
      form.addEventListener('load-request', (event: Event) => {
        const detail = (event as CustomEvent).detail;
        if (detail.length > 1) {
          config.onLoadRequest(
            detail[0] as RequestInfo,
            detail[1] as RequestInit
          );
        } else {
          config.onLoadRequest(detail[0] as RequestInfo);
        }
      });
    }

    if (isFunction(config.onLoadResponse)) {
      form.addEventListener('load-response', (event: Event) => {
        const detail = (event as CustomEvent).detail;
        config.onLoadResponse(detail[0] as Response);
      });
    }

    if (isFunction(config.onLoadError)) {
      form.addEventListener('load-error', (event: Event) => {
        const detail = (event as CustomEvent).detail;
        config.onLoadError(detail[0] as any);
      });
    }

    if (isFunction(config.onSubmitRequest)) {
      form.addEventListener('submit-request', (event: Event) => {
        const detail = (event as CustomEvent).detail;
        if (detail.length > 1) {
          config.onSubmitRequest(
            detail[0] as RequestInfo,
            detail[1] as RequestInit
          );
        } else {
          config.onSubmitRequest(detail[0] as RequestInfo);
        }
      });
    }

    if (isFunction(config.onSubmitResponse)) {
      form.addEventListener('submit-response', (event: Event) => {
        const detail = (event as CustomEvent).detail;
        config.onSubmitResponse(detail[0] as Response);
      });
    }

    if (isFunction(config.onSubmitError)) {
      form.addEventListener('submit-error', (event: Event) => {
        const detail = (event as CustomEvent).detail;
        config.onSubmitError(detail[0] as any);
      });
    }
    return element;
  }
}

declare global {
  interface Window {
    CamundaJsonFormsUtil: CamundaJsonFormsUtil;
  }
}
window.CamundaJsonFormsUtil = new CamundaJsonFormsUtil();
