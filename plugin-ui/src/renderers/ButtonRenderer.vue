<template>
  <v-btn
    v-if="layout.visible"
    :disabled="!layout.enabled || hasErrors"
    :color="color"
    :loading="loading"
    @click="click()"
  >
    {{ translatedLabel }}
  </v-btn>
</template>

<script lang="ts">
import {
  JsonFormsRendererRegistryEntry,
  JsonFormsSubStates,
  JsonSchema,
  JsonSchema7,
  Layout,
  rankWith,
  UISchemaElement,
  uiTypeIs,
} from '@jsonforms/core';
import { defineComponent, inject } from '@vue/composition-api';
import {
  DispatchRenderer,
  rendererProps,
  useJsonFormsLayout,
  RendererProps,
} from '@jsonforms/vue2';
import { useVuetifyLayout, useTranslator } from '@jsonforms/vue2-vuetify';
import { VBtn } from 'vuetify/lib';
import { CamundaFormConfig, CamundaFormContext } from '@/core/types';
import forOwn from 'lodash/forOwn';

const getCamundaType = (schema: JsonSchema): string => {
  switch (schema.type) {
    case 'string':
      return 'String';
    case 'integer':
      return 'Integer';
    case 'number':
      return 'Double';
    case 'object':
      return 'Json';
    case 'array':
      return 'Json';
    case 'boolean':
      return 'Boolean';
    case 'null':
      return 'Null';
  }
  return 'Json';
};

interface ButtonElement extends UISchemaElement {
  type: 'Button';
  /**
   * The text of button.
   */
  text: string;

  action: 'submit' | 'back';
  color?: string;
  variables?: Record<
    string,
    {
      type: string;
      value: any;
      valueInfo: Record<string, any>;
    }
  >;
}

const buttonRenderer = defineComponent({
  name: 'button-renderer',
  components: {
    DispatchRenderer,
    VBtn,
  },
  props: {
    ...rendererProps<Layout>(),
  },
  setup(props: RendererProps<Layout>) {
    const t = useTranslator();
    const layout = useVuetifyLayout(useJsonFormsLayout(props));

    const jsonforms = inject<JsonFormsSubStates>('jsonforms');
    if (!jsonforms) {
      throw new Error(
        "'jsonforms' couldn't be injected. Are you within JSON Forms?"
      );
    }

    const camundaFormConfig = inject<CamundaFormConfig>('camundaFormConfig');

    if (!camundaFormConfig) {
      throw new Error(
        "'camundaFormConfig' couldn't be injected. Are you within CamundaForm?"
      );
    }

    const camundaFormContext = inject<CamundaFormContext>('camundaFormContext');

    if (!camundaFormContext) {
      throw new Error(
        "'camundaFormContext' couldn't be injected. Are you within CamundaForm?"
      );
    }

    const loading = false;

    return {
      ...layout,
      t,
      jsonforms,
      camundaFormConfig,
      camundaFormContext,
      loading,
    };
  },
  computed: {
    hasErrors(): boolean {
      return this.jsonforms.core?.errors?.length > 0;
    },
    translatedLabel(): string | undefined {
      if (this.layout.uischema.options?.i18n) {
        return this.t(
          this.layout.uischema.options.i18n,
          (this.layout.uischema as ButtonElement).text
        );
      }
      return this.t(
        (this.layout.uischema as ButtonElement).text,
        (this.layout.uischema as ButtonElement).text
      );
    },
    color(): string | undefined {
      if ((this.layout.uischema as ButtonElement).color) {
        return (this.layout.uischema as ButtonElement).color;
      }
      return undefined;
    },
  },
  methods: {
    getParameterByName(name: string, url: string | undefined): string | null {
      if (!url) return null;

      let parts = url.split('?');
      if (parts.length > 1) {
        const params = new URLSearchParams('?' + parts[1]);

        return params.get(name);
      }

      return null;
    },
    click(event: Event) {
      if (
        this.jsonforms.core &&
        (!this.jsonforms.core.errors || this.jsonforms.core.errors.length == 0)
      ) {
        this.loading = true;

        let data = this.jsonforms.core.data;
        let schema = this.jsonforms.core.schema;

        const payload: any = { variables: {} }; // TODO: for start forms we can defined also businessKey

        if (schema && schema.properties) {
          let transient = this.getParameterByName(
            'transient',
            this.camundaFormContext.task !== undefined
              ? this.camundaFormContext.task.formKey
              : this.camundaFormConfig.formUrl
          );

          forOwn(schema.properties, function (value: any, key: string) {
            if (Object.prototype.hasOwnProperty.call(data, key)) {
              const type = getCamundaType(schema.properties![key]);

              if ((schema.properties![key] as JsonSchema7).readOnly !== true) {
                payload.variables[key] = {
                  value:
                    type === 'Json'
                      ? data[key]
                        ? JSON.stringify(data[key])
                        : null
                      : data[key],
                  type: type,
                  valueInfo: { transient: transient === 'true' },
                };
              }
            }
          });
        }

        if ((this.layout.uischema as ButtonElement).variables) {
          payload.variables = {
            ...payload.variables,
            ...(this.layout.uischema as ButtonElement).variables,
          };
        }

        const url = this.camundaFormConfig.taskId
          ? `${this.camundaFormConfig.camundaUrl}/task/${this.camundaFormConfig.taskId}/submit-form`
          : `${this.camundaFormConfig.camundaUrl}/process-definition/${this.camundaFormConfig.processDefinitionId}/submit-form`;

        const headers = this.camundaFormConfig.submitHeaders
          ? {
              ...this.camundaFormConfig.submitHeaders,
              Accept: 'application/json',
              'Content-Type': 'application/json',
            }
          : { Accept: 'application/json', 'Content-Type': 'application/json' };

        fetch(url, {
          body: JSON.stringify(payload),
          headers,
          method: 'post',
        })
          .then((response) => {
            try {
              if (response.status == 204 || response.status == 200) {
                if (this.camundaFormConfig.onSubmitSuccessResponse) {
                  this.camundaFormConfig.onSubmitSuccessResponse(response);
                }
              } else {
                if (this.camundaFormConfig.onSubmitErrorResponse) {
                  this.camundaFormConfig.onSubmitErrorResponse(response);
                }
              }
            } finally {
              this.loading = false;
            }
          })
          .catch((error) => {
            try {
              if (this.camundaFormConfig.onSubmitError) {
                this.camundaFormConfig.onSubmitError(error);
              }
            } finally {
              this.loading = false;
            }
          });
      }
    },
  },
});

export default buttonRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: buttonRenderer,
  tester: rankWith(1, uiTypeIs('Button')),
};
</script>
