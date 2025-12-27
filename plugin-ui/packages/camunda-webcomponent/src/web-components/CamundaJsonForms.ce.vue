<template>
  <Suspense>
    <div>
      <dynamic-element
        tag="style"
        type="text/css"
        :id="stylesheetId"
        :nonce="stylesheetNonce"
      >
        {{ vuetifyThemeCss }}
      </dynamic-element>

      <slot-element name="styles"></slot-element>

      <dynamic-element tag="style" type="text/css" :nonce="stylesheetNonce">
        {{ customStyleToUse }}
      </dynamic-element>

      <v-locale-provider :rtl="appStore.rtl" :locale="appStore.locale">
        <v-theme-provider :theme="theme">
          <v-defaults-provider :defaults="appStore.vuetifyOptions.defaults">
            <v-sheet>
              <v-container style="height: 400px" v-if="error !== undefined">
                <v-row
                  class="fill-height"
                  align-content="center"
                  justify="center"
                >
                  <v-col class="text-subtitle-1 text-center error" cols="12">
                    {{ error }}
                  </v-col>
                </v-row>
              </v-container>

              <template v-else>
                <slot-element name="form-header">
                  <!-- Place custom content inside <div slot="form-header"></div> within <vuetify-json-forms> to fill this slot -->
                </slot-element>

                <camunda-resolved-json-forms
                  part="json-forms"
                  :url="urlToUse"
                  :processDefinitionId="processDefinitionIdToUse"
                  :processDefinitionKey="processDefinitionKeyToUse"
                  :taskId="taskIdToUse"
                  :locale="localeToUse"
                  :vuetify-config="vuetifyConfig"
                  :config="configToUse"
                  :validationMode="validationModeToUse"
                  :readonly="readonlyToUse"
                  @change="onChange"
                  @load-request="onLoadRequest"
                  @load-response="onLoadResponse"
                  @load-error="onLoadError"
                  @submit-request="onSubmitRequest"
                  @submit-response="onSubmitResponse"
                  @submit-error="onSubmitError"
                ></camunda-resolved-json-forms>

                <slot-element name="form-footer">
                  <!-- Place custom content inside <div slot="form-footer"></div> within <vuetify-json-forms> to fill this slot -->
                </slot-element>
              </template>
            </v-sheet>
          </v-defaults-provider>
        </v-theme-provider>
      </v-locale-provider>
    </div>
  </Suspense>
</template>

<script lang="ts">
import {
  defaultVuetifyOptions,
  isValidVuetifyOptions,
  type VuetifyOptions,
} from '@/plugins/options';
import buildVuetify from '@/plugins/vuetify';
import { useAppStore } from '@/store';
import { CamundaResolvedJsonForms } from '@chobantonov/camunda-jsonforms';
import {
  DynamicElement,
  getLightDarkTheme,
  HandleActionEmitterKey,
  SlotElement,
  type VuetifyConfig
} from '@chobantonov/jsonforms-vuetify-renderers';
import {
  type ValidationMode
} from '@jsonforms/core';
import type { JsonFormsChangeEvent } from '@jsonforms/vue';
import { useMediaQuery } from '@vueuse/core';
import isPlainObject from 'lodash/isPlainObject';
import {
  computed,
  defineComponent,
  getCurrentInstance,
  onMounted,
  type PropType,
  ref,
  watch
} from 'vue';
import { useTheme } from 'vuetify';
import {
  VCol,
  VContainer,
  VDefaultsProvider,
  VLocaleProvider,
  VRow,
  VSheet,
  VThemeProvider,
} from 'vuetify/components';
import { createTheme } from 'vuetify/lib/composables/theme.mjs';
import { extractAndInjectFonts } from '../util/inject-fonts';

const DANGEROUS_TAGS = [
  'SCRIPT',
  'IFRAME',
  'OBJECT',
  'EMBED',
  'APPLET',
  'CANVAS', // Can be used for fingerprinting/malicious drawing
  'META', // Can redirect or change document behavior
  'BASE', // Can redirect or change document behavior
];

type SlotRules = {
  readonly [K in 'styles' | 'form-header' | 'form-footer']: {
    readonly allow?: readonly string[];
    readonly deny?: readonly string[];
  };
};
const slotRules: SlotRules = {
  styles: {
    allow: ['STYLE', 'LINK'],
  },
  'form-header': {
    deny: ['STYLE', 'LINK'],
  },
  'form-footer': {
    deny: ['STYLE', 'LINK'],
  },
};

const toBoolean = (val: any): boolean | undefined => {
  if (typeof val === 'string') {
    return val === 'true';
  }

  if (typeof val === 'boolean') {
    return val;
  }

  return undefined;
};

export default defineComponent({
  name: 'CamundaJsonForms',
  components: {
    CamundaResolvedJsonForms,
    VThemeProvider,
    VLocaleProvider,
    VDefaultsProvider,
    VContainer,
    VRow,
    VCol,
    VSheet,
    DynamicElement,
    SlotElement,
  },
  emits: [
    'change',
    'load-request',
    'load-response',
    'load-error',
    'submit-request',
    'submit-response',
    'submit-error',
    'handle-action',
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
    config: {
      type: [Object, String] as any,
      default: () => {
        return JSON.stringify({
          restrict: true,
          trim: false,
          showUnfocusedDescription: false,
          hideRequiredAsterisk: true,
        });
      },
      validator: (value: any) => {
        try {
          const obj = typeof value === 'string' ? JSON.parse(value) : value;
          return obj == null || isPlainObject(obj);
        } catch {
          return false;
        }
      },
    },
    readonly: { type: String, default: 'false' },
    validationMode: {
      type: String as PropType<ValidationMode>,
      default: 'ValidateAndShow',
      validator: (v: string) =>
        v === 'ValidateAndShow' ||
        v === 'ValidateAndHide' ||
        v === 'NoValidation',
    },
    locale: { type: String, default: 'en' },
    dark: { type: String, default: undefined },
    rtl: { type: String, default: 'false' },
    vuetifyOptions: {
      type: [Object as PropType<VuetifyOptions>, String] as any,
      validator: isValidVuetifyOptions,
    },
    customStyle: {
      type: String,
    },
  },
  setup(props, { emit }) {
    const normalize = (val: any) => {
      if (typeof val === 'string') {
        try {
          return JSON.parse(val);
        } catch {
          return val;
        }
      }
      return val;
    };

    const app = getCurrentInstance()?.appContext.app;
    const vuetifyOptions: VuetifyOptions | null | undefined = normalize(
      props.vuetifyOptions,
    );

    let dark: boolean | undefined = toBoolean(props.dark);

    if (dark === undefined) {
      // if dark is not yet defined by the props then check the vuetifyOptions
      const colorSchema = vuetifyOptions?.['color-schema'];

      if (colorSchema === 'dark') {
        dark = true;
      } else if (colorSchema === 'light') {
        dark = false;
      }
    }

    const appStore = useAppStore({
      dark,
      vuetifyOptions: vuetifyOptions ?? {},
    });

    // Configure Vuetify and other plugins here
    app!.use(buildVuetify(appStore));

    appStore.rtl = toBoolean(props.rtl) ?? false;
    appStore.locale = props.locale ?? vuetifyOptions?.locale?.locale ?? 'en';

    const error = ref<string | undefined>(undefined);
    const urlToUse = ref<string | undefined>(props.url);
    const processDefinitionIdToUse = ref<string | undefined>(
      props.processDefinitionId,
    );
    const processDefinitionKeyToUse = ref<string | undefined>(
      props.processDefinitionKey,
    );
    const taskIdToUse = ref<string | undefined>(props.taskId);

    const configToUse = ref(normalize(props.config));
    const readonlyToUse = ref<boolean | undefined>(toBoolean(props.readonly));
    const validationModeToUse = ref(
      props.validationMode === 'ValidateAndShow' ||
        props.validationMode === 'ValidateAndHide' ||
        props.validationMode === 'NoValidation'
        ? props.validationMode
        : 'ValidateAndShow',
    );
    const customStyleToUse = ref(props.customStyle);
    const localeToUse = ref<string | undefined>(appStore.locale);

    const themeInstance = useTheme();
    const isPreferredDark = useMediaQuery('(prefers-color-scheme: dark)');

    const theme = computed(() => {
      // add props as deps as well
      if (typeof props.vuetifyOptions === 'object') {
        const opts = props.vuetifyOptions as VuetifyOptions;
        if (typeof opts.theme === 'object') {
          // add defaultTheme as deps, only touching is needed
          opts.theme.defaultTheme;
        }
      }

      let dark = appStore.dark ?? isPreferredDark.value;
      let defaultTheme = dark ? 'dark' : 'light';
      if (
        typeof appStore.vuetifyOptions.theme === 'object' &&
        appStore.vuetifyOptions.theme.defaultTheme
      ) {
        defaultTheme = appStore.vuetifyOptions.theme.defaultTheme;
      }

      if (defaultTheme === 'system') {
        defaultTheme = isPreferredDark.value ? 'dark' : 'light';
      }
      const exists = (themeName: string) =>
        themeName in themeInstance.themes.value;

      return getLightDarkTheme(dark, defaultTheme, exists);
    });

    const stylesheetId = computed(() =>
      typeof appStore.vuetifyOptions.theme === 'object'
        ? (appStore.vuetifyOptions.theme.stylesheetId ??
          'camunda-theme-stylesheet')
        : 'camunda-theme-stylesheet',
    );

    const stylesheetNonce = computed(() =>
      typeof appStore.vuetifyOptions.theme === 'object'
        ? appStore.vuetifyOptions.theme.cspNonce
        : undefined,
    );

    const vuetifyThemeCss = computed(() => {
      let css = themeInstance?.styles.value ?? '';
      if (css.startsWith(':root {'))
        css = ':host {' + css.slice(':root {'.length);
      return css;
    });

    const vuetifyConfig = computed<VuetifyConfig>(() => ({
      components: appStore.vuetifyOptions.components ?? {},
      directives: appStore.vuetifyOptions.directives ?? {},
      defaults: appStore.vuetifyOptions.defaults ?? {},
      theme: theme.value,
      rtl: appStore.rtl,
    }));

    // ===== Watchers =====
    watch(
      () => props.url,
      (value, oldValue) => {
        if (value !== oldValue) {
          urlToUse.value = value;
        }
      },
    );
    watch(
      () => props.processDefinitionId,
      (value, oldValue) => {
        if (value !== oldValue) {
          processDefinitionIdToUse.value = value;

          // reset other 2
          processDefinitionKeyToUse.value = undefined;
          taskIdToUse.value = undefined;
        }
      },
    );
    watch(
      () => props.processDefinitionKey,
      (value, oldValue) => {
        if (value !== oldValue) {
          processDefinitionKeyToUse.value = value;

          // reset other 2
          processDefinitionIdToUse.value = undefined;
          taskIdToUse.value = undefined;
        }
      },
    );
    watch(
      () => props.taskId,
      (value, oldValue) => {
        if (value !== oldValue) {
          taskIdToUse.value = value;

          // reset other 2
          processDefinitionIdToUse.value = undefined;
          processDefinitionKeyToUse.value = undefined;
        }
      },
    );
    watch(
      () => props.config,
      (value, oldValue) => {
        if (value !== oldValue) {
          configToUse.value = normalize(value);
        }
      },
      { deep: true },
    );
    watch(
      () => props.readonly,
      (value, oldValue) => {
        if (value !== oldValue) {
          readonlyToUse.value = toBoolean(value);
        }
      },
    );

    watch(
      () => props.validationMode,
      (v) => (validationModeToUse.value = v as ValidationMode),
    );
    watch(
      () => props.customStyle,
      (customStyle, oldCustomStyle) => {
        if (customStyle !== oldCustomStyle) {
          customStyleToUse.value = customStyle;
        }
      },
    );
    watch(
      () => appStore.locale,
      (value, oldValue) => {
        if (value !== oldValue) {
          localeToUse.value = value;
        }
      },
    );

    watch(
      () => props.rtl,
      (v) => (appStore.rtl = toBoolean(v) ?? false),
    );
    watch(
      () => props.locale,
      (v) => {
        appStore.locale = v ?? 'en';
      },
    );

    watch(
      () => props.dark,
      (dark, oldDark) => {
        if (dark !== oldDark) {
          let newDark = toBoolean(dark);
          if (newDark === undefined) {
            // check the vuetify options
            if (props.vuetifyOptions?.['color-schema']) {
              const colorSchema = vuetifyOptions?.['color-schema'];

              if (colorSchema === 'dark') {
                newDark = true;
              } else if (colorSchema === 'light') {
                newDark = false;
              }
            }
          }

          appStore.dark = newDark;
        }
      },
    );
    watch(
      () => props.vuetifyOptions,
      (vuetifyOptions, oldVuetifyOptions) => {
        if (vuetifyOptions?.locale !== oldVuetifyOptions?.locale) {
          appStore.locale =
            props.locale ?? vuetifyOptions?.locale?.locale ?? 'en';
        }

        if (
          toBoolean(props.dark) === undefined &&
          vuetifyOptions?.['color-schema'] !==
            oldVuetifyOptions?.['color-schema']
        ) {
          const colorSchema = vuetifyOptions?.['color-schema'];

          if (colorSchema === 'dark') {
            appStore.dark = true;
          } else if (colorSchema === 'light') {
            appStore.dark = false;
          }
        }

        appStore.vuetifyOptions = {
          ...defaultVuetifyOptions,
          ...vuetifyOptions,
        };

        if (vuetifyOptions.theme) {
          const newThemeInstance = createTheme(appStore.vuetifyOptions.theme);

          themeInstance.themes.value = newThemeInstance.themes.value;
          themeInstance.global.name.value = theme.value;
        }
      },
      { deep: true },
    );

    const onChange = (event: JsonFormsChangeEvent) => {
      emit('change', event);
    };

    const onLoadRequest = (input: RequestInfo, init?: RequestInit) => {
      emit('load-request', input, init);
    };
    const onLoadResponse = (response: Response) => {
      emit('load-response', response);
    };
    const onLoadError = (error: any) => {
      emit('load-error', error);
    };
    const onSubmitRequest = (input: RequestInfo, init?: RequestInit) => {
      emit('submit-request', input, init);
    };
    const onSubmitResponse = (response: Response) => {
      emit('submit-response', response);
    };

    const onSubmitError = (error: any) => {
      emit('submit-error', error);
    };

    const injectFontsStyle = (root: Node) => {
      // Inject all @font-face rules from component's styles and Vuetify theme
      extractAndInjectFonts(root, 'camunda-json-forms-fonts');
    };

    const injectCustomFontsStyle = (css: string) => {
      extractAndInjectFonts(css, 'camunda-json-forms-fonts-custom');
    };

    onMounted(() => {
      const vm = getCurrentInstance();

      const shadowRoot = vm?.vnode?.el?.getRootNode();

      injectFontsStyle(shadowRoot);
      if (customStyleToUse.value) {
        injectCustomFontsStyle(customStyleToUse.value);
      }

      shadowRoot.addEventListener('slotchange', (e: Event) => {
        const slot = e.target;

        if (slot instanceof HTMLSlotElement && slot.name in slotRules) {
          const rules = slotRules[slot.name as keyof SlotRules];
          slot.assignedElements().forEach((el) => {
            if (DANGEROUS_TAGS.includes(el.tagName)) {
              console.error(
                `Security violation: <${el.tagName.toLowerCase()}> is not allowed in any slot. Element removed.`,
              );
              el.remove();
              return; // Skip further validation for this element
            }

            // Deny list check (highest priority)
            if (rules.deny && rules.deny.includes(el.tagName)) {
              console.error(
                `Denied: <${el.tagName.toLowerCase()}> removed from "${slot.name}".`,
              );
              el.remove();
              return;
            }

            if (rules.allow && !rules.allow.includes(el.tagName)) {
              console.error(
                `Consider using ${rules.allow.join(', ')} tags for ${slot.name} slot`,
              );
              el.remove();
              return;
            }
          });
        }
      });
    });

    watch(customStyleToUse, (newCss) => {
      if (newCss) injectCustomFontsStyle(newCss);
    });

    return {
      urlToUse,
      processDefinitionIdToUse,
      processDefinitionKeyToUse,
      taskIdToUse,
      configToUse,
      readonlyToUse,
      validationModeToUse,
      customStyleToUse,
      localeToUse,
      appStore,
      theme,
      stylesheetId,
      stylesheetNonce,
      vuetifyConfig,
      vuetifyThemeCss,
      error,
      onChange,
      onLoadRequest,
      onLoadResponse,
      onLoadError,
      onSubmitRequest,
      onSubmitResponse,
      onSubmitError,
    };
  },
  provide() {
    return {
      [HandleActionEmitterKey]: this.$root!.$emit,
    };
  },
});
</script>
