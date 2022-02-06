<template>
  <component :is="compiled" v-if="compiled">
    <template
      v-if="elements !== undefined && elements.length == 1"
      v-slot:default
    >
      <slot></slot>
    </template>

    <template v-for="(element, index) in elements" v-slot:[`${index}`]>
      <slot :key="`${index}`" :name="`${index}`"></slot>
    </template>
  </component>
</template>

<script lang="ts">
import { compileToFunctions } from 'vue-template-compiler';
import Vue, { ComponentOptions } from 'vue';
import { defineComponent } from '@vue/composition-api';
import { CompType } from '../config/config';
import { UISchemaElement } from '@jsonforms/core';
import merge from 'lodash/merge';

const templateCompiler = defineComponent({
  name: 'template-compiler',

  inheritAttrs: false,

  props: {
    parent: {
      type: Object,
      default: undefined,
    },

    template: {
      type: String,
      default: '<div></div>',
    },

    elements: {
      type: Array as CompType<UISchemaElement[], ArrayConstructor>,
      default: undefined,
    },
  },

  data() {
    return {
      isCompiling: false,
      compiled: null,
    };
  },

  computed: {
    componentProps() {
      const data = [
        this.parentData,
        this.parentProps,
        this.parentComponent._provided,
      ];
      return {
        filters: this.parentFilters,
        components: this.parentComponents,
        computed: this.parentComputed,
        methods: this.parentMethods,
        data: () => merge({}, ...data),
      };
    },

    parentComponent(): Vue {
      return this.parent || this.$parent;
    },

    parentData() {
      return (this.parentComponent as Vue).$data || {};
    },

    parentProps() {
      return (this.parentComponent as Vue).$props || {};
    },

    parentOptions(): ComponentOptions<Vue> {
      return (this.parentComponent as Vue).$options || {};
    },

    parentComputed() {
      return (this.parentOptions as ComponentOptions<Vue>).computed || {};
    },

    parentComponents() {
      return (this.parentOptions as ComponentOptions<Vue>).components || {};
    },

    parentMethods() {
      return (this.parentOptions as ComponentOptions<Vue>).methods || {};
    },

    parentFilters() {
      return (this.parentOptions as ComponentOptions<Vue>).filters || {};
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
