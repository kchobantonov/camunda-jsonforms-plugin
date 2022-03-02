import { AsyncComponent, Component } from 'vue';

/**
 * Compatibility type as defineComponent of '@vue/composition-api' can't properly handle PropTypes.
 */
 export type CompType<_S,V> = V;

 export type Components = {
    [key: string]:
      | Component<any, any, any, any>
      | AsyncComponent<any, any, any, any>;
  };