import type { JsonSchema } from '@jsonforms/core';
import type { VariableValue } from './types';

/** The REST descriptor without its value, attached to a root schema property. */
export type XCamunda = Partial<Omit<VariableValue, 'value'>>;
type VariableSchema = JsonSchema & {
  'x-camunda'?: XCamunda;
  readOnly?: boolean;
};

const nonNullTypes = (schema: JsonSchema): string[] =>
  (Array.isArray(schema.type)
    ? schema.type
    : schema.type
      ? [schema.type]
      : []
  ).filter((type) => type !== 'null');

export const getCamundaType = (schema: JsonSchema): string => {
  const types = nonNullTypes(schema);
  if (!types.length && schema.type) return 'Null';
  if (types.length !== 1) return 'Json';
  switch (types[0]) {
    case 'string':
      return schema.format === 'binary' ? 'File' : 'String';
    case 'integer':
      return 'Integer';
    case 'number':
      return 'Double';
    case 'boolean':
      return 'Boolean';
    default:
      return 'Json';
  }
};

const boxedType = (schema: any): string | undefined => {
  if (
    !schema ||
    typeof schema !== 'object' ||
    Array.isArray(schema) ||
    schema.$ref
  )
    return;
  const types = nonNullTypes(schema);
  if (types.length !== 1) return;
  return (
    {
      string: 'java.lang.String',
      integer: 'java.lang.Integer',
      number: 'java.lang.Double',
      boolean: 'java.lang.Boolean',
    } as Record<string, string>
  )[types[0]];
};

export const resolveVariableDescriptor = (
  schema: VariableSchema,
  override: XCamunda = {},
): Omit<VariableValue, 'value'> => {
  const annotation = schema['x-camunda'] ?? {};
  const type = override.type ?? annotation.type ?? getCamundaType(schema);
  const valueInfo = { ...annotation.valueInfo, ...override.valueInfo };
  if (type === 'Object') {
    if (
      valueInfo.serializationDataFormat &&
      valueInfo.serializationDataFormat !== 'application/json'
    ) {
      throw new Error(
        'x-camunda Object variables require application/json serialization',
      );
    }
    valueInfo.serializationDataFormat = 'application/json';
    if (!valueInfo.objectTypeName) {
      const types = nonNullTypes(schema);
      if (types.length === 1 && types[0] === 'array') {
        const item = boxedType(schema.items);
        if (item)
          valueInfo.objectTypeName = `java.util.${schema.uniqueItems ? 'LinkedHashSet' : 'ArrayList'}<${item}>`;
      } else if (
        types.length === 1 &&
        types[0] === 'object' &&
        !schema.properties
      ) {
        const item = boxedType(schema.additionalProperties);
        if (item)
          valueInfo.objectTypeName = `java.util.LinkedHashMap<java.lang.String,${item}>`;
      }
    }
    if (
      typeof valueInfo.objectTypeName !== 'string' ||
      !valueInfo.objectTypeName.trim()
    ) {
      throw new Error(
        'x-camunda Object requires valueInfo.objectTypeName for this schema',
      );
    }
    if (valueInfo.objectTypeName.includes('[]')) {
      throw new Error(
        'Use a JVM array descriptor (for example [Ljava.lang.String;) instead of Java [] syntax',
      );
    }
  }
  return { type, valueInfo };
};

export const encodeCamundaVariable = (
  name: string,
  schema: JsonSchema,
  data: any,
  override: XCamunda = {},
): VariableValue => {
  const descriptor = resolveVariableDescriptor(schema, override);
  let value = data;
  if (value != null) {
    if (descriptor.type === 'Json' || descriptor.type === 'Object') {
      value = JSON.stringify(value);
    } else if (descriptor.type === 'File') {
      if (
        typeof value !== 'string' ||
        !value.startsWith('data:') ||
        !value.includes(';base64,')
      ) {
        throw new Error(`File variable ${name} requires a base64 data URL`);
      }
      const index = value.indexOf(';base64,');
      const header = value.substring(5, index);
      const filenameIndex = header.indexOf(';filename=');
      descriptor.valueInfo = {
        filename:
          filenameIndex < 0
            ? name
            : decodeURIComponent(header.substring(filenameIndex + 10)),
        mimeType:
          filenameIndex < 0 ? header : header.substring(0, filenameIndex),
        ...descriptor.valueInfo,
      };
      value = value.substring(index + 8);
    }
  }
  return { ...descriptor, value };
};

export const attachCamundaVariable = (
  variables: Record<string, VariableValue>,
  name: string,
  schema: VariableSchema,
  data: any,
): void => {
  if (!schema.readOnly && data !== undefined)
    variables[name] = encodeCamundaVariable(name, schema, data);
};

export const encodeButtonVariables = (
  schema: JsonSchema,
  variables: Record<string, Partial<VariableValue>>,
): Record<string, Partial<VariableValue>> =>
  Object.fromEntries(
    Object.entries(variables).map(([name, variable]) => {
      const property = schema?.properties?.[name];
      if (!property && !variable.type) return [name, variable];
      // Existing buttons may already contain serialized REST values. Preserve those.
      const descriptor = resolveVariableDescriptor(property ?? {}, variable);
      if (
        typeof variable.value === 'string' &&
        (['Json', 'Object'].includes(descriptor.type) ||
          (descriptor.type === 'File' && !variable.value.startsWith('data:')))
      ) {
        return [name, { ...descriptor, value: variable.value }];
      }
      return [
        name,
        encodeCamundaVariable(name, property ?? {}, variable.value, variable),
      ];
    }),
  );

export const schemaAdmitsNull = (schema: any): boolean => {
  if (!schema || typeof schema !== 'object') return schema === true;
  return (
    schema.type === 'null' ||
    (Array.isArray(schema.type) && schema.type.includes('null')) ||
    schema.enum?.includes(null) ||
    schema.const === null ||
    (schema.oneOf ?? []).some(schemaAdmitsNull) ||
    (schema.anyOf ?? []).some(schemaAdmitsNull) ||
    false
  );
};

export const decodeCamundaVariable = (
  variable: VariableValue,
  schema: JsonSchema,
): any => {
  if (variable.type === 'File') return undefined;
  let value = variable.type === 'Null' ? null : variable.value;
  if (
    variable.type === 'Object' &&
    value != null &&
    variable.valueInfo?.serializationDataFormat !== 'application/json'
  ) {
    console.warn(
      'Omitting Object variable with unsupported serialization format',
    );
    return undefined;
  }
  if (value != null && (variable.type === 'Json' || variable.type === 'Object'))
    value = JSON.parse(value);
  return value === null && !schemaAdmitsNull(schema) ? undefined : value;
};
