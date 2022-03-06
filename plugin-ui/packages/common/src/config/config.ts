import { AsyncComponent, Component } from 'vue';

export type Components = {
  [key: string]:
    | Component<any, any, any, any>
    | AsyncComponent<any, any, any, any>;
};
