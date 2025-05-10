import { AppErrorCode, AppException } from './errors';
import { RestClient } from './rest';
import {
  type ProcessDefinition,
  type Resource,
  ResponseException,
  type Task,
  type TaskForm,
  type VariableValue,
} from './types';

export const getTaskFormVariables = async (
  client: RestClient,
  url: string,
  taskId: string,
  variableNames: string[]
): Promise<Record<string, VariableValue>> => {
  const response = await client
    .fetch(
      `${url}/task/${encodeURIComponent(
        taskId
      )}/form-variables?deserializeValues=false&variableNames=${encodeURIComponent(
        variableNames.join(',')
      )}`
    )
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_TASK_FORM_VARIABLES, e);
    });

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_TASK_FORM_VARIABLES_RESPONSE,
      e
    );
  });
};

export const getProcessDefinitionFormVariablesById = async (
  client: RestClient,
  url: string,
  processDefinitionId: string,
  variableNames: string[]
): Promise<Record<string, VariableValue>> => {
  const response = await client
    .fetch(
      `${url}/process-definition/${encodeURIComponent(
        processDefinitionId
      )}/form-variables?deserializeValues=false&variableNames=${encodeURIComponent(
        variableNames.join(',')
      )}`
    )
    .catch((e) => {
      throw new AppException(
        AppErrorCode.RETRIEVE_PROCESS_DEFINITION_FORM_VARIABLES,
        e
      );
    });

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_PROCESS_DEFINITION_FORM_VARIABLES_RESPONSE,
      e
    );
  });
};

export const getProcessDefinitionFormVariablesByKey = async (
  client: RestClient,
  url: string,
  processDefinitionKey: string,
  variableNames: string[],
  tenantId?: string
): Promise<Record<string, VariableValue>> => {
  const fetchUrl =
    tenantId && tenantId.length > 0
      ? `${url}/process-definition/key/${encodeURIComponent(
          processDefinitionKey
        )}/tenant-id/${encodeURIComponent(
          tenantId
        )}/form-variables?deserializeValues=false&variableNames=${encodeURIComponent(
          variableNames.join(',')
        )}`
      : `${url}/process-definition/key/${encodeURIComponent(
          processDefinitionKey
        )}/form-variables?deserializeValues=false&variableNames=${encodeURIComponent(
          variableNames.join(',')
        )}`;
  const response = await client.fetch(fetchUrl).catch((e) => {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION_FORM_VARIABLES,
      e
    );
  });

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_PROCESS_DEFINITION_FORM_VARIABLES_RESPONSE,
      e
    );
  });
};

export const getDeploymentResources = async (
  client: RestClient,
  url: string,
  deploymentId: string
): Promise<Resource[]> => {
  const response = await client
    .fetch(`${url}/deployment/${encodeURIComponent(deploymentId)}/resources`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_DEPLOYMENT_RESOURCES, e);
    });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_DEPLOYMENT_RESOURCES,
      new ResponseException(response)
    );
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
      `${url}/deployment/${encodeURIComponent(
        deploymentId
      )}/resources/${encodeURIComponent(resourceId)}/data`
    )
    .catch((e) => {
      throw new AppException(resourceErrorCode, e);
    });
  if (!response.ok) {
    throw new AppException(resourceErrorCode, new ResponseException(response));
  }
  return response.json().catch((e) => {
    throw new AppException(jsonErrorCode, e);
  });
};

export const getDeploymentResourceTextData = async (
  client: RestClient,
  url: string,
  deploymentId: string,
  resourceId: string,
  resourceErrorCode: AppErrorCode,
  jsonErrorCode: AppErrorCode
): Promise<string> => {
  const response = await client
    .fetch(
      `${url}/deployment/${encodeURIComponent(
        deploymentId
      )}/resources/${encodeURIComponent(resourceId)}/data`
    )
    .catch((e) => {
      throw new AppException(resourceErrorCode, e);
    });
  if (!response.ok) {
    throw new AppException(resourceErrorCode, new ResponseException(response));
  }
  return response.text().catch((e) => {
    throw new AppException(jsonErrorCode, e);
  });
};

export const getTask = async (
  client: RestClient,
  url: string,
  taskId: string
): Promise<Task> => {
  const response = await client
    .fetch(`${url}/task/${encodeURIComponent(taskId)}`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_TASK, e);
    });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_TASK,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_TASK_RESPONSE, e);
  });
};

export const getTaskForm = async (
  client: RestClient,
  url: string,
  taskId: string
): Promise<TaskForm> => {
  const response = await client
    .fetch(`${url}/task/${encodeURIComponent(taskId)}/form`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_TASK_FORM, e);
    });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_TASK_FORM,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_TASK_FORM_RESPONSE, e);
  });
};

export const getProcessDefinition = async (
  client: RestClient,
  url: string,
  processDefinitionId: string
): Promise<ProcessDefinition> => {
  const response = await client
    .fetch(
      `${url}/process-definition/${encodeURIComponent(processDefinitionId)}`
    )
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION, e);
    });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE, e);
  });
};

export const getProcessDefinitionByKey = async (
  client: RestClient,
  url: string,
  processDefinitionKey: string,
  tenantId?: string
): Promise<ProcessDefinition> => {
  const fetchUrl = tenantId
    ? `${url}/process-definition/key/${encodeURIComponent(
        processDefinitionKey
      )}/tenant-id/${encodeURIComponent(tenantId)}`
    : `${url}/process-definition/key/${encodeURIComponent(
        processDefinitionKey
      )}`;
  const response = await client.fetch(fetchUrl).catch((e) => {
    throw new AppException(AppErrorCode.RETRIEVE_PROCESS_DEFINITION, e);
  });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_PROCESS_DEFINITION_RESPONSE, e);
  });
};

export const getProcessDefinitionStartFormById = async (
  client: RestClient,
  url: string,
  processDefinitionId: string
): Promise<TaskForm> => {
  const response = await client
    .fetch(
      `${url}/process-definition/${encodeURIComponent(
        processDefinitionId
      )}/startForm`
    )
    .catch((e) => {
      throw new AppException(
        AppErrorCode.RETRIEVE_PROCESS_DEFINITION_START_FORM,
        e
      );
    });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION_START_FORM,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE,
      e
    );
  });
};

export const getProcessDefinitionStartFormByKey = async (
  client: RestClient,
  url: string,
  processDefinitionKey: string,
  tenantId?: string
): Promise<TaskForm> => {
  const fetchUrl =
    tenantId && tenantId.length > 0
      ? `${url}/process-definition/key/${encodeURIComponent(
          processDefinitionKey
        )}/tenant-id/${encodeURIComponent(tenantId)}/startForm`
      : `${url}/process-definition/key/${encodeURIComponent(
          processDefinitionKey
        )}/startForm`;

  const response = await client.fetch(fetchUrl).catch((e) => {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION_START_FORM,
      e
    );
  });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION_START_FORM,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_PROCESS_DEFINITION_START_FORM_RESPONSE,
      e
    );
  });
};

export const getTaskDeployedForm = async (
  client: RestClient,
  url: string,
  taskId: string
): Promise<Record<string, any>> => {
  const response = await client
    .fetch(`${url}/task/${encodeURIComponent(taskId)}/deployed-form`)
    .catch((e) => {
      throw new AppException(AppErrorCode.RETRIEVE_TASK_DEPLOYED_FORM, e);
    });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_TASK_DEPLOYED_FORM,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(AppErrorCode.INVALID_TASK_DEPLOYED_FORM_RESPONSE, e);
  });
};

export const getProcessDefinitionDeployedStartFormById = async (
  client: RestClient,
  url: string,
  processDefinitionId: string
): Promise<Record<string, any>> => {
  const response = await client
    .fetch(
      `${url}/process-definition/${encodeURIComponent(
        processDefinitionId
      )}/deployed-start-form`
    )
    .catch((e) => {
      throw new AppException(
        AppErrorCode.RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM,
        e
      );
    });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_PROCESS_DEFINITION_DEPLOYED_START_FORM_RESPONSE,
      e
    );
  });
};

export const getProcessDefinitionDeployedStartFormByKey = async (
  client: RestClient,
  url: string,
  processDefinitionKey: string,
  tenantId?: string
): Promise<Record<string, any>> => {
  const fetchUrl = tenantId
    ? `${url}/process-definition/${encodeURIComponent(
        processDefinitionKey
      )}/tenant-id/${encodeURIComponent(tenantId)}/deployed-start-form`
    : `${url}/process-definition/${encodeURIComponent(
        processDefinitionKey
      )}/deployed-start-form`;

  const response = await client.fetch(fetchUrl).catch((e) => {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM,
      e
    );
  });
  if (!response.ok) {
    throw new AppException(
      AppErrorCode.RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM,
      new ResponseException(response)
    );
  }

  return response.json().catch((e) => {
    throw new AppException(
      AppErrorCode.INVALID_PROCESS_DEFINITION_DEPLOYED_START_FORM_RESPONSE,
      e
    );
  });
};
