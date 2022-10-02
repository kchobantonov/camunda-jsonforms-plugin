const HtmlWebpackPlugin = require('html-webpack-plugin');

const fs = require('fs');
const path = require('path');

const data = require('./src/example/data.json');
const schema = require('./src/example/schema.json');
const uischema = require('./src/example/uischema.json');
const uischemas = require('./src/example/uischemas.json');
const preset = require('./src/example/preset.json');
const i18n = require('./src/example/i18n.json');
const uidata = require('./src/example/uidata.json');
const config = require('./src/example/config.json');
const style = fs
  .readFileSync(path.join(__dirname, './src/example/user-style.css'))
  .toString();
const actions = fs
  .readFileSync(path.join(__dirname, './src/example/actions.js'))
  .toString();

module.exports = {
  configureWebpack: {
    plugins: [
      new HtmlWebpackPlugin({
        filename: 'demo.html',
        minify: false,
        templateContent: `
        <meta charset="utf-8">
        <title>vuetify-json-forms demo</title>
        <!-- include the fonts outside the webcomponent for now - https://github.com/google/material-design-icons/issues/1165 -->
        <style type="text/css">
          @import url("//cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css");
        </style>
            
        <script type="text/javascript">
  
          const data = ${JSON.stringify(data)};
          const schema = ${JSON.stringify(schema)};
          const uischema = ${JSON.stringify(uischema)};
          const uischemas = ${JSON.stringify(uischemas)};
          const config = ${JSON.stringify(config)};
          const preset = ${JSON.stringify(preset)};
          const i18n = ${JSON.stringify(i18n)}; 
          const uidata = ${JSON.stringify(uidata)}; 

          ${actions.replace(/export const/g, 'const')}
        </script>
  
      <vuetify-json-forms id="vuetify-json-forms">
        <style slot="style" type="text/css">
          .v-application--wrap {
            min-height: 0px;
          }
        </style>
      </vuetify-json-forms>
  
      <script>
        let form = document.getElementById('vuetify-json-forms');

        form.setAttribute('data', JSON.stringify(data));
        form.setAttribute('schema', JSON.stringify(schema));
        form.setAttribute('uischema', JSON.stringify(uischema));
        form.setAttribute('uischemas', JSON.stringify(uischemas));
        form.setAttribute('uidata', JSON.stringify(uidata));
        form.setAttribute('config', JSON.stringify(config));
        form.setAttribute('translations', JSON.stringify(i18n));
        form.setAttribute('default-preset', JSON.stringify(preset));

        form.addEventListener('change', onChange);
        form.addEventListener('init', onInit);
      </script>`,
      }),
    ],
  },
  chainWebpack: (config) => {
    // remove typecheck
    config.plugins.delete('fork-ts-checker');

    return config;
  },
  devServer: {
    watchOptions: {
      ignored: ['node_modules'],
      poll: true,
    },
  },
  transpileDependencies: ['vuetify', '@jsonforms/core', '@jsonforms/vue2'],
};
