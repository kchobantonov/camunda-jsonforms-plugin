import { describe, expect, it, vi } from 'vitest';
import {
  attachCamundaVariable,
  decodeCamundaVariable,
  encodeButtonVariables,
  encodeCamundaVariable,
  resolveVariableDescriptor,
} from '../variables';

const array = { type: 'array', items: { type: 'string' } } as const;
const list = { ...array, 'x-camunda': { type: 'Object' } };

describe('Camunda variable mapping', () => {
  it('keeps arrays as Spin JSON by default and opts into a Java ArrayList', () => {
    expect(encodeCamundaVariable('tags', array, ['a'])).toEqual({
      type: 'Json',
      value: '["a"]',
      valueInfo: {},
    });
    expect(encodeCamundaVariable('tags', list, ['a'])).toEqual({
      type: 'Object',
      value: '["a"]',
      valueInfo: {
        objectTypeName: 'java.util.ArrayList<java.lang.String>',
        serializationDataFormat: 'application/json',
      },
    });
  });
  it.each([
    ['integer', 'Integer'],
    ['number', 'Double'],
    ['boolean', 'Boolean'],
  ])('derives boxed %s items', (type, boxed) => {
    expect(
      resolveVariableDescriptor({ ...list, items: { type: type as any } })
        .valueInfo.objectTypeName,
    ).toBe(`java.util.ArrayList<java.lang.${boxed}>`);
  });
  it('supports sets, maps, and explicit list overrides for unique arrays', () => {
    expect(
      resolveVariableDescriptor({ ...list, uniqueItems: true }).valueInfo
        .objectTypeName,
    ).toBe('java.util.LinkedHashSet<java.lang.String>');
    expect(
      resolveVariableDescriptor({
        type: 'object',
        additionalProperties: { type: 'boolean' },
        'x-camunda': { type: 'Object' },
      }).valueInfo.objectTypeName,
    ).toBe('java.util.LinkedHashMap<java.lang.String,java.lang.Boolean>');
    expect(
      resolveVariableDescriptor({
        ...list,
        uniqueItems: true,
        'x-camunda': {
          type: 'Object',
          valueInfo: {
            objectTypeName: 'java.util.ArrayList<java.lang.String>',
          },
        },
      }).valueInfo.objectTypeName,
    ).toBe('java.util.ArrayList<java.lang.String>');
  });
  it('requires explicit names for DTOs and refuses unsupported serialization', () => {
    expect(() =>
      resolveVariableDescriptor({ ...list, items: { type: 'object' } }),
    ).toThrow('objectTypeName');
    expect(() =>
      resolveVariableDescriptor(list, {
        valueInfo: {
          serializationDataFormat: 'application/x-java-serialized-object',
        },
      }),
    ).toThrow('application/json');
    expect(() =>
      resolveVariableDescriptor(list, {
        valueInfo: { objectTypeName: 'java.lang.String[]' },
      }),
    ).toThrow('JVM');
    expect(
      resolveVariableDescriptor(list, {
        valueInfo: { objectTypeName: 'java.util.ArrayList<com.example.Order>' },
      }).valueInfo.objectTypeName,
    ).toContain('com.example.Order');
  });
  it('preserves typed null, falsy values, and omits read-only or undefined writes', () => {
    expect(encodeCamundaVariable('tags', list, null).value).toBeNull();
    expect(
      encodeCamundaVariable('name', { type: ['string', 'null'] }, null).type,
    ).toBe('String');
    for (const value of [0, false, ''])
      expect(encodeCamundaVariable('v', {}, value).value).toBe(
        JSON.stringify(value),
      );
    const variables = {};
    attachCamundaVariable(
      variables,
      'a',
      { type: 'string', readOnly: true },
      'hidden',
    );
    attachCamundaVariable(variables, 'b', array, undefined);
    expect(variables).toEqual({});
  });
  it('round-trips lists and preserves falsy primitive reads', () => {
    expect(
      decodeCamundaVariable(encodeCamundaVariable('tags', list, []), list),
    ).toEqual([]);
    for (const value of [false, 0, ''])
      expect(
        decodeCamundaVariable({ type: 'String', value, valueInfo: {} }, {}),
      ).toBe(value);
    expect(
      decodeCamundaVariable(
        { type: 'Null', value: null, valueInfo: {} },
        { type: 'string' },
      ),
    ).toBeUndefined();
    expect(
      decodeCamundaVariable(
        { type: 'Null', value: null, valueInfo: {} },
        { type: ['string', 'null'] },
      ),
    ).toBeNull();
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
    expect(
      decodeCamundaVariable(
        { type: 'Object', value: 'base64', valueInfo: {} },
        array,
      ),
    ).toBeUndefined();
    expect(warn).toHaveBeenCalledOnce();
    warn.mockRestore();
  });
  it('inherits button mappings, encodes arrays, and preserves existing serialized button values', () => {
    const schema = { properties: { tags: list } };
    expect(
      encodeButtonVariables(schema, { tags: { value: ['a'] } }).tags,
    ).toEqual(encodeCamundaVariable('tags', list, ['a']));
    expect(
      encodeButtonVariables(schema, { tags: { value: '["a"]' } }).tags.value,
    ).toBe('["a"]');
    expect(
      encodeButtonVariables(schema, { marker: { value: true } }).marker,
    ).toEqual({ value: true });
    expect(
      encodeButtonVariables(schema, { tags: { type: 'Json', value: [] } }).tags
        .type,
    ).toBe('Json');
  });
  it('encodes file data URLs while preserving existing REST button file values', () => {
    expect(
      encodeCamundaVariable(
        'file',
        { type: 'string', format: 'binary' },
        'data:text/plain;filename=test.txt;base64,YQ==',
      ),
    ).toEqual({
      type: 'File',
      value: 'YQ==',
      valueInfo: { filename: 'test.txt', mimeType: 'text/plain' },
    });
    expect(
      encodeButtonVariables(
        {},
        {
          file: {
            type: 'File',
            value: 'YQ==',
            valueInfo: { filename: 'test.txt' },
          },
        },
      ).file,
    ).toEqual({
      type: 'File',
      value: 'YQ==',
      valueInfo: { filename: 'test.txt' },
    });
  });
});
