<template>
  <v-flex v-if="layout.visible" v-bind="vuetifyProps('v-flex')">
    <div v-if="templateError !== null" class="error">
      Template Error: {{ templateError }}
    </div>

    <template-compiler
      :template="template"
      :parent="parentComponent"
      :elements="elements"
      :componentComputed="componentComputed()"
      :componentMethods="componentMethods()"
      :componentFilters="componentFilters()"
      :componentComponents="componentComponents()"
    >
      <template
        v-if="elements !== undefined && elements.length == 1"
        v-slot:default
      >
        <dispatch-renderer
          :key="`${layout.path}-${0}`"
          :schema="layout.schema"
          :uischema="elements[0]"
          :path="layout.path"
          :enabled="layout.enabled"
          :renderers="layout.renderers"
          :cells="layout.cells"
        />
      </template>
      <template v-for="(element, index) in elements" v-slot:[`${index}`]>
        <dispatch-renderer
          :key="`${layout.path}-${index}`"
          :schema="layout.schema"
          :uischema="element"
          :path="layout.path"
          :enabled="layout.enabled"
          :renderers="layout.renderers"
          :cells="layout.cells"
        />
      </template>
    </template-compiler>
  </v-flex>
</template>

<script lang="ts">
import {
  JsonFormsRendererRegistryEntry,
  JsonFormsSubStates,
  Layout,
  rankWith,
  UISchemaElement,
  uiTypeIs,
} from '@jsonforms/core';
import {
  DispatchRenderer,
  rendererProps,
  RendererProps,
  useJsonFormsLayout,
} from '@jsonforms/vue2';
import { useTranslator, useVuetifyLayout } from '@jsonforms/vue2-vuetify';
import { ErrorObject } from 'ajv';
import Vue, { defineComponent, inject, ref, unref } from 'vue';
import { ComputedOptions, MethodOptions } from 'vue/types/v3-component-options';
import {
  VAlert,
  VApp,
  VAppBar,
  VAppBarNavIcon,
  VAppBarTitle,
  VAutocomplete,
  VAvatar,
  VBadge,
  VBanner,
  VBottomNavigation,
  VBottomSheet,
  VBreadcrumbs,
  VBreadcrumbsDivider,
  VBreadcrumbsItem,
  VBtn,
  VBtnToggle,
  VCalendar,
  VCalendarCategory,
  VCalendarDaily,
  VCalendarMonthly,
  VCalendarWeekly,
  VCard,
  VCardActions,
  VCardSubtitle,
  VCardText,
  VCardTitle,
  VCarousel,
  VCarouselItem,
  VCarouselReverseTransition,
  VCarouselTransition,
  VCheckbox,
  VChip,
  VChipGroup,
  VCol,
  VColorPicker,
  VColorPickerCanvas,
  VColorPickerSwatches,
  VCombobox,
  VContainer,
  VContent,
  VCounter,
  VData,
  VDataFooter,
  VDataIterator,
  VDataTable,
  VDataTableHeader,
  VDatePicker,
  VDatePickerDateTable,
  VDatePickerHeader,
  VDatePickerMonthTable,
  VDatePickerTitle,
  VDatePickerYears,
  VDialog,
  VDialogBottomTransition,
  VDialogTopTransition,
  VDialogTransition,
  VDivider,
  VEditDialog,
  VExpandTransition,
  VExpandXTransition,
  VExpansionPanel,
  VExpansionPanelContent,
  VExpansionPanelHeader,
  VExpansionPanels,
  VFabTransition,
  VFadeTransition,
  VFileInput,
  VFlex,
  VFooter,
  VForm,
  VHover,
  VIcon,
  VImg,
  VInput,
  VItem,
  VItemGroup,
  VLabel,
  VLayout,
  VLazy,
  VList,
  VListGroup,
  VListItem,
  VListItemAction,
  VListItemActionText,
  VListItemAvatar,
  VListItemContent,
  VListItemGroup,
  VListItemIcon,
  VListItemSubtitle,
  VListItemTitle,
  VMain,
  VMenu,
  VMenuTransition,
  VMessages,
  VNavigationDrawer,
  VOtpInput,
  VOverflowBtn,
  VOverlay,
  VPagination,
  VParallax,
  VPicker,
  VProgressCircular,
  VProgressLinear,
  VRadio,
  VRadioGroup,
  VRangeSlider,
  VRating,
  VResponsive,
  VRow,
  VScaleTransition,
  VScrollXReverseTransition,
  VScrollXTransition,
  VScrollYReverseTransition,
  VScrollYTransition,
  VSelect,
  VSheet,
  VSimpleCheckbox,
  VSimpleTable,
  VSkeletonLoader,
  VSlideGroup,
  VSlideItem,
  VSlider,
  VSlideXReverseTransition,
  VSlideXTransition,
  VSlideYReverseTransition,
  VSlideYTransition,
  VSnackbar,
  VSpacer,
  VSparkline,
  VSpeedDial,
  VStepper,
  VStepperContent,
  VStepperHeader,
  VStepperItems,
  VStepperStep,
  VSubheader,
  VSwitch,
  VSystemBar,
  VTab,
  VTabItem,
  VTableOverflow,
  VTabReverseTransition,
  VTabs,
  VTabsItems,
  VTabsSlider,
  VTabTransition,
  VTextarea,
  VTextField,
  VThemeProvider,
  VTimeline,
  VTimelineItem,
  VTimePicker,
  VTimePickerClock,
  VTimePickerTitle,
  VToolbar,
  VToolbarItems,
  VToolbarTitle,
  VTooltip,
  VTreeview,
  VTreeviewNode,
  VVirtualScroll,
  VVirtualTable,
  VWindow,
  VWindowItem,
} from 'vuetify/lib';
import TemplateCompiler from '../components/TemplateCompiler.vue';
import { Components } from '../config/config';
import { FormContext } from '../core/types';

interface TemplateElement extends Layout {
  type: 'TemplateLayout';
  /**
   * The template string.
   */
  template: string;
}

// Register any components that are not yet provided by other jsonforms renderers and we want to use them in the template like VOtpInput
const templateLayoutRenderer = defineComponent({
  name: 'template-layout-renderer',
  components: {
    DispatchRenderer,
    VLabel,
    VBtn,
    VSpacer,
    TemplateCompiler,
    VFlex,
  },
  props: {
    ...rendererProps<Layout>(),
  },
  setup(props: RendererProps<Layout>) {
    const t = useTranslator();
    const layout = useVuetifyLayout(useJsonFormsLayout(props));

    const jsonforms = inject<JsonFormsSubStates>('jsonforms');
    if (!jsonforms) {
      throw new Error(
        "'jsonforms' couldn't be injected. Are you within JsonForms?"
      );
    }

    const formContext = inject<FormContext>('formContext');

    if (!formContext) {
      throw new Error(
        "'formContext' couldn't be injected. Are you within JsonForms?"
      );
    }

    const templateError = ref<string | null>(null);

    const scopeData = inject<any>('scopeData', null);

    return {
      ...layout,
      t,
      jsonforms,
      parentComponent: this,
      templateError,
      formContext,
      scopeData,
    };
  },
  errorCaptured: function (err: Error, _vm: Vue, info: string) {
    if (info == 'render') {
      this.templateError = err.message;
    }
  },
  computed: {
    dataProvider(): any {
      const scopeData: any = this.scopeData;
      return scopeData;
    },
    data(): any {
      const jsonforms: JsonFormsSubStates = this.jsonforms;
      return jsonforms.core?.data;
    },
    context(): FormContext {
      return unref(this.formContext);
    },
    errors(): ErrorObject[] | undefined {
      const jsonforms: JsonFormsSubStates = this.jsonforms;
      return jsonforms.core?.errors;
    },
    template(): string | undefined {
      return (this.layout.uischema as TemplateElement).template;
    },
    elements(): UISchemaElement[] {
      return (this.layout.uischema as TemplateElement).elements;
    },
  },
  methods: {
    componentComputed(): ComputedOptions {
      const defaultComputed = {} as ComputedOptions;
      const parentComponent = this as any;

      for (const key of ['data', 'errors', 'context', 'dataProvider']) {
        defaultComputed[key] = function () {
          return parentComponent?.[key];
        };
      }

      return inject<ComputedOptions>(
        'templateLayoutRendererComponentComputed',
        defaultComputed,
        false
      );
    },
    componentMethods() {
      const defaultMethods = {
        translate: this.translate.bind(this.parentComponent),
      } as MethodOptions;

      return inject<MethodOptions>(
        'templateLayoutRendererComponentMethods',
        defaultMethods,
        false
      );
    },
    componentFilters() {
      const defaultFilters = {
        translate: this.translate.bind(this.parentComponent),
      } as MethodOptions;

      return inject<MethodOptions>(
        'templateLayoutRendererComponentFilters',
        defaultFilters,
        false
      );
    },
    componentComponents() {
      const defaultComponents = {
        VAlert,
        VApp,
        VAppBar,
        VAppBarNavIcon,
        VAppBarTitle,
        VAutocomplete,
        VAvatar,
        VBadge,
        VBanner,
        VBottomNavigation,
        VBottomSheet,
        VBreadcrumbs,
        VBreadcrumbsDivider,
        VBreadcrumbsItem,
        VBtn,
        VBtnToggle,
        VCalendar,
        VCalendarCategory,
        VCalendarDaily,
        VCalendarMonthly,
        VCalendarWeekly,
        VCard,
        VCardActions,
        VCardSubtitle,
        VCardText,
        VCardTitle,
        VCarousel,
        VCarouselItem,
        VCarouselReverseTransition,
        VCarouselTransition,
        VCheckbox,
        VChip,
        VChipGroup,
        VCol,
        VColorPicker,
        VColorPickerCanvas,
        VColorPickerSwatches,
        VCombobox,
        VContainer,
        VContent,
        VCounter,
        VData,
        VDataFooter,
        VDataIterator,
        VDataTable,
        VDataTableHeader,
        VDatePicker,
        VDatePickerDateTable,
        VDatePickerHeader,
        VDatePickerMonthTable,
        VDatePickerTitle,
        VDatePickerYears,
        VDialog,
        VDialogBottomTransition,
        VDialogTopTransition,
        VDialogTransition,
        VDivider,
        VEditDialog,
        VExpandTransition,
        VExpandXTransition,
        VExpansionPanel,
        VExpansionPanelContent,
        VExpansionPanelHeader,
        VExpansionPanels,
        VFabTransition,
        VFadeTransition,
        VFileInput,
        VFlex,
        VFooter,
        VForm,
        VHover,
        VIcon,
        VImg,
        VInput,
        VItem,
        VItemGroup,
        VLabel,
        VLayout,
        VLazy,
        VList,
        VListGroup,
        VListItem,
        VListItemAction,
        VListItemActionText,
        VListItemAvatar,
        VListItemContent,
        VListItemGroup,
        VListItemIcon,
        VListItemSubtitle,
        VListItemTitle,
        VMain,
        VMenu,
        VMenuTransition,
        VMessages,
        VNavigationDrawer,
        VOtpInput,
        VOverflowBtn,
        VOverlay,
        VPagination,
        VParallax,
        VPicker,
        VProgressCircular,
        VProgressLinear,
        VRadio,
        VRadioGroup,
        VRangeSlider,
        VRating,
        VResponsive,
        VRow,
        VScaleTransition,
        VScrollXReverseTransition,
        VScrollXTransition,
        VScrollYReverseTransition,
        VScrollYTransition,
        VSelect,
        VSheet,
        VSimpleCheckbox,
        VSimpleTable,
        VSkeletonLoader,
        VSlideGroup,
        VSlideItem,
        VSlider,
        VSlideXReverseTransition,
        VSlideXTransition,
        VSlideYReverseTransition,
        VSlideYTransition,
        VSnackbar,
        VSpacer,
        VSparkline,
        VSpeedDial,
        VStepper,
        VStepperContent,
        VStepperHeader,
        VStepperItems,
        VStepperStep,
        VSubheader,
        VSwitch,
        VSystemBar,
        VTab,
        VTabItem,
        VTableOverflow,
        VTabReverseTransition,
        VTabs,
        VTabsItems,
        VTabsSlider,
        VTabTransition,
        VTextarea,
        VTextField,
        VThemeProvider,
        VTimeline,
        VTimelineItem,
        VTimePicker,
        VTimePickerClock,
        VTimePickerTitle,
        VToolbar,
        VToolbarItems,
        VToolbarTitle,
        VTooltip,
        VTreeview,
        VTreeviewNode,
        VVirtualScroll,
        VVirtualTable,
        VWindow,
        VWindowItem,
      } as Components;

      return inject<Components>(
        'templateLayoutRendererComponentComponents',
        defaultComponents,
        false
      );
    },
    translate(
      key: string,
      defaultMessage: string | undefined
    ): string | undefined {
      return this.t(key, defaultMessage ?? '');
    },
  },
  filters: {
    translate: function (value: any) {
      if (!value) return '';
      value = value.toString();
      return this.t(value, '');
    },
  },
});

export default templateLayoutRenderer;

export const entry: JsonFormsRendererRegistryEntry = {
  renderer: templateLayoutRenderer,
  tester: rankWith(1, uiTypeIs('TemplateLayout')),
};
</script>
