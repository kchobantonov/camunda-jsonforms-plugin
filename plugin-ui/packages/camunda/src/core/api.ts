import type {
  JsonFormsUISchemaRegistryEntry,
  JsonSchema,
  JsonSchema7,
  UISchemaElement,
} from '@jsonforms/core';
import forOwn from 'lodash/forOwn';
import {
  getDeploymentResourceJsonData,
  getDeploymentResources,
  getDeploymentResourceTextData,
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
import { RestClient } from './rest';
import {
  type Action,
  type CamundaFormConfig,
  type CamundaFormContext,
  CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT,
  CAMUNDA_FORM_KEY_QUERY_PARAM_PATH,
  type FileValueInfo,
  isProcessDefinitionIdConfig,
  isProcessDefinitionKeyConfig,
  isTaskIdConfig,
  RESOURCE_I18N_SUFFIX,
  RESOURCE_SCHEMA_SUFFIX,
  RESOURCE_UISCHEMA_SUFFIX,
  ResponseException,
  type TaskForm,
  type ValueInfo,
  type VariableValue,
  RESOURCE_UISCHEMAS_SUFFIX,
  RESOURCE_UIDATA_SUFFIX,
} from './types';
import { parseAndTransformUISchemaRegistryEntries } from '@chobantonov/jsonforms-vuetify-renderers';
import { reactive } from 'vue';

export const getParameterByName = (
  name: string,
  url: string | undefined,
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
  variableData: any,
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

      const fileName =
        fileNameIndex !== -1
          ? decodeURIComponent(
              header.substring(fileNameIndex + ';filename='.length),
            )
          : variableName;

      const mimeType = header.substring(
        'data:'.length,
        fileNameIndex !== -1 ? fileNameIndex : header.length,
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
  private toCamudaPath(action: Action) {
    switch (action) {
      case 'camunda:submit':
        return 'submit-form';
      case 'camunda:submit-without-data':
        return 'submit-form';
      case 'camunda:complete':
        return 'complete';
      case 'camunda:complete-without-data':
        return 'complete';
      case 'camunda:resolve':
        return 'resolve';
      case 'camunda:resolve-without-data':
        return 'resolve';
      case 'camunda:error':
        return 'bpmnError';
      case 'camunda:escalation':
        return 'bpmnEscalation';
    }
  }

  async submitForm(
    client: RestClient,
    schema: JsonSchema,
    data: Record<string, any>,
    context: CamundaFormContext,
    action: Action,
    payload?: Record<string, any>,
  ) {
    if (!payload) {
      payload = {};
    }

    const includeDataVariables =
      action == 'camunda:submit' ||
      action == 'camunda:resolve' ||
      action == 'camunda:complete';

    if (includeDataVariables) {
      const dataVariables = {};
      if (schema && schema.properties) {
        forOwn(schema.properties, function (_value: any, key: string) {
          if (Object.prototype.hasOwnProperty.call(data, key)) {
            attachCamundaVariable(
              dataVariables,
              key,
              schema.properties![key],
              data[key],
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
      let errorAsJson: any = undefined;
      try {
        errorAsJson = await response.json();
        if (errorAsJson.message) {
          errorMessage = errorAsJson.message;
        }
      } catch (e) {
        // ignore
      }
      throw new AppException(
        AppErrorCode.SUBMIT_FORM,
        new ResponseException(response),
        {
          message: errorMessage,
        },
        errorAsJson,
      );
    }
  }

  submitFormUrl(context: CamundaFormContext, action: Action): string {
    if (isTaskIdConfig(context.camundaFormConfig)) {
      return `${context.camundaFormConfig.url}/task/${encodeURIComponent(
        context.camundaFormConfig.taskId,
      )}/${encodeURIComponent(this.toCamudaPath(action))}`;
    }

    if (isProcessDefinitionIdConfig(context.camundaFormConfig)) {
      const actionPath = this.toCamudaPath(action);
      if (actionPath !== 'submit-form') {
        throw new AppException(AppErrorCode.UNSUPPORTED_ACTION, undefined, {
          action: action,
        });
      }
      return `${context.camundaFormConfig.url}/process-definition/${encodeURIComponent(
        context.camundaFormConfig.processDefinitionId,
      )}/${encodeURIComponent(actionPath)}`;
    }

    if (isProcessDefinitionKeyConfig(context.camundaFormConfig)) {
      const actionPath = this.toCamudaPath(action);
      if (actionPath !== 'submit-form') {
        throw new AppException(AppErrorCode.UNSUPPORTED_ACTION, undefined, {
          action: action,
        });
      }
      if (context.camundaFormConfig.tenantId) {
        return `${
          context.camundaFormConfig.url
        }/process-definition/key/${encodeURIComponent(
          context.camundaFormConfig.processDefinitionKey,
        )}/tenant-id/${encodeURIComponent(
          context.camundaFormConfig.tenantId,
        )}/${encodeURIComponent(actionPath)}`;
      }

      return `${context.camundaFormConfig.url}/process-definition/key/${encodeURIComponent(
        context.camundaFormConfig.processDefinitionKey,
      )}/${encodeURIComponent(actionPath)}`;
    }

    throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_CONFIG);
  }

  async loadForm(
    client: RestClient,
    camundaFormConfig: CamundaFormConfig,
  ): Promise<CamundaFormContext> {
    const result: Partial<CamundaFormContext> = {
      camundaFormConfig: camundaFormConfig,
    };

    if (isTaskIdConfig(camundaFormConfig)) {
      const taskForm = await getTaskForm(
        client,
        camundaFormConfig.url,
        camundaFormConfig.taskId,
      );
      if (!taskForm.key) {
        throw new AppException(AppErrorCode.INVALID_TASK_FORM_RESPONSE);
      }
      result.taskForm = taskForm;
    } else {
      if (isProcessDefinitionIdConfig(camundaFormConfig)) {
        const taskForm = await getProcessDefinitionStartFormById(
          client,
          camundaFormConfig.url,
          camundaFormConfig.processDefinitionId,
        );
        if (!taskForm.key) {
          throw new AppException(AppErrorCode.INVALID_TASK_FORM_RESPONSE);
        }
        result.taskForm = taskForm;
      } else if (isProcessDefinitionKeyConfig(camundaFormConfig)) {
        const taskForm = await getProcessDefinitionStartFormByKey(
          client,
          camundaFormConfig.url,
          camundaFormConfig.processDefinitionKey,
          camundaFormConfig.tenantId,
        );
        if (!taskForm.key) {
          throw new AppException(AppErrorCode.INVALID_TASK_FORM_RESPONSE);
        }
        result.taskForm = taskForm;
      } else {
        throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_CONFIG);
      }
    }

    const { schema, schemaUrl, uischema, i18n, uischemas, uidata } =
      await this.loadResources(client, result.taskForm, camundaFormConfig);
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

    if (isTaskIdConfig(camundaFormConfig)) {
      const variableObject = await getTaskFormVariables(
        client,
        camundaFormConfig.url,
        camundaFormConfig.taskId,
        variableNames,
      );

      result.variables = variableObject;
    } else if (isProcessDefinitionIdConfig(camundaFormConfig)) {
      const variableObject = await getProcessDefinitionFormVariablesById(
        client,
        camundaFormConfig.url,
        camundaFormConfig.processDefinitionId,
        variableNames,
      );
      result.variables = variableObject;
    } else if (isProcessDefinitionKeyConfig(camundaFormConfig)) {
      const variableObject = await getProcessDefinitionFormVariablesByKey(
        client,
        camundaFormConfig.url,
        camundaFormConfig.processDefinitionKey,
        variableNames,
        camundaFormConfig.tenantId,
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

    result.schema = schema;
    result.schemaUrl = schemaUrl;
    result.uischema = uischema;
    result.data = data;
    result.uischemas = uischemas;
    result.uidata = reactive<Record<string, any>>(uidata);

    if (!(result.schema as any).$id) {
      (result.schema as any).$id = '/';
    }
    return result as CamundaFormContext;
  }

  private async loadResources(
    client: RestClient,
    taskForm: TaskForm,
    camundaFormConfig: CamundaFormConfig,
  ) {
    const deploymentLocation = getParameterByName(
      CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT,
      taskForm.key,
    );
    const pathLocation = getParameterByName(
      CAMUNDA_FORM_KEY_QUERY_PARAM_PATH,
      taskForm.key,
    );

    if (pathLocation) {
      if (!this.validPathLocation(pathLocation)) {
        throw new AppException(
          AppErrorCode.INVALID_CAMUNDA_FORM_KEY_PATH_PARAM,
          undefined,
          {
            formKey: taskForm.key,
            CAMUNDA_FORM_KEY_QUERY_PARAM_PATH: pathLocation,
          },
        );
      }
      // path should already start with /
      const contextPath =
        taskForm.contextPath && taskForm.contextPath !== '/'
          ? taskForm.contextPath
          : '';

      let resourcePath = contextPath + pathLocation;
      try {
        resourcePath = new URL(resourcePath, camundaFormConfig.url).toString();
      } catch {
        // ignore if the camundaFormConfig.url is relative URL
      }
      return this.loadResourcesFromPath(client, resourcePath);
    } else if (deploymentLocation) {
      if (!this.validDeploymentLocation(deploymentLocation)) {
        throw new AppException(
          AppErrorCode.INVALID_CAMUNDA_FORM_KEY_DEPLOYMENT_PARAM,
          undefined,
          {
            formKey: taskForm.key,
            CAMUNDA_FORM_KEY_QUERY_PARAM_DEPLOYMENT: deploymentLocation,
          },
        );
      }

      // try to load all resources with one call but that depends if the JsonFormsFormServicePlugin was installed on the server side
      try {
        return await this.loadResourcesFromDeployedForm(
          client,
          deploymentLocation,
          camundaFormConfig,
        );
      } catch (e) {
        // ignore any errors for deployed forms but rest interceptors will be notified for that.
      }

      // load the resources by looking into the deployed resources
      const deploymentId = await this.getDeploymentId(
        client,
        camundaFormConfig,
      );
      return this.loadResourcesFromDeployment(
        client,
        deploymentLocation,
        deploymentId,
        camundaFormConfig,
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

  private async getDeploymentId(
    client: RestClient,
    camundaFormConfig: CamundaFormConfig,
  ) {
    // last check directly for resources
    let deploymentId: string | undefined = undefined;
    if (isTaskIdConfig(camundaFormConfig)) {
      const task = await getTask(
        client,
        camundaFormConfig.url,
        camundaFormConfig.taskId,
      );
      if (!task.processDefinitionId) {
        throw new AppException(AppErrorCode.INVALID_TASK_RESPONSE);
      }
      const processDefinition = await getProcessDefinition(
        client,
        camundaFormConfig.url,
        task.processDefinitionId,
      );
      if (!processDefinition.deploymentId) {
        throw new AppException(
          AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE,
        );
      }
      deploymentId = processDefinition.deploymentId;
    } else if (isProcessDefinitionIdConfig(camundaFormConfig)) {
      const processDefinition = await getProcessDefinition(
        client,
        camundaFormConfig.url,
        camundaFormConfig.processDefinitionId,
      );
      if (!processDefinition.deploymentId) {
        throw new AppException(
          AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE,
        );
      }
      deploymentId = processDefinition.deploymentId;
    } else if (isProcessDefinitionKeyConfig(camundaFormConfig)) {
      const processDefinition = await getProcessDefinitionByKey(
        client,
        camundaFormConfig.url,
        camundaFormConfig.processDefinitionKey,
        camundaFormConfig.tenantId,
      );
      if (!processDefinition.deploymentId) {
        throw new AppException(
          AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE,
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
    deploymentLocation: string,
    camundaFormConfig: CamundaFormConfig,
  ) {
    let resources: Record<string, any> | undefined = undefined;
    if (isTaskIdConfig(camundaFormConfig)) {
      resources = await getTaskDeployedForm(
        client,
        camundaFormConfig.url,
        camundaFormConfig.taskId,
      );
    } else if (isProcessDefinitionIdConfig(camundaFormConfig)) {
      resources = await getProcessDefinitionDeployedStartFormById(
        client,
        camundaFormConfig.url,
        camundaFormConfig.processDefinitionId,
      );
    } else if (isProcessDefinitionKeyConfig(camundaFormConfig)) {
      resources = await getProcessDefinitionDeployedStartFormByKey(
        client,
        camundaFormConfig.url,
        camundaFormConfig.processDefinitionKey,
        camundaFormConfig.tenantId,
      );
    } else {
      throw new AppException(AppErrorCode.INVALID_CAMUNDA_FORM_CONFIG);
    }

    const schema = resources[`${deploymentLocation + RESOURCE_SCHEMA_SUFFIX}`];
    const uischema =
      resources[`${deploymentLocation + RESOURCE_UISCHEMA_SUFFIX}`];
    const i18n = resources[`${deploymentLocation + RESOURCE_I18N_SUFFIX}`];
    const uischemas =
      resources[`${deploymentLocation + RESOURCE_UISCHEMAS_SUFFIX}`];
    const uidata = resources[`${deploymentLocation + RESOURCE_UIDATA_SUFFIX}`];

    const schemaUrl = `${camundaFormConfig.url}/${encodeURIComponent(
      deploymentLocation + RESOURCE_SCHEMA_SUFFIX,
    )}`;

    if (!schema) {
      throw new AppException(AppErrorCode.MISSING_JSONFORMS_SCHEMA);
    }
    if (!uischema) {
      throw new AppException(AppErrorCode.MISSING_JSONFORMS_UISCHEMA);
    }

    return { schema, schemaUrl, uischema, i18n, uischemas, uidata };
  }

  private async loadResourcesFromPath(
    client: RestClient,
    pathLocation: string,
  ) {
    const schemaUrl = `${pathLocation}${RESOURCE_SCHEMA_SUFFIX}`;
    const schemaResponse = await client.fetch(schemaUrl).catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA, e);
    });
    if (!schemaResponse.ok) {
      throw new AppException(
        AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA,
        new ResponseException(schemaResponse),
      );
    }
    const schema = (await schemaResponse.json().catch((e) => {
      throw new AppException(AppErrorCode.INVALID_JSONFORMS_SCHEMA, e);
    })) as JsonSchema;

    let uischema: UISchemaElement | null = null;
    let i18n: Record<string, any> | null = null;
    let uischemas: JsonFormsUISchemaRegistryEntry[] | null = null;
    let uidata: Record<string, any> | null = null;

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

    const uischemasResponse = await client
      .fetch(`${pathLocation}${RESOURCE_UISCHEMAS_SUFFIX}`)
      .catch((e) => {
        throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_UISCHEMAS, e);
      });
    if (uischemasResponse.ok) {
      const uischemasJsonText = await uischemasResponse.text().catch((e) => {
        throw new AppException(AppErrorCode.INVALID_JSONFORMS_UISCHEMAS, e);
      });
      try {
        uischemas = parseAndTransformUISchemaRegistryEntries(uischemasJsonText);
      } catch (e) {
        throw new AppException(
          AppErrorCode.INVALID_JSONFORMS_UISCHEMAS,
          e as Error,
        );
      }
    }

    const uidataResponse = await client
      .fetch(`${pathLocation}${RESOURCE_UIDATA_SUFFIX}`)
      .catch((e) => {
        throw new AppException(AppErrorCode.RETRIEVE_JSONFORMS_UIDATA, e);
      });
    if (uidataResponse.ok) {
      uidata = await uidataResponse.json().catch((e) => {
        throw new AppException(AppErrorCode.INVALID_JSONFORMS_UIDATA, e);
      });
    }

    return { schema, schemaUrl, uischema, i18n, uischemas, uidata };
  }

  private async loadResourcesFromDeployment(
    client: RestClient,
    deploymentLocation: string,
    deploymentId: string,
    camundaFormConfig: CamundaFormConfig,
  ) {
    const resources = await getDeploymentResources(
      client,
      camundaFormConfig.url,
      deploymentId,
    );

    const formSchemaResource = resources.find(
      (i) => i.name === deploymentLocation + RESOURCE_SCHEMA_SUFFIX,
    );
    const formUiResource = resources.find(
      (i) => i.name === deploymentLocation + RESOURCE_UISCHEMA_SUFFIX,
    );
    const i18nResource = resources.find(
      (i) => i.name === deploymentLocation + RESOURCE_I18N_SUFFIX,
    );
    const uischemasResource = resources.find(
      (i) => i.name === deploymentLocation + RESOURCE_UISCHEMAS_SUFFIX,
    );
    const uidataResource = resources.find(
      (i) => i.name === deploymentLocation + RESOURCE_UIDATA_SUFFIX,
    );

    if (formSchemaResource == null) {
      throw new AppException(AppErrorCode.MISSING_JSONFORMS_SCHEMA);
    }

    if (formUiResource == null) {
      throw new AppException(AppErrorCode.MISSING_JSONFORMS_UISCHEMA);
    }

    const schema = (await getDeploymentResourceJsonData(
      client,
      camundaFormConfig.url,
      deploymentId,
      formSchemaResource.id,
      AppErrorCode.RETRIEVE_JSONFORMS_SCHEMA,
      AppErrorCode.INVALID_JSONFORMS_SCHEMA,
    )) as JsonSchema;

    const schemaUrl = `${camundaFormConfig.url}/deployment/${encodeURIComponent(
      deploymentId,
    )}/resources/${encodeURIComponent(formSchemaResource.id)}/data`;

    const uischema = (await getDeploymentResourceJsonData(
      client,
      camundaFormConfig.url,
      deploymentId,
      formUiResource.id,
      AppErrorCode.RETRIEVE_JSONFORMS_UISCHEMA,

      AppErrorCode.INVALID_JSONFORMS_UISCHEMA,
    )) as UISchemaElement;

    let i18n: Record<string, any> | null = null;
    if (i18nResource) {
      i18n = await getDeploymentResourceJsonData(
        client,
        camundaFormConfig.url,
        deploymentId,
        i18nResource.id,
        AppErrorCode.RETRIEVE_JSONFORMS_I18N,

        AppErrorCode.INVALID_JSONFORMS_I18N,
      );
    }

    let uischemas: JsonFormsUISchemaRegistryEntry[] | null = null;
    if (uischemasResource) {
      const uischemasJsonText = await getDeploymentResourceTextData(
        client,
        camundaFormConfig.url,
        deploymentId,
        uischemasResource.id,
        AppErrorCode.RETRIEVE_JSONFORMS_UISCHEMAS,
        AppErrorCode.INVALID_JSONFORMS_UISCHEMAS,
      );
      try {
        uischemas = parseAndTransformUISchemaRegistryEntries(uischemasJsonText);
      } catch (e) {
        throw new AppException(
          AppErrorCode.INVALID_JSONFORMS_UISCHEMAS,
          e as Error,
        );
      }
    }

    let uidata: Record<string, any> | null = null;
    if (uidataResource) {
      uidata = await getDeploymentResourceJsonData(
        client,
        camundaFormConfig.url,
        deploymentId,
        uidataResource.id,
        AppErrorCode.RETRIEVE_JSONFORMS_UIDATA,

        AppErrorCode.INVALID_JSONFORMS_UIDATA,
      );
    }

    return { schema, schemaUrl, uischema, i18n, uischemas, uidata };
  }
}
