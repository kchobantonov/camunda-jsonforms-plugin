import type { CamundaFormConfig } from './types';
import type { SchemaObject, ValidateFunction } from 'ajv';
import { createAjv as createDefaultAjv } from '@chobantonov/jsonforms-vuetify-renderers';

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
        url: {
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
      required: ['url', 'processDefinitionId'],
    },
    CamundaFormConfigProcessDefinitionKey: {
      type: 'object',
      properties: {
        url: {
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
        tenantId: {
          type: 'string',
        },
      },
      required: ['url', 'processDefinitionKey'],
    },
    CamundaFormConfigTaskId: {
      type: 'object',
      properties: {
        url: {
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
      required: ['url', 'taskId'],
    },
  },
};

export type validate<T> = ((data: unknown) => data is T) &
  Pick<ValidateFunction, 'errors'>;

const ajv = createDefaultAjv();
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
