import VueCompositionAPI from '@vue/composition-api';
import wrap from '@vue/web-component-wrapper';
import Vue from 'vue';
import RuntimeTemplateCompiler from 'vue-runtime-template-compiler';
import CamundaJsonForms from './CamundaJsonForms.vue';
import { CamundaFormConfig } from './core/types';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';

Vue.use(VueCompositionAPI);
Vue.use(RuntimeTemplateCompiler);

Vue.config.productionTip = false;

const CamundaJsonFormsElement = wrap(Vue, CamundaJsonForms);

window.customElements.define(
  'camunda-json-forms',
  CamundaJsonFormsElement as any
);

function htmlToElement(html: string): Element {
  const template = document.createElement('template');
  template.innerHTML = html.trim(); // Never return a text node of whitespace as the result;
  return (template.content.cloneNode(true) as Element);
}

interface CamundaFormCallback {
  onFormChange: (event: JsonFormsChangeEvent) => void;
  onFormLoadError: (error: any) => void;
  onFormSubmitSuccessResponse: (response: Response) => void;
  onFormSubmitErrorResponse: (response: Response) => void;
  onFormSubmitError: (error: any) => void;
  onFormSubmitHeadersBuilt: (headers: Record<string, string>) => void;
}

class CamundaJsonFormsUtil {
  createCamundaJsonFormsElement(
    config: CamundaFormConfig & CamundaFormCallback,
    template?: string
  ): Node {
    const element = htmlToElement(
      template ??
        `<camunda-json-forms>
            <link slot="link" rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900">
            <link slot="link" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css" integrity="sha512-Zw6ER2h5+Zjtrej6afEKgS8G5kehmDAHYp9M2xf38MPmpUWX39VrYmdGtCrDQbdLQrTnBVT8/gcNhgS4XPgvEg==" crossorigin="anonymous" referrerpolicy="no-referrer" />
            <link slot="link" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/vuetify/2.6.3/vuetify.min.css" integrity="sha512-yqxpsXY362HEPwSAOWC2FOd8ZCCuJVrTgZSd/0hPmuGUqG19+J2ULPQnc7p795j5mNYZMNHuq5CHPPHnOqapdw==" crossorigin="anonymous" referrerpolicy="no-referrer" />

            <!-- create vuetify theme style inside the webcomponent -->
            <style slot="style" type="text/css" id="vuetify-theme-stylesheet"></style>
            <style slot="style" type="text/css">${config.style ?? ''}</style>
        </camunda-json-forms>`
    );

    const form = element.querySelector("camunda-json-forms") as Element;

    // attributes
    if (config.camundaUrl) {
      form.setAttribute('camunda-url', config.camundaUrl);
    }
    if (config.processDefinitionId) {
      form.setAttribute('process-definition-id', config.processDefinitionId);
    }
    if (config.formUrl) {
      form.setAttribute('form-url', config.formUrl);
    }
    if (config.taskId) {
      form.setAttribute('task-id', config.taskId);
    }
    if (config.locale) {
      form.setAttribute('locale', config.locale);
    }

    //event listeners
    if (config.onFormChange) {
      form.addEventListener('change', (event: Event) =>
        config.onFormChange(
          ((event as CustomEvent).detail as JsonFormsChangeEvent[])[0]
        )
      );
    }
    if (config.onFormLoadError) {
      form.addEventListener('load-error', (event: Event) =>
        config.onFormLoadError(((event as CustomEvent).detail as any[])[0])
      );
    }
    if (config.onFormSubmitHeadersBuilt) {
      form.addEventListener('submit-headers-built', (event: Event) =>
        config.onFormSubmitHeadersBuilt(
          ((event as CustomEvent).detail as Record<string, string>[])[0]
        )
      );
    }
    if (config.onFormSubmitSuccessResponse) {
      form.addEventListener('submit-success-response', (event: Event) =>
        config.onFormSubmitSuccessResponse(
          ((event as CustomEvent).detail as Response[])[0]
        )
      );
    }

    if (config.onFormSubmitErrorResponse) {
      form.addEventListener('submit-error-response', (event: Event) =>
        config.onFormSubmitErrorResponse(
          ((event as CustomEvent).detail as Response[])[0]
        )
      );
    }
    if (config.onFormSubmitError) {
      form.addEventListener('submit-error', (event: Event) =>
        config.onFormSubmitError(((event as CustomEvent).detail as any[])[0])
      );
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
