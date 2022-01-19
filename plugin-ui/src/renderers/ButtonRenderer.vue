<template>
  <v-btn
    v-if="
      layout.visible &&
      (isSubmitButton ||
        isCompleteButton ||
        isResolveButton ||
        isErrorButton ||
        isEscalationButton)
    "
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

  action: 'submit' | 'complete' | 'resolve' | 'error' | 'escalation'; //TODO: claim, unclaim, delegate etc. ?
  errorCode?: string;
  errorMessage?: string;
  escalationCode?: string;
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
    isSubmitButton(): boolean {
      const action = (this.layout.uischema as ButtonElement).action;
      return !action || action === 'submit';
    },
    isCompleteButton(): boolean {
      const action = (this.layout.uischema as ButtonElement).action;
      // complete is defined on task only
      return action === 'complete' && this.camundaFormConfig.taskId;
    },
    isResolveButton(): boolean {
      const action = (this.layout.uischema as ButtonElement).action;
      // complete is defined on task only
      return action === 'resolve' && this.camundaFormConfig.taskId;
    },
    isErrorButton(): boolean {
      const action = (this.layout.uischema as ButtonElement).action;
      // error is defined on task only
      return action === 'error' && this.camundaFormConfig.taskId;
    },
    isEscalationButton(): boolean {
      const action = (this.layout.uischema as ButtonElement).action;
      // escalation is defined on task only
      return action === 'escalation' && this.camundaFormConfig.taskId;
    },
    hasErrors(): boolean {
      if (
        this.isSubmitButton ||
        this.isCompleteButton ||
        this.isResolveButton
      ) {
        return this.jsonforms.core?.errors?.length > 0;
      } else if (this.isErrorButton) {
        const errorCode = (this.layout.uischema as ButtonElement).errorCode;
        return (
          errorCode !== undefined &&
          errorCode !== null &&
          errorCode.trim().length > 0
        );
      } else if (this.isEscalationButton) {
        const escalationCode = (this.layout.uischema as ButtonElement)
          .escalationCode;
        return (
          escalationCode !== undefined &&
          escalationCode !== null &&
          escalationCode.trim().length > 0
        );
      }
      return false;
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
    async click(event: Event) {
      this.loading = true;

      try {
        if (this.isSubmitButton) {
          await this.send(event, true, 'submit-form');
        } else if (this.isResolveButton) {
          await this.send(event, true, 'resolve');
        } else if (this.isCompleteButton) {
          await this.send(event, true, 'complete');
        } else if (this.isErrorButton) {
          const errorCode = (this.layout.uischema as ButtonElement).errorCode;
          const errorMessage = (this.layout.uischema as ButtonElement)
            .errorMessage;

          await this.send(event, false, 'bpmnError', {
            errorCode: errorCode,
            errorMessage: errorMessage,
          });
        } else if (this.isEscalationButton) {
          const escalationCode = (this.layout.uischema as ButtonElement)
            .escalationCode;

          await this.send(event, false, 'bpmnEscalation', {
            escalationCode: escalationCode,
          });
        }
      } finally {
        this.loading = false;
      }
    },
    async send(
      event: Event,
      includeDataVariables: boolean,
      actionPath: string,
      payload?: Record<string, any>
    ) {
      if (!payload) {
        payload = {};
      }
      if (!payload.variables) {
        payload.variables = {};
      }

      if (includeDataVariables) {
        let data = this.jsonforms.core.data;
        let schema = this.jsonforms.core.schema;
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
      }

      if ((this.layout.uischema as ButtonElement).variables) {
        payload.variables = {
          ...payload.variables,
          ...(this.layout.uischema as ButtonElement).variables,
        };
      }

      const url = this.camundaFormConfig.taskId
        ? `${this.camundaFormConfig.camundaUrl}/task/${this.camundaFormConfig.taskId}/${actionPath}`
        : `${this.camundaFormConfig.camundaUrl}/process-definition/${this.camundaFormConfig.processDefinitionId}/${actionPath}`;

      const headers = this.camundaFormConfig.submitHeaders
        ? {
            ...this.camundaFormConfig.submitHeaders,
            Accept: 'application/json',
            'Content-Type': 'application/json',
          }
        : { Accept: 'application/json', 'Content-Type': 'application/json' };

      try {
        const response = await fetch(url, {
          body: JSON.stringify(payload),
          headers,
          method: 'post',
        });
        if (response.status == 204 || response.status == 200) {
          if (this.camundaFormConfig.onSubmitSuccessResponse) {
            this.camundaFormConfig.onSubmitSuccessResponse(response);
          }
        } else {
          if (this.camundaFormConfig.onSubmitErrorResponse) {
            this.camundaFormConfig.onSubmitErrorResponse(response);
          }
        }
      } catch (error) {
        if (this.camundaFormConfig.onSubmitError) {
          this.camundaFormConfig.onSubmitError(error);
        }
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
