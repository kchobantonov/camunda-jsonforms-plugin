<template>
  <component :is="compiled" v-if="compiled">
    <template
      v-if="elements !== undefined && elements.length == 1"
      v-slot:default
    >
      <slot></slot>
    </template>

    <template v-for="(_element, index) in elements" v-slot:[`${index}`]>
      <div :key="`${index}`">
        <slot :name="`${index}`"></slot>
      </div>
    </template>
  </component>
</template>

<script lang="ts">
import { UISchemaElement } from '@jsonforms/core';
import merge from 'lodash/merge';
import Vue, { defineComponent, PropType } from 'vue';
import {
  CompiledResultFunctions,
  compileToFunctions,
} from 'vue-template-compiler';
import { ComputedOptions, MethodOptions } from 'vue/types/v3-component-options';
import { Components } from '../config/config';

const templateCompiler = defineComponent({
  name: 'template-compiler',

  inheritAttrs: false,

  props: {
    parent: {
      type: [Object] as PropType<Vue>,
    },

    template: {
      type: String,
      default: '<div></div>',
    },

    componentComputed: {
      type: [Object] as PropType<ComputedOptions>,
    },

    componentMethods: {
      type: [Object] as PropType<MethodOptions>,
    },

    componentFilters: {
      type: [Object] as PropType<MethodOptions>,
    },

    componentComponents: {
      type: [Object] as PropType<Components>,
    },

    elements: {
      type: [Array] as PropType<UISchemaElement[]>,
    },
  },

  data() {
    return {
      isCompiling: false,
      compiled: null as CompiledResultFunctions | null,
    };
  },

  computed: {
    componentProps() {
      const data = [
        this.parentData,
        this.parentProps,
        (this.parentComponent as any)._provided,
      ];
      const computed: any = this.componentComputed || {};
      const methods: any = this.componentMethods || {};
      const filters: any = this.componentFilters || {};
      const components: any = this.componentComponents || {};
      return {
        components: components,
        computed: computed,
        filters: filters,
        methods: methods,
        data: () => merge({}, ...data),
      };
    },

    parentComponent(): Vue {
      return (this.parent as Vue) || this.$parent;
    },

    parentData() {
      return (this.parentComponent as any as Vue).$data || {};
    },

    parentProps() {
      return (this.parentComponent as any as Vue).$props || {};
    },
  },

  beforeMount() {
    this.compile();
  },

  methods: {
    compile() {
      const component = compileToFunctions(this.template);
      this.compiled = merge(component, this.componentProps);
    },
  },
});

export default templateCompiler;
</script>
