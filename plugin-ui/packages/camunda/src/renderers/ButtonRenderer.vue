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
    :disabled="!layout.enabled"
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
  Layout,
  rankWith,
  UISchemaElement,
  uiTypeIs,
} from '@jsonforms/core';
import {
  DispatchRenderer,
  rendererProps,
  RendererProps,
  useJsonFormsLayout,
} from '@jsonforms/vue2';
import { useTranslator, useVuetifyLayout } from '@jsonforms/vue2-vuetify';
import {
  Emitter,
  ResponseException,
  RestClient,
  SubmitEmitter,
} from '@kchobantonov/common-jsonforms';
import { ErrorObject } from 'ajv';
import isArray from 'lodash/isArray';
import { defineComponent, inject, ref, Ref } from 'vue';
import { VBtn } from 'vuetify/lib';
import { CamundaFormApi } from '../core/api';
import { AppErrorCode, AppException } from '../core/errors';
import { Action, CamundaFormContext, isTaskIdConfig } from '../core/types';

interface ButtonElement extends UISchemaElement {
  type: 'Button';
  /**
   * The text of button.
   */
  text: string;

  action: Action;
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

    const additionalErrors = inject<Ref<ErrorObject[]>>('additionalErrors');
    if (!additionalErrors) {
      throw new Error(
        "'additionalErrors' couldn't be injected. Are you within CamundaJsonForms?"
      );
    }

    const camundaFormContext = inject<CamundaFormContext>('formContext');
    if (!camundaFormContext) {
      throw new Error(
        "'formContext' couldn't be injected. Are you within CamundaJsonForms?"
      );
    }

    const camundaFormApi = inject<CamundaFormApi>('camundaFormApi');
    if (!camundaFormApi) {
      throw new Error(
        "'camundaFormApi' couldn't be injected. Are you within CamundaJsonForms?"
      );
    }

    const camundaFormEmitter = inject<Emitter>('camundaFormEmitter');
    if (!camundaFormEmitter) {
      throw new Error(
        "'camundaFormEmitter' couldn't be injected. Are you within CamundaJsonForms?"
      );
    }

    const loading = ref(false);

    return {
      ...layout,
      t,
      jsonforms,
      camundaFormContext,
      camundaFormApi,
      camundaFormEmitter,
      loading,
      additionalErrors,
    };
  },
  computed: {
    action(): Action {
      return (this.layout.uischema as ButtonElement).action ?? 'submit';
    },
    isSubmitButton(): boolean {
      return this.action === 'submit' || this.action === 'submit-without-data';
    },
    isCompleteButton(): boolean {
      // complete is defined on task only
      return (
        (this.action === 'complete' ||
          this.action === 'complete-without-data') &&
        isTaskIdConfig(this.camundaFormContext.config)
      );
    },
    isResolveButton(): boolean {
      return (
        (this.action === 'resolve' || this.action === 'resolve-without-data') &&
        isTaskIdConfig(this.camundaFormContext.config)
      );
    },
    isErrorButton(): boolean {
      return (
        this.action === 'error' &&
        isTaskIdConfig(this.camundaFormContext.config)
      );
    },
    isEscalationButton(): boolean {
      return (
        this.action === 'escalation' &&
        isTaskIdConfig(this.camundaFormContext.config)
      );
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
        const payload: Record<string, any> = {};

        if (this.isErrorButton) {
          const errorCode = (this.layout.uischema as ButtonElement).errorCode;
          const errorMessage = (this.layout.uischema as ButtonElement)
            .errorMessage;

          payload.errorCode = errorCode;
          payload.errorMessage = errorMessage;
        } else if (this.isEscalationButton) {
          const escalationCode = (this.layout.uischema as ButtonElement)
            .escalationCode;

          payload.escalationCode = escalationCode;
        } else {
          if ((this.layout.uischema as ButtonElement).variables) {
            payload.variables = (
              this.layout.uischema as ButtonElement
            ).variables;
          }
        }
        const restClient = new RestClient([
          new SubmitEmitter(this.camundaFormEmitter),
        ]);

        const data = this.jsonforms.core!.data;
        const schema = this.jsonforms.core!.schema;

        await this.camundaFormApi.submitForm(
          restClient,
          schema,
          data,
          this.camundaFormContext,
          this.action,
          payload
        );
      } catch (e) {
        if (
          e instanceof AppException &&
          e.code === AppErrorCode.SUBMIT_FORM &&
          e.cause instanceof ResponseException &&
          e.cause.code === 400 &&
          e.data &&
          isArray(e.data.validationErrors)
        ) {
          const additionalErrors: ErrorObject[] = e.data.validationErrors;
          this.additionalErrors = additionalErrors;
        }
        this.camundaFormEmitter('submit-error', e);
      } finally {
        this.loading = false;
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
