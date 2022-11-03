<template>
  <div>
    <div v-if="loading">
      <v-container style="height: 400px">
        <v-row class="fill-height" align-content="center" justify="center">
          <v-col class="text-subtitle-1 text-center" cols="12">
            Loading...
          </v-col>
          <v-col cols="6">
            <v-progress-linear
              indeterminate
              rounded
              height="6"
            ></v-progress-linear>
          </v-col>
        </v-row>
      </v-container>
    </div>

    <resolved-json-forms
      v-else-if="context !== null"
      :data="context.data"
      :schema="context.schema"
      :uischema="context.uischema"
      :renderers="renderers"
      :cells="cells"
      :config="config"
      :readonly="readonly"
      :uischemas="uischemas"
      :validationMode="validationMode"
      :ajv="ajv"
      :i18n="i18n"
      :additionalErrors="additionalErrors"
      @change="onChange"
    ></resolved-json-forms>
  </div>
</template>

<script lang="ts">
import {
  JsonFormsCellRendererRegistryEntry,
  JsonFormsI18nState,
  JsonFormsRendererRegistryEntry,
  JsonFormsUISchemaRegistryEntry,
  ValidationMode,
} from '@jsonforms/core';
import { JsonFormsChangeEvent, MaybeReadonly } from '@jsonforms/vue2';
import {
  createAjv,
  createTranslator,
  ResolvedJsonForms,
} from '@kchobantonov/common-jsonforms';
import { ErrorObject } from 'ajv';
import Ajv from 'ajv/dist/core';
import _get from 'lodash/get';
import isPlainObject from 'lodash/isPlainObject';
import merge from 'lodash/merge';
import _remove from 'lodash/remove';
import { defineComponent, PropType, ref, toRef } from 'vue';
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
import { LoadEmitter, RestClient } from '../core';
import { CamundaFormApi } from '../core/api';
import { CamundaFormContext, Emitter } from '../core/types';
import { validateCamundaFormConfig } from '../core/validate';
import { camundaRenderers } from '../renderers';

const camundaResolvedJsonForms = defineComponent({
  name: 'camunda-resolved-json-forms',
  components: {
    ResolvedJsonForms,
    VContainer,
    VRow,
    VCol,
    VProgressLinear,
  },
  emits: [
    'change',
    'load-request',
    'load-response',
    'load-error',
    'submit-request',
    'submit-response',
    'submit-error',
  ],
  props: {
    url: {
      required: true,
      type: String,
    },
    processDefinitionId: {
      required: false,
      type: String,
    },
    processDefinitionKey: {
      required: false,
      type: String,
    },
    taskId: {
      required: false,
      type: String,
    },
    renderers: {
      required: false,
      type: Array as PropType<MaybeReadonly<JsonFormsRendererRegistryEntry[]>>,
      default: () => camundaRenderers,
    },
    cells: {
      required: false,
      type: Array as PropType<
        MaybeReadonly<JsonFormsCellRendererRegistryEntry[]>
      >,
      default: () => camundaRenderers,
    },
    config: {
      required: false,
      type: Object as PropType<any>,
      default: undefined,
    },
    readonly: {
      required: false,
      type: Boolean,
      default: false,
    },
    uischemas: {
      required: false,
      type: Array as PropType<MaybeReadonly<JsonFormsUISchemaRegistryEntry[]>>,
      default: () => [],
    },
    validationMode: {
      required: false,
      type: String as PropType<ValidationMode>,
      default: 'ValidateAndShow',
    },
    ajv: {
      required: false,
      type: Object as PropType<Ajv>,
      default: () => createAjv(),
    },
    locale: {
      required: false,
      type: String,
      default: 'en',
    },
    additionalErrors: {
      required: false,
      type: Array as PropType<ErrorObject[]>,
      default: () => [],
    },
    defaultPreset: {
      required: false,
      type: [Object] as PropType<Partial<VuetifyPreset>>,
      default: () => {
        return {
          icons: {
            iconfont: 'mdi',
            values: {},
          },
          theme: {
            dark: false,
            default: 'light',
            disable: false,
            options: {
              cspNonce: undefined,
              customProperties: undefined,
              minifyTheme: undefined,
              themeCache: undefined,
            },
            themes: {
              light: {
                primary: '#1976D2',
                secondary: '#424242',
                accent: '#82B1FF',
                error: '#FF5252',
                info: '#2196F3',
                success: '#4CAF50',
                warning: '#FB8C00',
              },
              dark: {
                primary: '#2196F3',
                secondary: '#424242',
                accent: '#FF4081',
                error: '#FF5252',
                info: '#2196F3',
                success: '#4CAF50',
                warning: '#FB8C00',
              },
            },
          },
        };
      },
    },
    actions: {
      required: false,
      type: [Object] as PropType<Record<string, Function>>,
      default: () => {},
    },
  },
  setup(props) {
    const loading = ref(false);
    const context = ref<CamundaFormContext | null>(null);
    const api = new CamundaFormApi();
    const additionalErrors = ref<ErrorObject[]>([]);
    const previousData = ref({});
    const i18n = ref<JsonFormsI18nState | undefined>(undefined);
    const localeToUse = props.locale ? props.locale : 'en';
    const vuetifyLocale = 'en';

    return {
      vuetifyLocale,
      localeToUse,
      i18n,
      props,
      loading,
      context,
      api,
      additionalErrors,
      previousData,
    };
  },
  watch: {
    url: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.reload();
        }
      },
      deep: true,
    },
    processDefinitionId: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.reload();
        }
      },
      deep: true,
    },
    processDefinitionKey: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.reload();
        }
      },
      deep: true,
    },
    taskId: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.reload();
        }
      },
      deep: true,
    },
    additionalErrors: {
      handler(_value?: ErrorObject[], _oldValue?: ErrorObject[]) {
        // set the last data
        const context = this.context;
        if (context) {
          context.data = this.previousData;
        }
      },
      deep: true,
    },
    locale: {
      handler(value?: string, oldValue?: string) {
        if (value !== oldValue) {
          this.localeToUse = value ? value : 'en';
          this.i18n = {
            locale: this.localeToUse,
            translate: createTranslator(
              this.localeToUse,
              this.context?.translations
            ),
          };

          this.setVuetifyLocale(this.localeToUse);
          this.$forceUpdate();
        }
      },
    },
    context: {
      handler(context: CamundaFormContext | null) {
        let preset: Partial<VuetifyPreset> | null = null;

        if (context?.uischema?.options) {
          preset = this.vuetifyProps(
            context.uischema.options,
            'preset'
          ) as Partial<VuetifyPreset>;
        }

        this.i18n = {
          locale: this.localeToUse,
          translate: createTranslator(this.localeToUse, context?.translations),
        };

        // apply any themes
        this.$vuetify.theme = merge(
          this.$vuetify.theme,
          preset && preset.theme ? preset.theme : this.props.defaultPreset.theme
        );
        this.$vuetify.icons = merge(
          this.$vuetify.icons,
          preset && preset.icons ? preset.icons : this.props.defaultPreset.icons
        );
      },
    },
  },
  async mounted() {
    await this.reload();
    this.vuetifyLocale = this.$vuetify.lang.current;
    this.setVuetifyLocale(this.localeToUse);
  },
  provide() {
    return {
      additionalErrors: toRef(this, 'additionalErrors'),
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
      camundaFormApi: this.api,
      camundaFormEmitter: this.$emit.bind(this),
    };
  },
  methods: {
    onChange(event: JsonFormsChangeEvent): void {
      if (this.additionalErrors.length > 0) {
        // remove the error when we detect a change on the data
        _remove(this.additionalErrors, (error: ErrorObject) => {
          if (error.instancePath) {
            let controlPath: string = error.instancePath;

            // change '/' chars to '.'
            controlPath = controlPath.replace(/\//g, '.');

            // remove '.' chars at the beginning of paths
            controlPath = controlPath.replace(/^./, '');

            if (
              _get(event.data, controlPath) !==
              _get(this.previousData, controlPath)
            ) {
              return true;
            }

            return false;
          }
          return true;
        });
      }
      this.previousData = event.data;
      // for now JsonFormsChangeEvent does not include the additionalErrors
      this.$emit('change', {
        ...event,
        additionalErrors: this.additionalErrors,
      });
    },
    async reload() {
      this.loading = true;
      this.context = null;

      try {
        const restClient = new RestClient([
          new LoadEmitter(this.$emit.bind(this) as Emitter),
        ]);
        const config = validateCamundaFormConfig(this.props);
        const context = await this.api.loadForm(restClient, config);
        context.config = config;

        this.context = context;
      } catch (e) {
        this.$emit('load-error', e);
      } finally {
        this.loading = false;
      }
    },
    vuetifyProps(
      options: Record<string, any>,
      path: string
    ): Record<string, any> {
      const props = _get(options?.vuetify, path);

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

export default camundaResolvedJsonForms;
</script>
