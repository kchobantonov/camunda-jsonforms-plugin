import jsonFormsVuetifyStyles from '@chobantonov/jsonforms-vuetify-renderers/lib/jsonforms-vuetify-renderers.css?url';
import jsonFormsVueVuetifyStyles from '@jsonforms/vue-vuetify/lib/jsonforms-vue-vuetify.css?url';
import materialDesignIconsStyles from '@mdi/font/css/materialdesignicons.css?url';
import fontAwesomeStyles from '@fortawesome/fontawesome-free/css/all.css?url';
import vuetifyStyles from 'vuetify/dist/vuetify.css?url';
import vuetifyLabsStyles from 'vuetify/dist/vuetify-labs.css?url';
import monacoEditorStyles from 'monaco-editor/min/vs/editor/editor.main.css?url';
import customVuetifyStyles from './styles/vuetify.scss?url';

const vuetifyResetStyles = `
:host {
  box-sizing: border-box;
  overflow-y: scroll;
  /* All browsers without overlaying scrollbars */
  -webkit-text-size-adjust: 100%;
  /* Prevent adjustments of font size after orientation changes in iOS */
  word-break: normal;
  tab-size: 4;
}

:host {
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0);
  font-family: Roboto, sans-serif;
  font-size: 1rem;
  line-height: 1.5;
  overflow-x: hidden;
}

:host(.overflow-y-hidden) {
  overflow-y: hidden !important;
}

:host(.v-overlay-scroll-blocked) {
  position: fixed;
  top: var(--v-body-scroll-y);
  left: var(--v-body-scroll-x);
  width: 100%;
  height: 100%;
}

:host {
  --v-theme-background: #fff;
  --v-theme-on-background: #000;
  --v-theme-surface: #fff;
  --v-theme-on-surface: #000;
  --v-theme-overlay-multiplier: 1;
  --v-scrollbar-offset: 0px;
}`;

export const styles = [
  vuetifyResetStyles,
  `
  @import url('${materialDesignIconsStyles}');
  @import url('${fontAwesomeStyles}');
  @import url('${vuetifyStyles}');
  @import url('${vuetifyLabsStyles}');
  @import url('${jsonFormsVuetifyStyles}');
  @import url('${jsonFormsVueVuetifyStyles}');
  @import url('${monacoEditorStyles}');
  @import url('${customVuetifyStyles}');
  `,
];
