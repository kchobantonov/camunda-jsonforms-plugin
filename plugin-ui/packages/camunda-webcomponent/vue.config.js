const HtmlWebpackPlugin = require('html-webpack-plugin');

const fs = require('fs');
const path = require('path');

const preset = require('./src/example/preset.json');
const config = require('./src/example/config.json');
const style = fs
  .readFileSync(path.join(__dirname, './src/example/user-style.css'))
  .toString();
const listeners = fs
  .readFileSync(path.join(__dirname, './src/example/listeners.js'))
  .toString();

module.exports = {
  configureWebpack: {
    optimization: {
      splitChunks: {
        chunks: 'async',
        minSize: 10000,
        cacheGroups: {
          vendor: {
            test: /[\\/]node_modules[\\/]/,
            name(module) {
              const packageName = module.context.match(/[\\/]node_modules[\\/](.*?)([\\/]|$)/)[1];
              return `npm.${packageName.replace('@', '')}`;
            },
          },
        },
      },
    },
    plugins: [
      new HtmlWebpackPlugin({
        filename: 'demo.html',
        minify: false,
        templateContent: `
        <meta charset="utf-8">
        <title>camunda-json-forms demo</title>

        <!-- include the fonts outside the webcomponent for now - https://github.com/google/material-design-icons/issues/1165 -->
        <style type="text/css">
          @import url('//cdn.jsdelivr.net/npm/@mdi/font@6.x/css/materialdesignicons.min.css');
        </style>

        <style>
          /* # =================================================================
          # Global selectors
          # ================================================================= */
          html {
            box-sizing: border-box;
            overflow-y: scroll;
            /* All browsers without overlaying scrollbars */
            -webkit-text-size-adjust: 100%;
            /* Prevent adjustments of font size after orientation changes in iOS */
            word-break: normal;
            -moz-tab-size: 4;
            tab-size: 4;
          }
          
          *,
          ::before,
          ::after {
            background-repeat: no-repeat;
            /* Set background-repeat: no-repeat to all elements and pseudo elements */
            box-sizing: inherit;
          }
          
          ::before,
          ::after {
            text-decoration: inherit;
            /* Inherit text-decoration and vertical align to ::before and ::after pseudo elements */
            vertical-align: inherit;
          }
          
          * {
            padding: 0;
            /* Reset padding and margin of all elements */
            margin: 0;
          }
    
        </style>

        <script type="text/javascript">
        const config = ${JSON.stringify(config)};
        const preset = ${JSON.stringify(preset)};
        const style = ${"`" + style + "`"}; 

        ${listeners
          .replace(/export const /g, 'const ')
          .replace(/export let /g, 'let ')}

        </script>

        <camunda-json-forms id="camunda-json-forms"
        url="/engine-rest"
        locale="en"
        readonly="false">
        </camunda-json-forms>
  
      <script>
        let form = document.getElementById('camunda-json-forms');
        form.setAttribute('custom-style', style);
        form.setAttribute('config', JSON.stringify(config));
        form.setAttribute('default-preset', JSON.stringify(preset));

        if (processDefinitionKey) {
          form.setAttribute('process-definition-key', processDefinitionKey);
        }
        if (processDefinitionId) {
          form.setAttribute('process-definition-id', processDefinitionId);
        }
        if (taskId) {
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
      '^/engine-rest': {
        target: 'http://localhost:8080/',
        changeOrigin: true,
        logLevel: 'debug' 
      },
      '^/forms': {
        target: 'http://localhost:8080/',
        changeOrigin: true,
        logLevel: 'debug' 
      },
    },
    watchOptions: {
      ignored: ['node_modules'],
      poll: true,
    },
  },
  transpileDependencies: ['vuetify', '@jsonforms/core', '@jsonforms/vue2'],
};
