import { JsonSchema, JsonSchema7, UISchemaElement } from '@jsonforms/core';
import forOwn from 'lodash/forOwn';
import {
  getDeploymentResourceJsonData,
  getDeploymentResources,
  getProcessDefinition,
  getProcessDefinitionByKey,
  getProcessDefinitionDeployedStartFormById,
  getProcessDefinitionDeployedStartFormByKey,
  getProcessDefinitionFormVariablesById,
  getProcessDefinitionFormVariablesByKey,
  getProcessDefinitionStartFormById,
  getProcessDefinitionStartFormByKey,
  getTask,
  getTaskDeployedForm,
  getTaskForm,
  getTaskFormVariables,
} from './camunda';
import { AppErrorCode, AppException } from './errors';
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
  TaskForm,
  ValueInfo,
  VariableValue,
} from './types';

import {
  RestClient,
  ResponseException,
  RESOURCE_I18N_SUFFIX,
  RESOURCE_SCHEMA_SUFFIX,
  RESOURCE_UISCHEMA_SUFFIX,
} from '@kchobantonov/common-jsonforms';

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
  constructor(private config: CamundaFormConfig) {}

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
        forOwn(schema.properties, function (_value: any, key: string) {
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

    const url = this.submitFormUrl(context, action);

    const headers = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    };

    const response = await client.fetch(url, {
      body: JSON.stringify(payload),
      headers,
      method: 'post',
    });

    if (!response.ok) {
      let errorMessage = response.statusText;
      try {
        const errorAsJson = await response.json();
        if (errorAsJson.message) {
          errorMessage = errorAsJson.message;
        }
      } catch (e) {
      }
      throw new AppException(
        AppErrorCode.SUBMIT_FORM,
        new ResponseException(response),
        {
          message: errorMessage,
        }
      );
    }
  }

  submitFormUrl(context: CamundaFormContext, action: Action): string {
    if (isTaskIdConfig(context.config)) {
      return `${this.config.url}/task/${encodeURIComponent(
        context.config.taskId
      )}/${encodeURIComponent(this.toCamudaPath(action))}`;
    }

    if (isProcessDefinitionIdConfig(context.config)) {
      const actionPath = this.toCamudaPath(action);
      if (actionPath !== 'submit-form') {
        throw new AppException(AppErrorCode.UNSUPPORTED_ACTION, undefined, {
          action: action,
        });
      }
      return `${this.config.url}/process-definition/${encodeURIComponent(
        context.config.processDefinitionId
      )}/${encodeURIComponent(actionPath)}`;
    }

    if (isProcessDefinitionKeyConfig(context.config)) {
      const actionPath = this.toCamudaPath(action);
      if (actionPath !== 'submit-form') {
        throw new AppException(AppErrorCode.UNSUPPORTED_ACTION, undefined, {
          action: action,
        });
      }
      if (context.config.tenantId) {
        return `${this.config.url}/process-definition/key/${encodeURIComponent(
          context.config.processDefinitionKey
        )}/tenant-id/${encodeURIComponent(
          context.config.tenantId
        )}/${encodeURIComponent(actionPath)}`;
      }

      return `${this.config.url}/process-definition/key/${encodeURIComponent(
        context.config.processDefinitionKey
      )}/${encodeURIComponent(actionPath)}`;
    }

    throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_CONFIG);
  }

  async loadForm(client: RestClient): Promise<CamundaFormContext> {
    const result: Partial<CamundaFormContext> = { config: this.config };

    if (isTaskIdConfig(this.config)) {
      const taskForm = await getTaskForm(
        client,
        this.config.url,
        this.config.taskId
      );
      if (!taskForm.key) {
        throw new AppException(AppErrorCode.INVALID_TASK_FORM_RESPONSE);
      }
      result.taskForm = taskForm;
    } else {
      if (isProcessDefinitionIdConfig(this.config)) {
        const taskForm = await getProcessDefinitionStartFormById(
          client,
          this.config.url,
          this.config.processDefinitionId
        );
        if (!taskForm.key) {
          throw new AppException(AppErrorCode.INVALID_TASK_FORM_RESPONSE);
        }
        result.taskForm = taskForm;
      } else if (isProcessDefinitionKeyConfig(this.config)) {
        const taskForm = await getProcessDefinitionStartFormByKey(
          client,
          this.config.url,
          this.config.processDefinitionKey,
          this.config.tenantId
        );
        if (!taskForm.key) {
          throw new AppException(AppErrorCode.INVALID_TASK_FORM_RESPONSE);
        }
        result.taskForm = taskForm;
      } else {
        throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_CONFIG);
      }
    }

    const { schema, schemaUrl, uischema, i18n } = await this.loadResources(
      client,
      result.taskForm
    );
    if (i18n) {
      result.translations = i18n;
    }

    const data: any = {};

    const variableNames: string[] = [];

    if (schema && schema.properties) {
      forOwn(schema.properties, function (_value: any, key: string) {
        if ((schema.properties![key] as JsonSchema7).writeOnly !== true) {
          variableNames.push(key);
        }
      });
    }

    if (isTaskIdConfig(this.config)) {
      const variableObject = await getTaskFormVariables(
        client,
        this.config.url,
        this.config.taskId,
        variableNames
      );

      result.variables = variableObject;
    } else if (isProcessDefinitionIdConfig(this.config)) {
      const variableObject = await getProcessDefinitionFormVariablesById(
        client,
        this.config.url,
        this.config.processDefinitionId,
        variableNames
      );
      result.variables = variableObject;
    } else if (isProcessDefinitionKeyConfig(this.config)) {
      const variableObject = await getProcessDefinitionFormVariablesByKey(
        client,
        this.config.url,
        this.config.processDefinitionKey,
        variableNames,
        this.config.tenantId
      );
      result.variables = variableObject;
    }

    forOwn(result.variables, function (value: VariableValue, key: string) {
      if (value.type === 'Json') {
        value.value = value.value ? JSON.parse(value.value) : value.value;
      }

      if (value.value) {
        // set only when the value is not undefined or null
        data[key] = value.value;
      }
    });

    result.input = {
      schema: schema,
      schemaUrl: schemaUrl,
      uischema: uischema ?? undefined,
      data: data,
    };

    if (!(result.input.schema as any).$id) {
      (result.input.schema as any).$id = '/';
    }
    return result as CamundaFormContext;
  }

  private async loadResources(client: RestClient, taskForm: TaskForm) {
    const deploymentLocation = getParameterByName(
      CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT,
      taskForm.key
    );
    const pathLocation = getParameterByName(
      CAMUNDA_FORM_KEY_QUERY_PARAM_PATH,
      taskForm.key
    );

    if (pathLocation) {
      if (!this.validPathLocation(pathLocation)) {
        throw new AppException(
          AppErrorCode.INVALID_CAMUNDA_FORM_KEY_PATH_PARAM,
          undefined,
          {
            formKey: taskForm.key,
            CAMUNDA_FORM_KEY_QUERY_PARAM_PATH: pathLocation,
          }
        );
      }
      // path should already start with /
      const contextPath =
        taskForm.contextPath && taskForm.contextPath !== '/'
          ? taskForm.contextPath
          : '';
      return this.loadResourcesFromPath(client, contextPath + pathLocation);
    } else if (deploymentLocation) {
      if (!this.validDeploymentLocation(deploymentLocation)) {
        throw new AppException(
          AppErrorCode.INVALID_CAMUNDA_FORM_KEY_DEPLOYMENT_PARAM,
          undefined,
          {
            formKey: taskForm.key,
            CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT: deploymentLocation,
          }
        );
      }

      // try to load all resources with one call but that depends if the JsonFormsFormServicePlugin was installed on the server side
      try {
        return await this.loadResourcesFromDeployedForm(
          client,
          deploymentLocation
        );
      } catch (e) {
        // ignore any errors for deployed forms but rest interceptors will be notified for that.
      }

      // load the resources by looking into the deployed resources
      const deploymentId = await this.getDeploymentId(client);
      return this.loadResourcesFromDeployment(
        client,
        deploymentLocation,
        deploymentId
      );
    } else {
      throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_KEY, undefined, {
        formKey: taskForm.key,
      });
    }
  }

  private validDeploymentLocation(location?: string | null) {
    return (
      location !== undefined && location !== null && location.trim().length > 0
    );
  }

  private validPathLocation(location?: string | null) {
    return (
      location !== undefined &&
      location !== null &&
      location.startsWith('/') &&
      location.trim().length > 1
    );
  }

  private async getDeploymentId(client: RestClient) {
    // last check directly for resources
    let deploymentId: string | undefined = undefined;
    if (isTaskIdConfig(this.config)) {
      const task = await getTask(client, this.config.url, this.config.taskId);
      if (!task.processDefinitionId) {
        throw new AppException(AppErrorCode.INVALID_TASK_RESPONSE);
      }
      const processDefinition = await getProcessDefinition(
        client,
        this.config.url,
        task.processDefinitionId
      );
      if (!processDefinition.deploymentId) {
        throw new AppException(
          AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE
        );
      }
      deploymentId = processDefinition.deploymentId;
    } else if (isProcessDefinitionIdConfig(this.config)) {
      const processDefinition = await getProcessDefinition(
        client,
        this.config.url,
        this.config.processDefinitionId
      );
      if (!processDefinition.deploymentId) {
        throw new AppException(
          AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE
        );
      }
      deploymentId = processDefinition.deploymentId;
    } else if (isProcessDefinitionKeyConfig(this.config)) {
      const processDefinition = await getProcessDefinitionByKey(
        client,
        this.config.url,
        this.config.processDefinitionKey,
        this.config.tenantId
      );
      if (!processDefinition.deploymentId) {
        throw new AppException(
          AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE
        );
      }
      deploymentId = processDefinition.deploymentId;
    } else {
      throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_CONFIG);
    }

    return deploymentId;
  }

  private async loadResourcesFromDeployedForm(
    client: RestClient,
    deploymentLocation: string
  ) {
    let resources: Record<string, any> | undefined = undefined;
    if (isTaskIdConfig(this.config)) {
      resources = await getTaskDeployedForm(
        client,
        this.config.url,
        this.config.taskId
      );
    } else if (isProcessDefinitionIdConfig(this.config)) {
      resources = await getProcessDefinitionDeployedStartFormById(
        client,
        this.config.url,
        this.config.processDefinitionId
      );
    } else if (isProcessDefinitionKeyConfig(this.config)) {
      resources = await getProcessDefinitionDeployedStartFormByKey(
        client,
        this.config.url,
        this.config.processDefinitionKey,
        this.config.tenantId
      );
    } else {
      throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_CONFIG);
    }

    const schema = resources[`${deploymentLocation + RESOURCE_SCHEMA_SUFFIX}`];
    const uischema =
      resources[`${deploymentLocation + RESOURCE_UISCHEMA_SUFFIX}`];
    const i18n = resources[`${deploymentLocation + RESOURCE_I18N_SUFFIX}`];

    const schemaUrl = `${this.config.url}/${encodeURIComponent(
      deploymentLocation + RESOURCE_SCHEMA_SUFFIX
    )}`;

    if (!schema) {
      throw new AppException(AppErrorCode.MISSING_JSONFORMS_SCHEMA);
    }
    if (!uischema) {
      throw new AppException(AppErrorCode.MISSING_JSONFORMS_UISCHEMA);
    }

    return { schema, schemaUrl, uischema, i18n };
  }

  private async loadResourcesFromPath(
    client: RestClient,
    pathLocation: string
  ) {
    const schemaUrl = `${pathLocation}${RESOURCE_SCHEMA_SUFFIX}`;
    const schemaResponse = await client.fetch(schemaUrl).catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA, e);
    });
    if (!schemaResponse.ok) {
      throw new AppException(
        AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA,
        new ResponseException(schemaResponse)
      );
    }
    const schema = (await schemaResponse.json().catch((e) => {
      throw new AppException(AppErrorCode.INVALID_JSONFORMS_SCHEMA, e);
    })) as JsonSchema;

    let uischema: UISchemaElement | null = null;
    let i18n: Record<string, any> | null = null;

    const uischemaResponse = await client
      .fetch(`${pathLocation}${RESOURCE_UISCHEMA_SUFFIX}`)
      .catch((e) => {
        throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_UISCHEMA, e);
      });
    if (uischemaResponse.ok) {
      uischema = await uischemaResponse.json().catch((e) => {
        throw new AppException(AppErrorCode.INVALID_JSONFORMS_UISCHEMA, e);
      });
    }

    const i18nResponse = await client
      .fetch(`${pathLocation}${RESOURCE_I18N_SUFFIX}`)
      .catch((e) => {
        throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_I18N, e);
      });
    if (i18nResponse.ok) {
      i18n = await i18nResponse.json().catch((e) => {
        throw new AppException(AppErrorCode.INVALID_JSONFORMS_I18N, e);
      });
    }

    return { schema, schemaUrl, uischema, i18n };
  }

  private async loadResourcesFromDeployment(
    client: RestClient,
    deploymentLocation: string,
    deploymentId: string
  ) {
    const resources = await getDeploymentResources(
      client,
      this.config.url,
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

    const schema = (await getDeploymentResourceJsonData(
      client,
      this.config.url,
      deploymentId,
      formSchemaResource.id,
      AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA,
      AppErrorCode.INVALID_JSONFORMS_SCHEMA
    )) as JsonSchema;

    const schemaUrl = `${this.config.url}/deployment/${encodeURIComponent(
      deploymentId
    )}/resources/${encodeURIComponent(formSchemaResource.id)}/data`;

    const uischema = (await getDeploymentResourceJsonData(
      client,
      this.config.url,
      deploymentId,
      formUiResource.id,
      AppErrorCode.RETRIEVE_JSONFORMS_UISCHEMA,

      AppErrorCode.INVALID_JSONFORMS_UISCHEMA
    )) as UISchemaElement;

    let i18n: Record<string, any> | null = null;
    if (i18nResource) {
      i18n = await getDeploymentResourceJsonData(
        client,
        this.config.url,
        deploymentId,
        i18nResource.id,
        AppErrorCode.RETRIEVE_JSONFORMS_I18N,

        AppErrorCode.INVALID_JSONFORMS_I18N
      );
    }

    return { schema, schemaUrl, uischema, i18n };
  }
}
