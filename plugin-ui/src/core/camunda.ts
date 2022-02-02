import { RestClient } from './rest';
import { AppErrorCode, AppException } from './errors';
import { ProcessDefinition, Resource, Task, VariableValue } from './types';

export const getVariables = async (
  client: RestClient,
  camundaUrl: string,
  taskId: string,
  variableNames: string[]
): Promise<Record<string, VariableValue>> => {
  const response = await client
    .fetch(
      `${camundaUrl}/task/${taskId}/form-variables?deserializeValues=false&variableNames=${variableNames.join(
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
  camundaUrl: string,
  deploymentId: string
): Promise<Resource[]> => {
  const response = await client
    .fetch(`${camundaUrl}/deployment/${deploymentId}/resources`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_DEPLOYMENT_RESOURCES, e);
    });

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_DEPLOYMENT_RESOURCES_RESPONSE,
      e
    );
  });
};

export const getDeploymentResourceJsonData = async (
  client: RestClient,
  camundaUrl: string,
  deploymentId: string,
  resourceId: string,
  resourceErrorCode: AppErrorCode,
  jsonErrorCode: AppErrorCode
): Promise<Record<string, any>> => {
  const response = await client
    .fetch(
      `${camundaUrl}/deployment/${deploymentId}/resources/${resourceId}/data`
    )
    .catch((e) => {
      throw new AppException(resourceErrorCode, e);
    });
  return response.json().catch((e) => {
    throw new AppException(jsonErrorCode, e);
  });
};

export const getTask = async (
  client: RestClient,
  camundaUrl: string,
  taskId: string
): Promise<Task> => {
  const response = await client
    .fetch(`${camundaUrl}/task/${taskId}`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_TASK, e);
    });

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_TASK_RESPONSE, e);
  });
};

export const getProcessDefinition = async (
  client: RestClient,
  camundaUrl: string,
  processDefinitionId: string
): Promise<ProcessDefinition> => {
  const response = await client
    .fetch(`${camundaUrl}/process-definition/${processDefinitionId}`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION, e);
    });

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE, e);
  });
};

export const getProcessDefinitionByKey = async (
  client: RestClient,
  camundaUrl: string,
  processDefinitionKey: string
): Promise<ProcessDefinition> => {
  const response = await client
    .fetch(`${camundaUrl}/process-definition/key/${processDefinitionKey}`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION, e);
    });

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE, e);
  });
};

export const getProcessDefinitionStartForm = async (
  client: RestClient,
  camundaUrl: string,
  processDefinitionId: string
): Promise<{ key: string }> => {
  const response = await client
    .fetch(`${camundaUrl}/process-definition/${processDefinitionId}/startForm`)
    .catch((e) => {
      throw new AppException(
        AppErrorCode.RETRIEVE_PROCESS_DEFINITION_START_FORM,
        e
      );
    });

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE,
      e
    );
  });
};
