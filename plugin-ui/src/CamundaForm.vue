<template>
  <v-app>
    <div v-if="loading" class="loading">Loading...</div>

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
      return await (
        await fetch(
          `${this.camundaFormConfig.camundaUrl}/task/${taskId}`
        ).catch((e) => {
          throw new Error(`Unable to retrieve task: ${e}`);
        })
      )
        .json()
        .catch((e) => {
          throw new Error(`Invalid JSON response when retrieving task: ${e}`);
        });
    },
    async getProcessDefinition(
      processDefinitionId: string
    ): Promise<ProcessDefinition> {
      return await (
        await fetch(
          `${this.camundaFormConfig.camundaUrl}/process-definition/${processDefinitionId}`
        ).catch((e) => {
          throw new Error(`Unable to retrieve process definition: ${e}`);
        })
      )
        .json()
        .catch((e) => {
          throw new Error(
            `Invalid JSON response when retrieving process definition: ${e}`
          );
        });
    },
    async fetchData(): Promise<void> {
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
            throw new Error(
              'Unable to retrieve proceess definition id from task'
            );
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
          ).catch((e) => {
            throw new Error(
              `Unable to retrieve resources for deployment: ${e}`
            );
          })
        )
          .json()
          .catch((e) => {
            throw new Error(
              `Invalid JSON response when retrieving resources for deployment: ${e}`
            );
          });

        let formSchemaResource = resources.find(
          (i) => i.name === formNameDeployment + '.schema.json'
        );
        let formUiResource = resources.find(
          (i) => i.name === formNameDeployment + '.uischema.json'
        );
        let i18nResource = resources.find(
          (i) => i.name === formNameDeployment + '.i18n.json'
        );

        if (formSchemaResource == null) {
          throw new Error('Unable to find JsonForms schema');
        }

        if (formUiResource == null) {
          throw new Error('Unable to find JsonForms uischema');
        }

        let schema = await (
          await fetch(
            `${this.camundaFormConfig.camundaUrl}/deployment/${processDefinition.deploymentId}/resources/${formSchemaResource.id}/data`
          ).catch((e) => {
            throw new Error(`Unable to retrieve JsonForms schema: ${e}`);
          })
        )
          .json()
          .catch((e) => {
            throw new Error(
              `Invalid JSON response when retrieving JsonForms schema: ${e}`
            );
          });

        let uischema = await (
          await fetch(
            `${this.camundaFormConfig.camundaUrl}/deployment/${processDefinition.deploymentId}/resources/${formUiResource.id}/data`
          ).catch((e) => {
            throw new Error(`Unable to retrieve JsonForms uischema: ${e}`);
          })
        )
          .json()
          .catch((e) => {
            throw new Error(
              `Invalid JSON response when retrieving JsonForms uischema: ${e}`
            );
          });

        if (i18nResource) {
          let i18n = await (
            await fetch(
              `${this.camundaFormConfig.camundaUrl}/deployment/${processDefinition.deploymentId}/resources/${i18nResource.id}/data`
            ).catch((e) => {
              throw new Error(`Unable to retrieve JsonForms i18n: ${e}`);
            })
          )
            .json()
            .catch((e) => {
              `Invalid JSON response when retrieving JsonForms i18n: ${e}`;
            });

          this.camundaFormContext.translations = i18n;
        }

        let data: any = {};

        if (this.camundaFormConfig.taskId) {
          let variableNames: string[] = [];

          if (schema && schema.properties) {
            forOwn(schema.properties, function (value: any, key: string) {
              if ((schema.properties![key] as JsonSchema7).writeOnly !== true) {
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
            ).catch((e) => {
              throw new Error(`Unable to retrieve camunda variables: ${e}`);
            })
          )
            .json()
            .catch((e) => {
              throw new Error(
                `Invalid JSON response when retrieving camunda variables: ${e}`
              );
            });

          this.camundaFormContext.variables = variableObject;

          forOwn(variableObject, function (value: any, key: string) {
            if (value.type === 'Json') {
              value.value = value.value ? JSON.parse(value.value) : value.value;
            }

            if (value.value) {
              // set only when the value is not undefined or null
              data[key] = value.value;
            }
          });
        }

        this.input = {
          schema: schema,
          uischema: uischema,
          data: data,
        };

        this.loading = false;
      } catch (e) {
        if (this.camundaFormConfig.onLoadError) {
          this.camundaFormConfig.onLoadError(e);
        } else {
          // just log the error
          console.log(`Load Error: ${e}`);
        }
        this.loading = false;
      }
    },
  },
};
</script>

<style scoped></style>
