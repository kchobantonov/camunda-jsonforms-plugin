const HtmlWebpackPlugin = require('html-webpack-plugin');

const fs = require('fs');
const path = require('path');

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
        <title>camunda-json-forms demo</title>

        <!-- include the fonts outside the webcomponent for now - https://github.com/google/material-design-icons/issues/1165 -->
        <style type="text/css">
          @import url("//cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css");
        </style>
            
        <script type="text/javascript">

          // selected either processDefinitionKey, processDefinitionId or taskId
          const processDefinitionKey = 'embeddedFormsQuickstart';
          const processDefinitionId = undefined;
          const taskId = undefined;

          const config = {"restrict":true,"trim":false,"showUnfocusedDescription":false,"hideRequiredAsterisk":true};
          const preset = {"theme":{"dark":false}}
          
          const onChange = (event) => {
            let [data] = event.detail;
            console.log('Form state changed:' + JSON.stringify(data));
          };
          const onLoadRequest = (event) => {
            console.log('onLoadRequest');
            let [requestInfo, requestInit] = event.detail;
            
          };
          const onLoadResponse = (event) => {
            console.log('onLoadResponse');
            let [response] = event.detail;
          };
          const onLoadError = (event) => {
            console.log('onLoadError');
            let [error] = event.detail;
      
            if (
              error.name === 'AppException' &&
              (error.code === 'RETRIEVE_TASK_DEPLOYED_FORM' ||
                error.code === 'INVALID_TASK_DEPLOYED_FORM_RESPONSE' ||
                error.code === 'RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM' ||
                error.code ===
                  'INVALID_PROCESS_DEFINITION_DEPLOYED_START_FORM_RESPONSE')
            ) {
              // ignore loading from deployed forms - most likely JsonFormsFormServicePlugin was not installed - just log the error in the console
              return;
            }
            if (
              error.name === 'ResponseException' &&
              (error.response.request.url.endsWith('/deployed-start-form') ||
                error.response.request.url.endsWith('/deployed-form'))
            ) {
              // ignore loading from deployed forms - most likely JsonFormsFormServicePlugin was not installed - just log the error in the console
              return;
            }
      
            alert('Error: ' + error.message);
          };
          const onSubmitRequest = (event) => {
            console.log('onSubmitRequest');
            let [requestInfo, requestInit] = event.detail;
      
          };
          const onSubmitResponse = (event) => {
            console.log('onSubmitResponse');
            let [response] = event.detail;
            if (response.status >= 200 && response.status < 300) {
              alert('Form Completed');
            }
          };
          const onSubmitError = (event) => {
            console.log('onSubmitError');
            let [error] = event.detail;
            if (error.name === 'AppException' && error.response) {
              let response = error.response;
              if (response.status == 401) {
                alert('Error: You are not authenticated');
                return;
              } else if (response.status == 403) {
                alert('Error: You are not authorized');
                return;
              }
            }
      
            alert('Error: ' + error.message);
          };
        </script>

        <camunda-json-forms id="camunda-json-forms"
        url="/engine-rest"
        locale="en"
        readonly="false">
        <style slot="style" type="text/css">
          .v-application--wrap {
            min-height: 0px;
          }
        </style>
      </camunda-json-forms>
  
      <script>
        let form = document.getElementById('camunda-json-forms');
        form.setAttribute('config', JSON.stringify(config));
        form.setAttribute('default-preset', JSON.stringify(preset));

        if (processDefinitionKey) {
          form.setAttribute('process-definition-key', processDefinitionKey);
        }
        if (processDefinitionId) {
          form.setAttribute('process-definition-id', processDefinitionId);
        }
        if (processDefinitionKey) {
          form.setAttribute('task-id', taskId);
        }
        
        form.addEventListener('change', onChange);
        form.addEventListener('load-request', onLoadRequest);
        form.addEventListener('load-response', onLoadResponse);
        form.addEventListener('load-error', onLoadError);
        form.addEventListener('submit-request', onSubmitRequest);
        form.addEventListener('submit-response', onSubmitResponse);
        form.addEventListener('submit-error', onSubmitError);  
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
    proxy: {
      '^/camunda': {
        target: 'http://localhost:8080/engine-rest',
        changeOrigin: true,
        pathRewrite: {
          "^/camunda": "/",
        },
      },
      '^/forms': {
        target: 'http://localhost:8080/',
        changeOrigin: true,
      },
    },
    watchOptions: {
      ignored: ['node_modules'],
      poll: true,
    },
  },
  transpileDependencies: ['vuetify', '@jsonforms/core', '@jsonforms/vue2'],
};
