const HtmlWebpackPlugin = require('html-webpack-plugin');

const fs = require('fs');
const path = require('path');

const data = require('./src/example/data.json');
const schema = require('./src/example/schema.json');
const uischema = require('./src/example/uischema.json');
const preset = require('./src/example/preset.json');
const config = require('./src/example/config.json');
const style = fs
  .readFileSync(path.join(__dirname, './src/example/user-style.css'))
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
  
          const data = {"firstName":"Test"};
          const schema = {"type":"object","required":["firstName"],"properties":{"address":{"type":"string"},"gender":{"type":"string","enum":["Male","Female","Undisclosed"]},"firstName":{"type":"string","minLength":2,"maxLength":20},"lastName":{"type":"string","minLength":5,"maxLength":15}}};
          const uischema = {"type":"VerticalLayout","elements":[{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/properties/firstName"},{"type":"Control","scope":"#/properties/lastName"}]},{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/properties/gender"}]},{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/properties/address"}]}]};
          const config = {"restrict":true,"trim":false,"showUnfocusedDescription":false,"hideRequiredAsterisk":true};
          const preset = {"theme":{"dark":false}}
          const onChange = (event) => {
            let [data] = event.detail;
            console.log('Form state changed:' + JSON.stringify(data));
          };
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
        form.setAttribute('config', JSON.stringify(config));
        form.setAttribute('default-preset', JSON.stringify(preset));
        form.addEventListener('change', onChange);
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
