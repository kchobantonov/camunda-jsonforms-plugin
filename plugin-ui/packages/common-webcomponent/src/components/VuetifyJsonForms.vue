<template>
  <div>
    <custom-style type="text/css" id="vuetify-theme">
      {{ vuetifyThemeCss }}
    </custom-style>

    <custom-style type="text/css">
      {{ customStyle }}
    </custom-style>

    <v-app ref="root">
      <div v-if="error !== undefined">
        <v-container style="height: 400px">
          <v-row class="fill-height" align-content="center" justify="center">
            <v-col class="text-subtitle-1 text-center error" cols="12">
              {{ error }}
            </v-col>
          </v-row>
        </v-container>
      </div>
      <v-sheet v-else :dark="dark" tile>
        <resolved-json-forms
          :data="dataToUse"
          :schema="schemaToUse"
          :schemaUrl="schemaUrlToUse"
          :uischema="uischemaToUse"
          :renderers="renderers"
          :cells="cells"
          :config="configToUse"
          :readonly="readonlyToUse"
          :uischemas="uischemasToUse"
          :validationMode="validationModeToUse"
          :i18n="i18nToUse"
          :additionalErrors="additionalErrorsToUse"
          :actions="actionsToUse"
          @change="onChange"
        ></resolved-json-forms>
      </v-sheet>
    </v-app>
  </div>
</template>

<script lang="ts">
import {
  JsonFormsI18nState,
  JsonFormsUISchemaRegistryEntry,
  NOT_APPLICABLE,
  Translator,
  UISchemaElement,
  UISchemaTester,
  ValidationMode,
} from '@jsonforms/core';
import { JsonFormsChangeEvent } from '@jsonforms/vue2';
import {
  ActionEvent,
  Actions,
  AsyncFunction,
  commonRenderers,
  createTranslator,
  FormContext,
  ResolvedJsonForms,
} from '@kchobantonov/common-jsonforms';
import { ErrorObject } from 'ajv';
import { isArray, isPlainObject, merge } from 'lodash';
import get from 'lodash/get';
import Vue, { defineComponent, PropType, Ref, ref, toRef } from 'vue';
import LoadScript from 'vue-plugin-load-script';
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
import { VuetifyPreset } from 'vuetify/types/services/presets';
import { isActionsParams, validateActions } from '../core';
import vuetify, { preset as defaultPreset } from '../plugins/vuetify';

Vue.use(LoadScript);

Vue.config.productionTip = false;

const CustomStyle = defineComponent({
  name: 'custom-style',
  render(createElement) {
    return createElement('style', this.$slots.default);
  },
});

const transformUISchemas = (
  uischemas?: string
): JsonFormsUISchemaRegistryEntry[] => {
  const uischemasMap: {
    tester: string;
    uischema: UISchemaElement;
  }[] = typeof uischemas == 'string' ? JSON.parse(uischemas) : [];

  return uischemasMap
    .map((elem, index) => {
      if (elem.tester) {
        const action: UISchemaTester = (jsonSchema, schemaPath, path) => {
          try {
            const tester = new Function(
              'jsonSchema, schemaPath, path',
              `const NOT_APPLICABLE = -1; const tester = ${elem.tester}; return tester(jsonSchema, schemaPath, path);`
            );
            const result = tester(jsonSchema, schemaPath, path);
            if (typeof result !== 'number') {
              console.error(
                `Error at uischema tester[${index}]: invalid result type, expected number but got ${typeof result}`
              );
            }
            return typeof result === 'number' ? result : NOT_APPLICABLE;
          } catch (e) {
            console.error(`Error at uischema tester[${index}]: ${e}`);
            return NOT_APPLICABLE;
          }
        };
        return {
          tester: action,
          uischema: elem.uischema,
        };
      }
      return null;
    })
    .filter((x) => !!x) as JsonFormsUISchemaRegistryEntry[];
};

// eslint-disable-next-line @typescript-eslint/ban-types
const transformActions = (actionsString?: string): Record<string, Function> => {
  const actionsData: {
    [id: string]: string;
  } =
    typeof actionsString == 'string'
      ? validateActions(JSON.parse(actionsString))
      : {};
  const actions: Actions = {};

  Object.keys(actionsData).forEach((key) => {
    const action = (event: ActionEvent) => {
      try {
        const fn = AsyncFunction(
          'event',
          `const fn = ${actionsData[key]};\n return await fn(event);`
        );
        fn(event);
      } catch (e) {
        console.log(`Error at action[${key}]: ${e}`);
      }
    };
    actions[key] = action;
  });

  return actions;
};
const vuetifyFormWc = defineComponent({
  vuetify,
  components: {
    ResolvedJsonForms,
    VApp,
    VSheet,
    CustomStyle,
  },
  emits: ['change'],
  props: {
    data: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const data = typeof value == 'string' ? JSON.parse(value) : value;

          return data !== undefined && data !== null;
        } catch (e) {
          return false;
        }
      },
    },
    schema: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const schema = typeof value == 'string' ? JSON.parse(value) : value;

          return schema !== undefined && schema !== null;
        } catch (e) {
          return false;
        }
      },
    },
    schemaUrl: {
      required: false,
      type: String,
      default: undefined,
    },
    uischema: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const uischema = typeof value == 'string' ? JSON.parse(value) : value;

          return uischema !== undefined && uischema !== null;
        } catch (e) {
          return false;
        }
      },
    },
    config: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const config = typeof value == 'string' ? JSON.parse(value) : value;

          return config !== undefined && config !== null;
        } catch (e) {
          return false;
        }
      },
    },
    readonly: {
      required: false,
      type: String,
      default: 'false',
    },
    uischemas: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const uischemas =
            typeof value == 'string' ? JSON.parse(value) : value;

          return (
            uischemas !== undefined && uischemas !== null && isArray(uischemas)
          );
        } catch (e) {
          return false;
        }
      },
    },
    validationMode: {
      required: false,
      type: [String] as PropType<ValidationMode>,
      default: 'ValidateAndShow',
      validator: function (value) {
        return (
          value === 'ValidateAndShow' ||
          value === 'ValidateAndHide' ||
          value == 'NoValidation'
        );
      },
    },
    locale: {
      required: false,
      type: String,
      default: 'en',
    },
    customStyle: {
      required: false,
      type: String,
      default: '.v-application--wrap { min-height: 0px; }',
    },
    translations: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const translations =
            typeof value == 'string' ? JSON.parse(value) : value;

          return translations !== null;
        } catch (e) {
          return false;
        }
      },
    },
    additionalErrors: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const additionalErrors =
            typeof value == 'string' ? JSON.parse(value) : value;

          return (
            additionalErrors !== undefined &&
            additionalErrors !== null &&
            isArray(additionalErrors)
          );
        } catch (e) {
          return false;
        }
      },
    },
    defaultPreset: {
      required: false,
      type: String,
      validator: function (value) {
        try {
          const preset = typeof value == 'string' ? JSON.parse(value) : value;

          return preset !== undefined && preset !== null;
        } catch (e) {
          return false;
        }
      },
    },
    uidata: {
      required: false,
      type: String,
      default: () => {
        return {};
      },
      validator: function (value) {
        try {
          const data = typeof value == 'string' ? JSON.parse(value) : value;

          return data !== undefined && data !== null;
        } catch (e) {
          return false;
        }
      },
    },
    actions: {
      required: false,
      type: String,
      default: () => {
        return {};
      },
      validator: function (value) {
        const actions = typeof value == 'string' ? JSON.parse(value) : {};

        return isActionsParams(actions);
      },
    },
  },
  setup(props) {
    let error: any = undefined;

    let dataToUse: any = undefined;
    let schemaToUse: Record<string, any> | undefined = undefined;
    let schemaUrlToUse: string | undefined = undefined;
    let uischemaToUse: UISchemaElement | undefined = undefined;
    let configToUse: Record<string, any> | undefined = undefined;
    let readonlyToUse = false;
    let uischemasToUse: JsonFormsUISchemaRegistryEntry[] = [];
    let validationModeToUse: ValidationMode = 'ValidateAndShow';
    let i18nToUse: JsonFormsI18nState | undefined = undefined;
    let additionalErrorsToUse: ErrorObject[] = [];
    let translationsToUse: Record<string, any> = {};
    let localeToUse = 'en';

    let dataDefaultPreset =
      typeof props.defaultPreset == 'string'
        ? merge({}, defaultPreset, JSON.parse(props.defaultPreset))
        : defaultPreset;
    // eslint-disable-next-line  @typescript-eslint/ban-types
    let actionsToUse: Record<string, Function> = {};
    let uidataToUse: Record<string, any> = {};
    try {
      dataToUse =
        typeof props.data == 'string' ? JSON.parse(props.data) : undefined;
      schemaToUse =
        typeof props.schema == 'string' ? JSON.parse(props.schema) : undefined;
      schemaUrlToUse =
        typeof props.schemaUrl == 'string' ? props.schemaUrl : undefined;
      uischemaToUse =
        typeof props.uischema == 'string'
          ? JSON.parse(props.uischema)
          : undefined;
      configToUse =
        typeof props.config == 'string' ? JSON.parse(props.config) : undefined;

      readonlyToUse = props.readonly == 'true';

      validationModeToUse =
        props.validationMode == 'ValidateAndShow' ||
        props.validationMode == 'ValidateAndHide' ||
        props.validationMode == 'NoValidation'
          ? props.validationMode
          : 'ValidateAndShow';

      translationsToUse =
        typeof props.translations == 'string'
          ? JSON.parse(props.translations)
          : undefined;

      localeToUse = props.locale ? props.locale : localeToUse;

      i18nToUse = {
        locale: localeToUse,
        translate: createTranslator(localeToUse, translationsToUse),
      };

      dataDefaultPreset =
        typeof props.defaultPreset == 'string'
          ? merge({}, defaultPreset, JSON.parse(props.defaultPreset))
          : defaultPreset;

      uischemasToUse = transformUISchemas(props.uischemas);

      additionalErrorsToUse =
        typeof props.additionalErrors == 'string'
          ? JSON.parse(props.additionalErrors)
          : undefined;

      actionsToUse = transformActions(props.actions);
      uidataToUse =
        typeof props.uidata == 'string' ? JSON.parse(props.uidata) : {};
    } catch (e) {
      error = `Config error: ${e}`;
    }
    let context: Ref<FormContext & { uidata: Record<string, any> }> = ref({
      uidata: uidataToUse,
    });

    const vuetifyLocale = 'en';

    return {
      vuetifyLocale,
      error,
      renderers: Object.freeze(commonRenderers),
      cells: Object.freeze(commonRenderers),

      dataToUse,
      schemaToUse,
      schemaUrlToUse,
      uischemaToUse,
      configToUse,
      readonlyToUse,
      uischemasToUse,
      validationModeToUse,
      i18nToUse,
      translationsToUse,
      localeToUse,
      additionalErrorsToUse,
      dataDefaultPreset,
      vuetifyTheme: ref<{ generatedStyles: string }>(
        vuetify.framework.theme as any
      ),
      actionsToUse,
      uidataToUse,
      context,
    };
  },
  provide() {
    return {
      // demo how we can extend the template layout components that we can use.
      templateLayoutRendererComponentComponents: {
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
      },
      templateLayoutRendererContext: this.context || {},
      formContext: toRef(this, 'context'),
    };
  },
  watch: {
    data: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          const data = typeof value == 'string' ? JSON.parse(value) : undefined;
          this.dataToUse = data;
          this.$forceUpdate();
        }
      },
    },
    schema: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          const schema =
            typeof value == 'string' ? JSON.parse(value) : undefined;
          this.schemaToUse = schema;
          this.$forceUpdate();
        }
      },
    },
    schemaUrl: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          const schemaUrl = typeof value == 'string' ? value : undefined;
          this.schemaUrlToUse = schemaUrl;
          this.$forceUpdate();
        }
      },
    },
    uischema: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          const uischema =
            typeof value == 'string' ? JSON.parse(value) : undefined;
          this.uischemaToUse = uischema;
          this.$forceUpdate();
        }
      },
    },
    config: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          const config =
            typeof value == 'string' ? JSON.parse(value) : undefined;
          this.configToUse = config;
          this.$forceUpdate();
        }
      },
    },
    readonly: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.readonlyToUse = value == 'true';
          this.$forceUpdate();
        }
      },
    },
    uischemas: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.uischemasToUse = transformUISchemas(value);
          this.$forceUpdate();
        }
      },
    },
    validationMode: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.validationModeToUse =
            value == 'ValidateAndShow' ||
            value == 'ValidateAndHide' ||
            value == 'NoValidation'
              ? value
              : 'ValidateAndShow';
          this.$forceUpdate();
        }
      },
    },
    locale: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.localeToUse = value ? value : 'en';
          this.i18nToUse = {
            locale: this.localeToUse,
            translate: createTranslator(
              this.localeToUse,
              this.translationsToUse
            ) as Translator,
          };
          this.setVuetifyLocale(this.localeToUse);
          this.$forceUpdate();
        }
      },
    },
    translations: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.translationsToUse =
            typeof value == 'string' ? JSON.parse(value) : {};

          this.i18nToUse = {
            locale: this.localeToUse,
            translate: createTranslator(
              this.localeToUse,
              this.translationsToUse
            ) as Translator,
          };
          this.$forceUpdate();
        }
      },
    },
    additionalErrors: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.additionalErrorsToUse =
            typeof value == 'string' ? JSON.parse(value) : [];
          this.$forceUpdate();
        }
      },
    },
    defaultPreset: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.dataDefaultPreset =
            typeof value == 'string'
              ? merge({}, defaultPreset, JSON.parse(value))
              : undefined;

          this.applyTheme();
          this.$forceUpdate();
        }
      },
    },
    actions: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.actionsToUse = transformActions(value);
          this.$forceUpdate();
        }
      },
    },
    uidata: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.uidataToUse = typeof value == 'string' ? JSON.parse(value) : {};
          this.context.value.uidata = this.uidataToUse;
          this.$forceUpdate();
        }
      },
    },
  },
  async mounted() {
    this.applyTheme();
    this.vuetifyLocale = this.$vuetify.lang.current;
    this.setVuetifyLocale(this.localeToUse);

    // include the fonts outside the webcomponent for now - https://github.com/google/material-design-icons/issues/1165
    if (this.$el.getRootNode() instanceof ShadowRoot) {
      let el = document.querySelector(
        'style[id="vuetify-json-forms-materialdesignicons"]'
      );
      if (!el) {
        el = document.createElement('style');
        el.id = 'vuetify-json-forms-materialdesignicons';

        const root = this.$el.getRootNode();
        if (root.hasChildNodes()) {
          let children = root.childNodes;
          for (const node of children) {
            if (
              node.nodeName.toLowerCase() === 'style' &&
              node.textContent?.startsWith(
                '@font-face{font-family:Material Design Icons;'
              )
            ) {
              el.textContent = node.textContent;
              break;
            }
          }
        }
        document.head.appendChild(el);
      }
    }
  },
  computed: {
    dark() {
      return this.dataDefaultPreset?.theme?.dark || false;
    },
    vuetifyThemeCss() {
      return this.vuetifyTheme?.generatedStyles;
    },
  },
  methods: {
    applyTheme(): void {
      let preset: Partial<VuetifyPreset> | null = null;
      if (this.uischemaToUse?.options) {
        preset = this.vuetifyProps(
          this.uischemaToUse.options,
          'preset'
        ) as Partial<VuetifyPreset>;
      }
      // apply any themes
      this.$vuetify.theme = merge(
        this.$vuetify.theme,
        preset && preset.theme
          ? preset.theme
          : this.dataDefaultPreset?.theme || {}
      );

      this.$vuetify.icons = merge(
        this.$vuetify.icons,
        preset && preset.icons
          ? preset.icons
          : this.dataDefaultPreset?.icons || {}
      );
    },
    onChange(event: JsonFormsChangeEvent): void {
      this.$emit('change', event);
    },
    vuetifyProps(
      options: Record<string, any>,
      path: string
    ): Record<string, any> {
      const props = get(options?.vuetify, path);

      return props && isPlainObject(props) ? props : {};
    },
    setVuetifyLocale(locale: string): void {
      // if vuetify supports that locale then change it
      if (this.$vuetify.lang.locales[locale]) {
        this.$vuetify.lang.current = locale;
      } else {
        this.$vuetify.lang.current = this.vuetifyLocale || 'en';
      }
    },
  },
});

export default vuetifyFormWc;
</script>

<style scoped>
@import url('//fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900');
@import '~@mdi/font/css/materialdesignicons.min.css';
@import '~vuetify/dist/vuetify.min.css';
</style>
