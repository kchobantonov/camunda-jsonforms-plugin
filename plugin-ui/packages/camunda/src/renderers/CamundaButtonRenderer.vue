<template>
  <v-btn
    v-if="
      button.visible &&
      (isSubmitButton ||
        isCompleteButton ||
        isResolveButton ||
        isErrorButton ||
        isEscalationButton)
    "
    :disabled="!button.enabled"
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
  useCamundaButton,
  useJsonFormsCamundaButton,
  type CamundaButtonElement,
} from '@/utils/composition';
import { FormContextKey } from '@chobantonov/jsonforms-vuetify-renderers';
import type { JsonFormsSubStates } from '@jsonforms/core';
import { rendererProps, type RendererProps } from '@jsonforms/vue';
import { useTranslator } from '@jsonforms/vue-vuetify';
import { type ErrorObject } from 'ajv';
import isArray from 'lodash/isArray';
import { defineComponent, inject, ref, type Ref } from 'vue';
import { VBtn } from 'vuetify/components';
import { RestClient, SubmitEmitter } from '../core';
import { CamundaFormApi } from '../core/api';
import { AppErrorCode, AppException } from '../core/errors';
import {
  CamundaAdditionalErrorsKey,
  CamundaFormApiKey,
  CamundaFormEmitterKey,
  isTaskIdConfig,
  ResponseException,
  type CamundaFormContext,
  type Emitter,
} from '../core/types';

// TODO: pass withVariablesInReturn ??

const camundaButtonRenderer = defineComponent({
  name: 'camunda-button-renderer',
  components: {
    VBtn,
  },
  props: {
    ...rendererProps<CamundaButtonElement>(),
  },
  setup(props: RendererProps<CamundaButtonElement>) {
    const t = useTranslator();
    const button = useCamundaButton(useJsonFormsCamundaButton(props));

    const jsonforms = inject<JsonFormsSubStates>('jsonforms');
    if (!jsonforms) {
      throw new Error(
        "'jsonforms' couldn't be injected. Are you within JSON Forms?",
      );
    }

    const additionalErrors = inject<Ref<ErrorObject[]>>(
      CamundaAdditionalErrorsKey,
    );
    if (!additionalErrors) {
      throw new Error(
        "'additionalErrors' couldn't be injected. Are you within CamundaJsonForms?",
      );
    }

    const camundaFormContext = inject<Ref<CamundaFormContext>>(FormContextKey);
    if (!camundaFormContext) {
      throw new Error(
        "'formContext' couldn't be injected. Are you within CamundaJsonForms?",
      );
    }

    const camundaFormApi = inject<CamundaFormApi>(CamundaFormApiKey);
    if (!camundaFormApi) {
      throw new Error(
        "'camundaFormApi' couldn't be injected. Are you within CamundaJsonForms?",
      );
    }

    const camundaFormEmitter = inject<Emitter>(CamundaFormEmitterKey);
    if (!camundaFormEmitter) {
      throw new Error(
        "'camundaFormEmitter' couldn't be injected. Are you within CamundaJsonForms?",
      );
    }

    const loading = ref(false);

    return {
      ...button,
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
    isSubmitButton(): boolean {
      return (
        this.button.action === 'camunda:submit' ||
        this.button.action === 'camunda:submit-without-data'
      );
    },
    isCompleteButton(): boolean {
      // complete is defined on task only
      return (
        (this.button.action === 'camunda:complete' ||
          this.button.action === 'camunda:complete-without-data') &&
        isTaskIdConfig(this.camundaFormContext.camundaFormConfig)
      );
    },
    isResolveButton(): boolean {
      return (
        (this.button.action === 'camunda:resolve' ||
          this.button.action === 'camunda:resolve-without-data') &&
        isTaskIdConfig(this.camundaFormContext.camundaFormConfig)
      );
    },
    isErrorButton(): boolean {
      return (
        this.button.action === 'camunda:error' &&
        isTaskIdConfig(this.camundaFormContext.camundaFormConfig)
      );
    },
    isEscalationButton(): boolean {
      return (
        this.button.action === 'camunda:escalation' &&
        isTaskIdConfig(this.camundaFormContext.camundaFormConfig)
      );
    },
    translatedLabel(): string | undefined {
      if (this.button.uischema.options?.i18n) {
        return this.t(
          this.button.uischema.options.i18n,
          this.button.uischema.label,
        );
      }
      return this.t(this.button.uischema.label, this.button.uischema.label);
    },
    color(): string | undefined {
      return this.button.uischema.color;
    },
  },
  methods: {
    async click() {
      this.loading = true;
      const readonly = this.camundaFormContext.readonly;

      try {
        this.camundaFormContext.readonly = true;

        const payload: Record<string, any> = {};

        if (this.isErrorButton) {
          payload.errorCode = this.button.errorCode;
          payload.errorMessage = this.button.errorMessage;
        } else if (this.isEscalationButton) {
          payload.escalationCode = this.button.escalationCode;
        } else {
          payload.variables = this.button.variables;
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
          this.button.action,
          payload,
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
        this.camundaFormContext.readonly = readonly;

        this.loading = false;
      }
    },
  },
});

export default camundaButtonRenderer;
</script>
