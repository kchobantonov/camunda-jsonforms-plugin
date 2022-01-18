<template>
  <v-app>
    <div v-if="loading" class="loading">Loading...</div>

    <div v-if="error !== null" class="error">
      {{ error.value }}
    </div>

    <resolved-json-forms
      v-if="input != null"
      :input="input"
      :renderers="renderers"
      :cells="cells"
      :config="config"
      :validationMode="validationMode"
      :ajv="ajv"
      :locale="locale"
      :translations="camundaFormContext.translations"
      :readonly="readonly"
      @change="onChange"
    />
  </v-app>
</template>

<script lang="ts">
import ResolvedJsonForms from './components/ResolvedJsonForms.vue';
import { createAjv } from '@jsonforms/vue2-vuetify';
import { allRenderers } from './renderers';
import { CamundaJsonFormInput } from '@/core/types';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import { VApp } from 'vuetify/lib';
import forOwn from 'lodash/forOwn';
import { PropType } from 'vue';
import {
  CamundaFormConfig,
  CamundaFormContext,
  ProcessDefinition,
  Task,
} from '@/core/types';
import { JsonSchema7 } from '@jsonforms/core';

const ajv = createAjv({ useDefaults: true });

export default {
  name: 'CamundaForm',
  components: {
    ResolvedJsonForms,
    VApp,
  },
  props: {
    camundaFormConfig: {
      type: Object as PropType<CamundaFormConfig>,
      required: true,
    },
  },
  async created() {
    // fetch the data when the view is created and the data is
    // already being observed
    this.fetchData();
  },
  provide() {
    return {
      camundaFormConfig: this.camundaFormConfig,
      camundaFormContext: this.camundaFormContext,
    };
  },
  data() {
    return {
      loading: false,
      error: null as string | null,
      input: null as CamundaJsonFormInput | null,
      camundaFormContext: {} as CamundaFormContext,

      readonly: false,
      validationMode: 'ValidateAndShow',
      config: {
        restrict: true,
        trim: false,
        showUnfocusedDescription: false,
        hideRequiredAsterisk: true,
      },
      renderers: allRenderers,
      cells: allRenderers,
      ajv,
      locale: this.camundaFormConfig.locale ?? 'en',
    };
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      if (this.camundaFormContext.debug) {
        console.log('onChange: ' + JSON.stringify(event));
      }
    },
    getParameterByName(name: string, url: string | undefined): string | null {
      if (!url) return null;

      let parts = url.split('?');
      if (parts.length > 1) {
        const params = new URLSearchParams('?' + parts[1]);

        return params.get(name);
      }

      return null;
    },
    async getTask(taskId: string): Promise<Task> {
      let task = await (
        await fetch(`${this.camundaFormConfig.camundaUrl}/task/${taskId}`)
      ).json();

      return task;
    },
    async getProcessDefinition(
      processDefinitionId: string
    ): Promise<ProcessDefinition> {
      let processDefinition = await (
        await fetch(
          `${this.camundaFormConfig.camundaUrl}/process-definition/${processDefinitionId}`
        )
      ).json();

      return processDefinition;
    },
    async fetchData(): Promise<void> {
      this.error = null;
      this.input = null;

      this.loading = true;

      try {
        let task:
          | {
              formKey?: string;
              processDefinitionId?: string;
            }
          | undefined = undefined;

        if (this.camundaFormConfig.taskId) {
          task = await this.getTask(this.camundaFormConfig.taskId);

          if (!task.processDefinitionId) {
            this.error = 'Unable to retrieve proceess definition id from task';
            this.loading = false;
            return;
          }

          this.camundaFormContext.task = task;
        }

        let formNameDeployment = this.getParameterByName(
          'deployment',
          task !== undefined ? task.formKey : this.camundaFormConfig.formUrl
        );

        let debug =
          this.getParameterByName(
            'debug',
            task !== undefined ? task.formKey : this.camundaFormConfig.formUrl
          ) === 'true';

        let processDefinition = await this.getProcessDefinition(
          task !== undefined
            ? task.processDefinitionId!
            : this.camundaFormConfig.processDefinitionId
        );

        this.camundaFormContext.processDefinition = processDefinition;
        this.camundaFormContext.debug = debug;

        let resources = await (
          await fetch(
            `${this.camundaFormConfig.camundaUrl}/deployment/${processDefinition.deploymentId}/resources`
          )
        ).json();

        let formSchemaResource = resources.find(
          (i) => i.name === formNameDeployment + '.schema.json'
        );
        let formUiResource = resources.find(
          (i) => i.name === formNameDeployment + '.uischema.json'
        );
        let i18nResource = resources.find(
          (i) => i.name === formNameDeployment + '.i18n.json'
        );

        if (formSchemaResource == null || formUiResource == null) {
          this.error = 'Unable to find schema or uischema resources';
        } else {
          let schema = await (
            await fetch(
              `${this.camundaFormConfig.camundaUrl}/deployment/${processDefinition.deploymentId}/resources/${formSchemaResource.id}/data`
            )
          ).json();

          let uischema = await (
            await fetch(
              `${this.camundaFormConfig.camundaUrl}/deployment/${processDefinition.deploymentId}/resources/${formUiResource.id}/data`
            )
          ).json();

          if (i18nResource) {
            let i18n = await (
              await fetch(
                `${this.camundaFormConfig.camundaUrl}/deployment/${processDefinition.deploymentId}/resources/${i18nResource.id}/data`
              )
            ).json();

            this.camundaFormContext.translations = i18n;
          }

          let data: any = {};

          if (this.camundaFormConfig.taskId) {
            let variableNames: string[] = [];

            if (schema && schema.properties) {
              forOwn(schema.properties, function (value: any, key: string) {
                if (
                  (schema.properties![key] as JsonSchema7).writeOnly !== true
                ) {
                  variableNames.push(key);
                }
              });
            }

            let variableObject = await (
              await fetch(
                `${this.camundaFormConfig.camundaUrl}/task/${
                  this.camundaFormConfig.taskId
                }/form-variables?deserializeValues=false&variableNames=${variableNames.join(
                  ','
                )}`
              )
            ).json();

            forOwn(variableObject, function (value: any, key: string) {
              if (value.type === 'Json') {
                value.value = JSON.parse(value.value);
              }
              data[key] = value.value;
            });
          }

          this.input = {
            schema: schema,
            uischema: uischema,
            data: data,
          };
        }

        this.loading = false;
      } catch (e) {
        this.error = `${e}`;
        this.loading = false;
      }
    },
  },
};
</script>

<style scoped></style>
