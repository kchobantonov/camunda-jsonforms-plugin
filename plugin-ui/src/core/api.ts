import { JsonSchema, JsonSchema7, UISchemaElement } from '@jsonforms/core';
import forOwn from 'lodash/forOwn';
import {
  getDeploymentResourceJsonData,
  getDeploymentResources,
  getProcessDefinition,
  getProcessDefinitionByKey,
  getProcessDefinitionStartForm,
  getTask,
  getVariables,
} from './camunda';
import { AppErrorCode, AppException } from './errors';
import { RestClient } from './rest';
import {
  Action,
  CamundaFormConfig,
  CamundaFormContext,
  CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT,
  CAMUNDA_FORM_KEY_QUERY_PARAM_PATH,
  FileValueInfo,
  isProcessDefinitionIdConfig,
  isProcessDefinitionKeyConfig,
  isTaskIdConfig,
  RESOURCE_I18N_SUFFIX,
  RESOURCE_SCHEMA_SUFFIX,
  RESOURCE_UISCHEMA_SUFFIX,
  ValueInfo,
} from './types';

export const getParameterByName = (
  name: string,
  url: string | undefined
): string | null => {
  if (!url) return null;

  const parts = url.split('?');
  if (parts.length > 1) {
    const params = new URLSearchParams('?' + parts[1]);

    return params.get(name);
  }

  return null;
};

const getCamundaType = (schema: JsonSchema): string => {
  switch (schema.type) {
    case 'string':
      return (schema as any).format === 'binary' ? 'File' : 'String';
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
    const valueInfo: ValueInfo = {};

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

export class CamundaFormApi {
  constructor(private config: CamundaFormConfig) { }

  private toCamudaPath(action: Action) {
    switch (action) {
      case 'submit':
        return 'submit-form';
      case 'submit-without-data':
        return 'submit-form';
      case 'complete':
        return 'complete';
      case 'complete-without-data':
        return 'complete';
      case 'resolve':
        return 'resolve';
      case 'resolve-without-data':
        return 'resolve';
      case 'error':
        return 'bpmnError';
      case 'escalation':
        return 'bpmnEscalation';
    }
  }

  async submitForm(
    client: RestClient,
    schema: JsonSchema,
    data: Record<string, any>,
    context: CamundaFormContext,
    action: Action,
    payload?: Record<string, any>
  ) {
    if (!payload) {
      payload = {};
    }

    const includeDataVariables =
      action == 'submit' || action == 'resolve' || action == 'complete';

    if (includeDataVariables) {
      const dataVariables = {};
      if (schema && schema.properties) {
        forOwn(schema.properties, function (value: any, key: string) {
          if (Object.prototype.hasOwnProperty.call(data, key)) {
            attachCamundaVariable(
              dataVariables,
              key,
              schema.properties![key],
              data[key]
            );
          }
        });
      }

      // the variables from button have presedence
      payload.variables = payload.variables
        ? {
          ...dataVariables,
          ...payload.variables,
        }
        : dataVariables;
    }

    const url = context.task
      ? `${this.config.camundaUrl}/task/${context.task.id}/${this.toCamudaPath(
        action
      )}`
      : `${this.config.camundaUrl}/process-definition/${context.processDefinition.id
      }/${this.toCamudaPath(action)}`;

    const headers = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    };

    await client.fetch(url, {
      body: JSON.stringify(payload),
      headers,
      method: 'post',
    });
  }

  async loadForm(client: RestClient): Promise<CamundaFormContext> {
    const result: Partial<CamundaFormContext> = {};

    if (isTaskIdConfig(this.config)) {
      const task = await getTask(
        client,
        this.config.camundaUrl,
        this.config.taskId
      );

      if (!task.processDefinitionId) {
        throw new AppException(AppErrorCode.INVALID_TASK_RESPONSE);
      }

      result.task = task;
      result.processDefinition = await getProcessDefinition(
        client,
        this.config.camundaUrl,
        task.processDefinitionId
      );
    } else if (isProcessDefinitionIdConfig(this.config)) {
      result.processDefinition = await getProcessDefinition(
        client,
        this.config.camundaUrl,
        this.config.processDefinitionId
      );
    } else if (isProcessDefinitionKeyConfig(this.config)) {
      result.processDefinition = await getProcessDefinitionByKey(
        client,
        this.config.camundaUrl,
        this.config.processDefinitionKey
      );
    } else {
      throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_CONFIG);
    }

    const formKey =
      result.task !== undefined
        ? result.task.formKey
        : (
          await getProcessDefinitionStartForm(
            client,
            this.config.camundaUrl,
            result.processDefinition.id
          )
        ).key;
    const deploymentLocation = getParameterByName(
      CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT,
      formKey
    );
    const pathLocation = getParameterByName(
      CAMUNDA_FORM_KEY_QUERY_PARAM_PATH,
      formKey
    );

    if (!this.validDeploymentLocation(deploymentLocation) && !this.validPathLocation(pathLocation)) {
      throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_KEY, {
        formKey: formKey,
      });
    }

    const { schema, uischema, i18n } = deploymentLocation ? await this.loadResourcesFromDeployment(client, deploymentLocation, result.processDefinition.deploymentId!) : await this.loadResourcesFromPath(client, pathLocation!);
    if (i18n) {
      result.translations = i18n;
    }

    const data: any = {};

    if (result.task) {
      const variableNames: string[] = [];

      if (schema && schema.properties) {
        forOwn(schema.properties, function (value: any, key: string) {
          if ((schema.properties![key] as JsonSchema7).writeOnly !== true) {
            variableNames.push(key);
          }
        });
      }

      const variableObject = await getVariables(
        client,
        this.config.camundaUrl,
        result.task.id,
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
      uischema: uischema ?? undefined,
      data: data,
    };

    if (!(result.input.schema as any).$id) {
      (result.input.schema as any).$id = '/';
    }
    return result as CamundaFormContext;
  }

  private validDeploymentLocation(location?: string | null) {
    return location !== undefined && location !== null;
  }

  private validPathLocation(location?: string | null) {
    return location !== undefined && location !== null && location.startsWith('/');
  }

  private async loadResourcesFromPath(client: RestClient, pathLocation: string) {
    const schemaResponse = await client
      .fetch(
        `${pathLocation}${RESOURCE_SCHEMA_SUFFIX}`
      )
      .catch((e) => {
        throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA, e);
      });
    if (!schemaResponse.ok) {
      throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA, e);
    }
    const schema = await schemaResponse.json().catch((e) => {
      throw new AppException(AppErrorCode.INVALID_JSONFORMS_SCHEMA, e);
    }) as JsonSchema;

    let uischema: UISchemaElement | null = null;
    let i18n: Record<string, any> | null = null;

    const uischemaResponse = await client.fetch(`${pathLocation}${RESOURCE_UISCHEMA_SUFFIX}`).catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_UISCHEMA, e);
    });
    if (uischemaResponse.ok) {
      uischema = await uischemaResponse.json().catch((e) => {
        throw new AppException(AppErrorCode.INVALID_JSONFORMS_UISCHEMA, e);
      });
    }

    const i18nResponse = await client.fetch(`${pathLocation}${RESOURCE_I18N_SUFFIX}`).catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_I18N, e);
    });
    if (i18nResponse.ok) {
      i18n = await i18nResponse.json().catch((e) => {
        throw new AppException(AppErrorCode.INVALID_JSONFORMS_I18N, e);
      });
    }

    return { schema, uischema, i18n };
  }

  private async loadResourcesFromDeployment(client: RestClient, deploymentLocation: string, deploymentId: string) {
    const resources = await getDeploymentResources(
      client,
      this.config.camundaUrl,
      deploymentId
    );

    const formSchemaResource = resources.find(
      (i) => i.name === deploymentLocation + RESOURCE_SCHEMA_SUFFIX
    );
    const formUiResource = resources.find(
      (i) => i.name === deploymentLocation + RESOURCE_UISCHEMA_SUFFIX
    );
    const i18nResource = resources.find(
      (i) => i.name === deploymentLocation + RESOURCE_I18N_SUFFIX
    );

    if (formSchemaResource == null) {
      throw new AppException(AppErrorCode.MISSING_JSONFORMS_SCHEMA);
    }

    if (formUiResource == null) {
      throw new AppException(AppErrorCode.MISSING_JSONFORMS_UISCHEMA);
    }

    const schema = await getDeploymentResourceJsonData(
      client,
      this.config.camundaUrl,
      deploymentId!,
      formSchemaResource.id,
      AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA,
      AppErrorCode.INVALID_JSONFORMS_SCHEMA
    ) as JsonSchema;


    const uischema = await getDeploymentResourceJsonData(
      client,
      this.config.camundaUrl,
      deploymentId!,
      formUiResource.id,
      AppErrorCode.RETRIEVE_JSONFORMS_UISCHEMA,

      AppErrorCode.INVALID_JSONFORMS_UISCHEMA
    ) as UISchemaElement;

    let i18n: Record<string, any> | null = null;
    if (i18nResource) {
      i18n = await getDeploymentResourceJsonData(
        client,
        this.config.camundaUrl,
        deploymentId!,
        i18nResource.id,
        AppErrorCode.RETRIEVE_JSONFORMS_I18N,

        AppErrorCode.INVALID_JSONFORMS_I18N
      );

    }

    return { schema, uischema, i18n };
  }
}
