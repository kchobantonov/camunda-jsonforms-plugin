import { RestClient } from '@kchobantonov/common-jsonforms';
import { AppErrorCode, AppException } from './errors';
import { ProcessDefinition, Resource, Task, VariableValue } from './types';

export const getVariables = async (
  client: RestClient,
  url: string,
  taskId: string,
  variableNames: string[]
): Promise<Record<string, VariableValue>> => {
  const response = await client
    .fetch(
      `${url}/task/${taskId}/form-variables?deserializeValues=false&variableNames=${variableNames.join(
        ','
      )}`
    )
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_FORM_VARIABLES, e);
    });

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_FORM_VARIABLES_RESPONSE, e);
  });
};

export const getDeploymentResources = async (
  client: RestClient,
  url: string,
  deploymentId: string
): Promise<Resource[]> => {
  const response = await client
    .fetch(`${url}/deployment/${deploymentId}/resources`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_DEPLOYMENT_RESOURCES, e);
    });
  if (!response.ok) {
    throw new AppException(AppErrorCode.RETRIEVE_DEPLOYMENT_RESOURCES);
  }

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_DEPLOYMENT_RESOURCES_RESPONSE,
      e
    );
  });
};

export const getDeploymentResourceJsonData = async (
  client: RestClient,
  url: string,
  deploymentId: string,
  resourceId: string,
  resourceErrorCode: AppErrorCode,
  jsonErrorCode: AppErrorCode
): Promise<Record<string, any>> => {
  const response = await client
    .fetch(
      `${url}/deployment/${deploymentId}/resources/${resourceId}/data`
    )
    .catch((e) => {
      throw new AppException(resourceErrorCode, e);
    });
  if (!response.ok) {
    throw new AppException(resourceErrorCode);
  }
  return response.json().catch((e) => {
    throw new AppException(jsonErrorCode, e);
  });
};

export const getTask = async (
  client: RestClient,
  url: string,
  taskId: string
): Promise<Task> => {
  const response = await client
    .fetch(`${url}/task/${taskId}`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_TASK, e);
    });
  if (!response.ok) {
    throw new AppException(AppErrorCode.RETRIEVE_TASK);
  }

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_TASK_RESPONSE, e);
  });
};

export const getProcessDefinition = async (
  client: RestClient,
  url: string,
  processDefinitionId: string
): Promise<ProcessDefinition> => {
  const response = await client
    .fetch(`${url}/process-definition/${processDefinitionId}`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION, e);
    });
  if (!response.ok) {
    throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION);
  }

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE, e);
  });
};

export const getProcessDefinitionByKey = async (
  client: RestClient,
  url: string,
  processDefinitionKey: string
): Promise<ProcessDefinition> => {
  const response = await client
    .fetch(`${url}/process-definition/key/${processDefinitionKey}`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION, e);
    });
  if (!response.ok) {
    throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION);
  }

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE, e);
  });
};

export const getProcessDefinitionStartForm = async (
  client: RestClient,
  url: string,
  processDefinitionId: string
): Promise<{ key: string }> => {
  const response = await client
    .fetch(`${url}/process-definition/${processDefinitionId}/startForm`)
    .catch((e) => {
      throw new AppException(
        AppErrorCode.RETRIEVE_PROCESS_DEFINITION_START_FORM,
        e
      );
    });
  if (!response.ok) {
    throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION_START_FORM);
  }

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE,
      e
    );
  });
};
