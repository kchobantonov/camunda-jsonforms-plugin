import actions from './actions.js';
import config from './config.json';
import preset from './preset.json';
import style from './style.css?inline';

export const input = {
  actions: actions,
  config: config,
  preset: preset,
  style: style,
  processDefinitionKey: 'embeddedFormsQuickstart',
  processDefinitionId: undefined,
  taskId: undefined,
};
