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
  and,
  JsonFormsRendererRegistryEntry,
  JsonFormsSubStates,
  Layout,
  rankWith,
  UISchemaElement,
  uiTypeIs,
  Tester,
} from '@jsonforms/core';
import {
  DispatchRenderer,
  rendererProps,
  RendererProps,
  useJsonFormsLayout,
} from '@jsonforms/vue2';
import { useTranslator, useVuetifyLayout } from '@jsonforms/vue2-vuetify';
import { type ButtonElement } from '@kchobantonov/common-jsonforms';
import { ErrorObject } from 'ajv';
import isArray from 'lodash/isArray';
import { defineComponent, inject, ref, Ref } from 'vue';
import { VBtn } from 'vuetify/lib';
import { RestClient, SubmitEmitter } from '../core';
import { CamundaFormApi } from '../core/api';
import { AppErrorCode, AppException } from '../core/errors';
import {
  Action,
  CamundaFormContext,
  Emitter,
  isAction,
  isTaskIdConfig,
  ResponseException,
} from '../core/types';

interface CamundaButtonElement extends ButtonElement {
  action: Action;
  errorCode?: string;
  errorMessage?: string;
  escalationCode?: string;
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

const camundaButtonRenderer = defineComponent({
  name: 'camunda-button-renderer',
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
      return (this.layout.uischema as CamundaButtonElement).action;
    },
    isSubmitButton(): boolean {
      return (
        this.action === 'camunda:submit' ||
        this.action === 'camunda:submit-without-data'
      );
    },
    isCompleteButton(): boolean {
      // complete is defined on task only
      return (
        (this.action === 'camunda:complete' ||
          this.action === 'camunda:complete-without-data') &&
        isTaskIdConfig(this.camundaFormContext.config)
      );
    },
    isResolveButton(): boolean {
      return (
        (this.action === 'camunda:resolve' ||
          this.action === 'camunda:resolve-without-data') &&
        isTaskIdConfig(this.camundaFormContext.config)
      );
    },
    isErrorButton(): boolean {
      return (
        this.action === 'camunda:error' &&
        isTaskIdConfig(this.camundaFormContext.config)
      );
    },
    isEscalationButton(): boolean {
      return (
        this.action === 'camunda:escalation' &&
        isTaskIdConfig(this.camundaFormContext.config)
      );
    },
    translatedLabel(): string | undefined {
      if (this.layout.uischema.options?.i18n) {
        return this.t(
          this.layout.uischema.options.i18n,
          (this.layout.uischema as CamundaButtonElement).label
        );
      }
      return this.t(
        (this.layout.uischema as CamundaButtonElement).label,
        (this.layout.uischema as CamundaButtonElement).label
      );
    },
    color(): string | undefined {
      if ((this.layout.uischema as CamundaButtonElement).color) {
        return (this.layout.uischema as CamundaButtonElement).color;
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
          const errorCode = (this.layout.uischema as CamundaButtonElement)
            .errorCode;
          const errorMessage = (this.layout.uischema as CamundaButtonElement)
            .errorMessage;

          payload.errorCode = errorCode;
          payload.errorMessage = errorMessage;
        } else if (this.isEscalationButton) {
          const escalationCode = (this.layout.uischema as CamundaButtonElement)
            .escalationCode;

          payload.escalationCode = escalationCode;
        } else {
          if ((this.layout.uischema as CamundaButtonElement).variables) {
            payload.variables = (
              this.layout.uischema as CamundaButtonElement
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

export default camundaButtonRenderer;

export const isCamundaAction: Tester = (uischema: UISchemaElement): boolean => {
  return isAction((uischema as any).action);
};

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: camundaButtonRenderer,
  tester: rankWith(2, and(uiTypeIs('Button'), isCamundaAction)),
};
</script>
