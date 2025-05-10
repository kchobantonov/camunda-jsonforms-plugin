import type { ButtonElement } from '@chobantonov/jsonforms-vuetify-renderers';
import {
  getAjv,
  getCells,
  getConfig,
  getData,
  getI18nKeyPrefixBySchema,
  getRenderers,
  getTranslator,
  hasShowRule,
  isInherentlyEnabled,
  isVisible,
  type JsonFormsState,
  type OwnPropsOfRenderer
} from '@jsonforms/core';
import { useControl, type RendererProps } from '@jsonforms/vue';
import { useStyles } from '@jsonforms/vue-vuetify';
import cloneDeep from 'lodash/cloneDeep';
import get from 'lodash/get';
import isPlainObject from 'lodash/isPlainObject';
import merge from 'lodash/merge';
import { computed } from 'vue';
import type { Action } from '../core';

export interface CamundaButtonElement extends ButtonElement {
  action: Action;
  errorCode?: string;
  errorMessage?: string;
  escalationCode?: string;
  variables?: Record<
    string,
    {
      type: string;
      value: any;
      valueInfo: Record<string, any>;
    }
  >;
}

export interface OwnPropsOfCamundaButton extends OwnPropsOfRenderer {
  uischema: CamundaButtonElement;
}

export const mapStateToCamundaButtonProps = (
  state: JsonFormsState,
  ownProps: OwnPropsOfCamundaButton,
) => {
  const rootData = getData(state);
  const { uischema } = ownProps;
  const visible: boolean =
    ownProps.visible === undefined || hasShowRule(uischema)
      ? isVisible(ownProps.uischema, rootData, ownProps.path!, getAjv(state))
      : ownProps.visible;
  const label = uischema.label;
  const icon = uischema.icon;
  const action = uischema.action;
  const color = uischema.color;
  const params = uischema.params;
  const t = getTranslator()(state);
  const i18nKeyPrefix = getI18nKeyPrefixBySchema(undefined, uischema);
  const i18nKey = i18nKeyPrefix ? `${i18nKeyPrefix}.label` : (label ?? '');
  const i18nText = t(i18nKey, label, { uischema });

  const errorCode = uischema.errorCode;
  const errorMessage = uischema.errorMessage;
  const escalationCode = uischema.escalationCode;
  const variables = uischema.variables;

  const config = getConfig(state);
  const enabled: boolean = isInherentlyEnabled(
    state,
    ownProps,
    uischema,
    undefined, // layouts have no associated schema
    rootData,
    config,
  );

  return {
    label: i18nText,
    icon,
    action,
    params,
    visible,
    enabled,
    color,
    config: getConfig(state),
    renderers: ownProps.renderers || getRenderers(state),
    cells: ownProps.cells || getCells(state),
    errorCode,
    errorMessage,
    escalationCode,
    variables
  };
};

export const useJsonFormsCamundaButton = (props: RendererProps<CamundaButtonElement>) => {
  const { control, ...other } = useControl(props, mapStateToCamundaButtonProps);
  return { button: control, ...other };
};

export const useCamundaButton = <I extends { button: any }>(input: I) => {
  const styles = useStyles(input.button.value.uischema);
  const appliedOptions = computed(() =>
    merge(
      {},
      cloneDeep(input.button.value.config),
      cloneDeep(input.button.value.uischema.options),
    ),
  );
  const vuetifyProps = (path: string) => {
    const props = get(appliedOptions.value?.vuetify, path);

    return props && isPlainObject(props) ? props : {};
  };
  return {
    ...input,
    appliedOptions,
    vuetifyProps,
    styles,
  };
};

