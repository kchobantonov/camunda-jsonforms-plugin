import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { getTaskFormVariables } from '../camunda';
import { expect, it, vi } from 'vitest';
vi.mock('@chobantonov/jsonforms-vuetify-renderers', () => ({
  parseAndTransformUISchemaRegistryEntries: vi.fn(),
}));
vi.mock('../camunda', () => ({
  getTaskForm: vi.fn(async () => ({ key: 'form' })),
  getTaskFormVariables: vi.fn(async () => ({
    tags: {
      type: 'Object',
      value: '["rush"]',
      valueInfo: { serializationDataFormat: 'application/json' },
    },
    count: { type: 'Integer', value: 0 },
  })),
}));
import { CamundaFormApi } from '../api';
const schema = {
  properties: {
    tags: {
      type: 'array',
      items: { type: 'string' },
      'x-camunda': { type: 'Object' },
    },
    count: { type: 'integer' },
  },
} as any;
const context = {
  camundaFormConfig: { url: '/engine-rest', taskId: 'task' },
} as any;

it.each([
  'submit',
  'complete',
  'resolve',
  'submit-without-data',
  'complete-without-data',
  'resolve-without-data',
  'error',
  'escalation',
])('encodes button arrays on camunda:%s', async (action) => {
  const fetch = vi.fn(async () => ({ ok: true }));
  await new CamundaFormApi().submitForm(
    { fetch } as any,
    schema,
    { tags: ['data'], count: 0 },
    context,
    `camunda:${action}` as any,
    { variables: { tags: { value: ['button'] } } },
  );
  const payload = JSON.parse((fetch.mock.calls as any)[0][1].body);
  expect(payload.variables.tags).toEqual({
    type: 'Object',
    value: '["button"]',
    valueInfo: {
      objectTypeName: 'java.util.ArrayList<java.lang.String>',
      serializationDataFormat: 'application/json',
    },
  });
  expect(payload.variables.count?.value).toBe(
    ['submit', 'complete', 'resolve'].includes(action) ? 0 : undefined,
  );
});

it('loads JSON serialized lists and zero without mutating the REST descriptors', async () => {
  const api = new CamundaFormApi();
  vi.spyOn(api as any, 'loadResources').mockResolvedValue({
    schema,
    uischema: {},
    uidata: {},
  });
  const result = await api.loadForm({} as any, context.camundaFormConfig);
  expect(result.data).toEqual({ tags: ['rush'], count: 0 });
  expect(result.variables.tags.value).toBe('["rush"]');
});

it('rejects an ambiguous Object annotation while loading the form', async () => {
  const api = new CamundaFormApi();
  vi.spyOn(api as any, 'loadResources').mockResolvedValue({
    schema: {
      properties: {
        tags: {
          type: 'array',
          items: { type: 'object' },
          'x-camunda': { type: 'Object' },
        },
      },
    },
  });
  await expect(
    api.loadForm({} as any, context.camundaFormConfig),
  ).rejects.toThrow('objectTypeName');
});

const conformance = JSON.parse(
  readFileSync(
    resolve(__dirname, '../../../spec/variable-mapping.json'),
    'utf8',
  ),
);

it.each(conformance.write)(
  '$name: submits the complete object-root form as named variables',
  async (test) => {
    for (const action of ['camunda:submit', 'camunda:complete'] as const) {
      const fetch = vi.fn(async () => ({ ok: true }));
      await new CamundaFormApi().submitForm(
        { fetch } as any,
        test.schema,
        test.data,
        context,
        action,
      );
      const [url, options] = (fetch.mock.calls as any)[0];
      expect(url).toBe(
        `/engine-rest/task/task/${action === 'camunda:submit' ? 'submit-form' : 'complete'}`,
      );
      expect(JSON.parse(options.body).variables).toEqual(test.variables);
    }
  },
);

it.each(conformance.write)(
  '$name: loads the form variables under their schema property names',
  async (test) => {
    const api = new CamundaFormApi();
    vi.spyOn(api as any, 'loadResources').mockResolvedValue({
      schema: test.schema,
      uischema: {},
      uidata: {},
    });
    vi.mocked(getTaskFormVariables).mockResolvedValueOnce(test.variables);
    const result = await api.loadForm({} as any, context.camundaFormConfig);
    const expected = { ...test.data };
    for (const [name, variable] of Object.entries(test.variables)) {
      if ((variable as any).type === 'File') delete expected[name];
    }
    expect(result.data).toEqual(expected);
    expect(getTaskFormVariables).toHaveBeenLastCalledWith(
      {},
      '/engine-rest',
      'task',
      Object.keys(test.schema.properties),
    );
  },
);
