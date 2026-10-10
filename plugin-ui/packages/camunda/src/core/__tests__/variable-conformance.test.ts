import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import Ajv from 'ajv';
import { describe, expect, it, vi } from 'vitest';
import {
  attachCamundaVariable,
  decodeCamundaVariable,
  resolveVariableDescriptor,
} from '../variables';
import type { VariableValue } from '../types';

const spec = JSON.parse(
  readFileSync(
    resolve(__dirname, '../../../spec/variable-mapping.json'),
    'utf8',
  ),
);

describe('shared variable conformance cases', () => {
  it.each(spec.write)(
    '$name: produces the REST payload consumed by the engine tests',
    (test) => {
      expect(test.schema.type).toBe('object');
      const variables: Record<string, VariableValue> = {};
      for (const [name, schema] of Object.entries(test.schema.properties)) {
        if (Object.hasOwn(test.data, name))
          attachCamundaVariable(
            variables,
            name,
            schema as any,
            test.data[name],
          );
      }
      expect(variables).toEqual(test.variables);
    },
  );

  it.each(spec.write)(
    '$name: restores the form value from serialized REST data',
    (test) => {
      for (const [name, variable] of Object.entries(test.variables)) {
        const decoded = decodeCamundaVariable(
          variable as VariableValue,
          test.schema.properties[name],
        );
        expect(decoded).toEqual(
          (variable as VariableValue).type === 'File'
            ? undefined
            : test.data[name],
        );
      }
    },
  );

  it.each(spec.reject)(
    '$name: form validation rejects the same invalid value as the backend',
    (test) => {
      expect(test.schema.type).toBe('object');
      const validate = new Ajv({ strict: false }).compile(test.schema);
      expect(validate(test.data)).toBe(false);
      expect(
        validate.errors?.some((error) => error.instancePath === test.path),
      ).toBe(true);
      const variables: Record<string, VariableValue> = {};
      for (const [name, schema] of Object.entries(test.schema.properties)) {
        attachCamundaVariable(variables, name, schema as any, test.data[name]);
      }
      expect(variables).toEqual(test.variables);
    },
  );

  it.each(spec.unsupportedFormats)(
    'refuses %s for Object writes and omits it on read',
    (format) => {
      const schema = {
        type: 'array',
        items: { type: 'string' },
        'x-camunda': {
          type: 'Object',
          valueInfo: { serializationDataFormat: format },
        },
      } as any;
      expect(() => resolveVariableDescriptor(schema)).toThrow(
        'application/json',
      );
      const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
      try {
        expect(
          decodeCamundaVariable(
            {
              type: 'Object',
              value: 'encoded content',
              valueInfo: {
                serializationDataFormat: format,
                objectTypeName: 'java.util.ArrayList',
              },
            },
            schema,
          ),
        ).toBeUndefined();
        expect(warn).toHaveBeenCalledOnce();
      } finally {
        warn.mockRestore();
      }
    },
  );
});
