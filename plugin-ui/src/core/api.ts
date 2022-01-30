import forOwn from 'lodash/forOwn';

import {
  CamundaFormConfig,
  CamundaFormContext,
  ProcessDefinition,
  Task,
  Resource,
  VariableValue,
} from './types';

import { JsonSchema7, UISchemaElement } from '@jsonforms/core';

function getParameterByName(
  name: string,
  url: string | undefined
): string | null {
  if (!url) return null;

  const parts = url.split('?');
  if (parts.length > 1) {
    const params = new URLSearchParams('?' + parts[1]);

    return params.get(name);
  }

  return null;
}

export class CamundaFormApi {
  private camundaUrl: string;
  private processDefinitionId: string;
  private formUrl: string;
  private taskId?: string;

  constructor(config: CamundaFormConfig) {
    this.camundaUrl = config.camundaUrl;
    this.processDefinitionId = config.processDefinitionId;
    this.formUrl = config.formUrl;
    this.taskId = config.taskId;
  }

  async getCamundaFormContext(): Promise<CamundaFormContext> {
    const result: Partial<CamundaFormContext> = {};

    if (this.taskId) {
      const task = await this.getTask(this.taskId);

      if (!task.processDefinitionId) {
        throw new Error('Unable to retrieve proceess definition id from task');
      }

      result.task = task;
    }

    const formNameDeployment = getParameterByName(
      'deployment',
      result.task !== undefined ? result.task.formKey : this.formUrl
    );

    const processDefinition = await this.getProcessDefinition(
      result.task !== undefined
        ? result.task.processDefinitionId!
        : this.processDefinitionId
    );

    result.processDefinition = processDefinition;

    const resources = await this.getDeploymentResources(
      processDefinition.deploymentId!
    );

    const formSchemaResource = resources.find(
      (i) => i.name === formNameDeployment + '.schema.json'
    );
    const formUiResource = resources.find(
      (i) => i.name === formNameDeployment + '.uischema.json'
    );
    const i18nResource = resources.find(
      (i) => i.name === formNameDeployment + '.i18n.json'
    );

    if (formSchemaResource == null) {
      throw new Error('Unable to find JsonForms schema');
    }

    if (formUiResource == null) {
      throw new Error('Unable to find JsonForms uischema');
    }

    const schema = await this.getDeploymentResourceJsonData(
      processDefinition.deploymentId!,
      formSchemaResource.id,
      'Unable to retrieve JsonForms schema',
      'Invalid JSON response when retrieving JsonForms schema'
    );

    const uischema = await this.getDeploymentResourceJsonData(
      processDefinition.deploymentId!,
      formUiResource.id,
      'Unable to retrieve JsonForms uischema',
      'Invalid JSON response when retrieving JsonForms uischema'
    );

    if (i18nResource) {
      const i18n = await this.getDeploymentResourceJsonData(
        processDefinition.deploymentId!,
        i18nResource.id,
        'Unable to retrieve JsonForms i18n',
        'Invalid JSON response when retrieving JsonForms i18n'
      );

      result.translations = i18n;
    }

    const data: any = {};

    if (this.taskId) {
      const variableNames: string[] = [];

      if (schema && schema.properties) {
        forOwn(schema.properties, function (value: any, key: string) {
          if ((schema.properties![key] as JsonSchema7).writeOnly !== true) {
            variableNames.push(key);
          }
        });
      }

      const variableObject = await this.getVariables(
        this.taskId,
        variableNames
      );

      result.variables = variableObject;

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

    result.input = {
      schema: schema,
      uischema: uischema as UISchemaElement,
      data: data,
    };

    return result as CamundaFormContext;
  }

  async getVariables(
    taskId: string,
    variableNames: string[]
  ): Promise<Record<string, VariableValue>> {
    const response = await fetch(
      `${
        this.camundaUrl
      }/task/${taskId}/form-variables?deserializeValues=false&variableNames=${variableNames.join(
        ','
      )}`
    ).catch((e) => {
      throw new Error(`Unable to retrieve camunda variables: ${e}`);
    });

    return response.json().catch((e) => {
      throw new Error(
        `Invalid JSON response when retrieving camunda variables: ${e}`
      );
    });
  }

  async getDeploymentResources(deploymentId: string): Promise<Resource[]> {
    const response = await fetch(
      `${this.camundaUrl}/deployment/${deploymentId}/resources`
    ).catch((e) => {
      throw new Error(`Unable to retrieve resources for deployment: ${e}`);
    });

    return response.json().catch((e) => {
      throw new Error(
        `Invalid JSON response when retrieving resources for deployment: ${e}`
      );
    });
  }

  async getDeploymentResourceJsonData(
    deploymentId: string,
    resourceId: string,
    resourceErrorMessage: string,
    jsonErrorMessage: string
  ): Promise<Record<string, any>> {
    const response = await fetch(
      `${this.camundaUrl}/deployment/${deploymentId}/resources/${resourceId}/data`
    ).catch((e) => {
      throw new Error(`${resourceErrorMessage}: ${e}`);
    });
    return response.json().catch((e) => {
      `${jsonErrorMessage}: ${e}`;
    });
  }

  async getTask(taskId: string): Promise<Task> {
    const response = await fetch(`${this.camundaUrl}/task/${taskId}`).catch(
      (e) => {
        throw new Error(`Unable to retrieve task: ${e}`);
      }
    );

    return response.json().catch((e) => {
      throw new Error(`Invalid JSON response when retrieving task: ${e}`);
    });
  }

  async getProcessDefinition(
    processDefinitionId: string
  ): Promise<ProcessDefinition> {
    const response = await fetch(
      `${this.camundaUrl}/process-definition/${processDefinitionId}`
    ).catch((e) => {
      throw new Error(`Unable to retrieve process definition: ${e}`);
    });

    return response.json().catch((e) => {
      throw new Error(
        `Invalid JSON response when retrieving process definition: ${e}`
      );
    });
  }
}
