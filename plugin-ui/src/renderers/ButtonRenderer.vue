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
    v-bind="vuetifyProps('v-btn')"
    @click="click"
  >
    {{ translatedLabel }}
  </v-btn>
</template>

<script lang="ts">
import {
  JsonFormsRendererRegistryEntry,
  JsonFormsSubStates,
  JsonSchema,
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
import { CamundaFormConfig } from '../core/types';
import forOwn from 'lodash/forOwn';

const getCamundaType = (schema: JsonSchema): string => {
  switch (schema.type) {
    case 'string':
      return (schema as any).format === 'file' ? 'File' : 'String';
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

const attachCamundaVariable = (
  variables: Record<string, any>,
  variableName: string,
  variableSchema: JsonSchema,
  variableData: any
): void => {
  if ((variableSchema as any).readOnly !== true) {
    const type = getCamundaType(variableSchema);

    let value = variableData;
    let valueInfo: ValueInfo = {};

    if (type === 'Json') {
      value = value ? JSON.stringify(value) : value;
    } else if (type === 'File') {
      if (!value) {
        // invalid value
        return;
      }

      const dataUrl = value as string;

      const base64Index = dataUrl.indexOf(';base64,');

      const header = dataUrl.substring(0, base64Index); // data header without the base64
      value = dataUrl.substring(base64Index + ';base64,'.length); // get only the base64 value

      const fileNameIndex = header.indexOf(';filename=');

      const fileName = decodeURIComponent(
        header.substring(fileNameIndex + ';filename='.length)
      );

      const mimeType = header.substring(
        'data:'.length,
        header.indexOf(';filename=')
      );

      (valueInfo as FileValueInfo).filename = fileName;
      (valueInfo as FileValueInfo).mimeType = mimeType;
    }

    variables[variableName] = {
      value: value,
      type: type,
      valueInfo: valueInfo,
    };
  }
};

interface ValueInfo {
  /**
   * A string representation of the object's type name.
   */
  objectTypeName?: string;
  /**
   * The serialization format used to store the variable.
   */
  serializationDataFormat?: string;

  /**
   * Mark the variables as transient
   */
  transient?: boolean;
}

interface FileValueInfo extends ValueInfo {
  /**
   * The name of the file. This is not the variable name but the name that will be used when downloading the file again.
   */
  filename: string;

  /**
   * The mime type of the file that is being uploaded.
   */
  mimeType?: string;

  /**
   *  Identifies the file's encoding as specified on value creation.
   */
  encoding?: string;
}

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

// TODO: pass withVariablesInReturn ??

const buttonRenderer = defineComponent({
  name: 'button-renderer',
  components: {
    DispatchRenderer,
    VBtn,
  },
  emits: [
    'submit-headers-built',
    'submit-success-response',
    'submit-error-response',
    'submit-error',
  ],
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

    const loading = false;

    return {
      ...layout,
      t,
      jsonforms,
      camundaFormConfig,
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
      return (
        action === 'complete' && this.camundaFormConfig.taskId !== undefined
      );
    },
    isResolveButton(): boolean {
      const action = (this.layout.uischema as ButtonElement).action;
      // complete is defined on task only
      return (
        action === 'resolve' && this.camundaFormConfig.taskId !== undefined
      );
    },
    isErrorButton(): boolean {
      const action = (this.layout.uischema as ButtonElement).action;
      // error is defined on task only
      return action === 'error' && this.camundaFormConfig.taskId !== undefined;
    },
    isEscalationButton(): boolean {
      const action = (this.layout.uischema as ButtonElement).action;
      // escalation is defined on task only
      return (
        action === 'escalation' && this.camundaFormConfig.taskId !== undefined
      );
    },
    hasErrors(): boolean {
      if (
        this.isSubmitButton ||
        this.isCompleteButton ||
        this.isResolveButton
      ) {
        const numberOfErrors = this.jsonforms!.core?.errors?.length;
        return numberOfErrors == undefined || numberOfErrors > 0;
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
    async click() {
      this.loading = true;

      try {
        if (this.isSubmitButton) {
          await this.send(true, 'submit-form');
        } else if (this.isResolveButton) {
          await this.send(true, 'resolve');
        } else if (this.isCompleteButton) {
          await this.send(true, 'complete');
        } else if (this.isErrorButton) {
          const errorCode = (this.layout.uischema as ButtonElement).errorCode;
          const errorMessage = (this.layout.uischema as ButtonElement)
            .errorMessage;

          await this.send(false, 'bpmnError', {
            errorCode: errorCode,
            errorMessage: errorMessage,
          });
        } else if (this.isEscalationButton) {
          const escalationCode = (this.layout.uischema as ButtonElement)
            .escalationCode;

          await this.send(false, 'bpmnEscalation', {
            escalationCode: escalationCode,
          });
        }
      } finally {
        this.loading = false;
      }
    },
    async send(
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
        let data = this.jsonforms!.core.data;
        let schema = this.jsonforms!.core.schema;
        if (schema && schema.properties) {
          forOwn(schema.properties, function (value: any, key: string) {
            if (Object.prototype.hasOwnProperty.call(data, key)) {
              attachCamundaVariable(
                payload!.variables,
                key,
                schema.properties![key],
                data[key]
              );
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

      const headers = {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      };

      // outside function can provide additional headers during the submit like xsrf
      this.$root.$emit('submit-headers-built', headers);

      try {
        const response = await fetch(url, {
          body: JSON.stringify(payload),
          headers,
          method: 'post',
        });
        if (response.status == 204 || response.status == 200) {
          this.$root.$emit('submit-success-response', response);
        } else {
          this.$root.$emit('submit-error-response', response);
        }
      } catch (error) {
        this.$root.$emit('submit-error', error);
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
