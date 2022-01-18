import Vue from 'vue';
import vuetify from './plugins/vuetify';
import VueCompositionAPI from '@vue/composition-api';
import CamundaForm from './CamundaForm.vue';
import { CamundaFormConfig } from '@/core/types';

Vue.use(VueCompositionAPI);
Vue.config.productionTip = false;

class JsonFormsUtil {
  createForm(
    divId: string,
    camundaFormConfig: CamundaFormConfig
  ): Promise<Vue> {
    const result = new Vue({
      vuetify,
      data() {
        return {
          camundaFormConfig,
        };
      },
      render(createElement) {
        return createElement(CamundaForm, {
          props: {
            camundaFormConfig: this.camundaFormConfig,
          },
        });
      },
    });

    result.$mount('#' + divId);

    return Promise.resolve(result);
  }
}

declare global {
  var JsonFormsUtil: JsonFormsUtil;
}

window.JsonFormsUtil = new JsonFormsUtil();
