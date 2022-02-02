import { CamundaFormConfig } from './types';
import { SchemaObject, ValidateFunction } from 'ajv';
import { createAjv } from '@jsonforms/vue2-vuetify';

export const ajv = createAjv({ useDefaults: true });

export const CamundaFormConfigSchema: SchemaObject = {
  $schema: 'http://json-schema.org/draft-07/schema#',
  type: 'object',
  anyOf: [
    {
      $ref: '#/definitions/CamundaFormConfigProcessDefinitionId',
    },
    {
      $ref: '#/definitions/CamundaFormConfigProcessDefinitionKey',
    },
    {
      $ref: '#/definitions/CamundaFormConfigTaskId',
    },
  ],
  definitions: {
    CamundaFormConfigProcessDefinitionId: {
      type: 'object',
      properties: {
        camundaUrl: {
          type: 'string',
          format: 'uri-reference',
        },
        locale: {
          type: 'string',
        },
        style: {
          type: 'string',
        },
        processDefinitionId: {
          type: 'string',
        },
      },
      required: ['camundaUrl', 'processDefinitionId'],
    },
    CamundaFormConfigProcessDefinitionKey: {
      type: 'object',
      properties: {
        camundaUrl: {
          type: 'string',
          format: 'uri-reference',
        },
        locale: {
          type: 'string',
        },
        style: {
          type: 'string',
        },
        processDefinitionKey: {
          type: 'string',
        },
      },
      required: ['camundaUrl', 'processDefinitionKey'],
    },
    CamundaFormConfigTaskId: {
      type: 'object',
      properties: {
        camundaUrl: {
          type: 'string',
          format: 'uri-reference',
        },
        locale: {
          type: 'string',
        },
        style: {
          type: 'string',
        },
        taskId: {
          type: 'string',
        },
      },
      required: ['camundaUrl', 'processDefinitionKey'],
    },
  },
};

export type validate<T> = ((data: unknown) => data is T) &
  Pick<ValidateFunction, 'errors'>;

const isCamundaFormConfig = ajv.compile(
  CamundaFormConfigSchema
) as validate<CamundaFormConfig>;

export const validateCamundaFormConfig = (
  config: unknown
): CamundaFormConfig => {
  if (isCamundaFormConfig(config)) {
    return config;
  } else {
    throw new Error(
      ajv.errorsText(
        isCamundaFormConfig.errors?.filter((e: any) => e.keyword !== 'if'),
        { separator: '\n', dataVar: 'CamundaFormConfig' }
      ) +
        '\n\n' +
        'current input data:\n' +
        (config ? JSON.stringify(config) : config)
    );
  }
};
