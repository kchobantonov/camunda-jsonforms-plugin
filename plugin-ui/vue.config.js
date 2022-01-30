module.exports = {
  css: {
    extract: {
      filename: 'css/camunda-jsonforms.css',
      chunkFilename: 'css/camunda-jsonforms-chunk.css',
    },
  },
  configureWebpack: {
    output: {
      filename: 'js/camunda-jsonforms.js',
      chunkFilename: 'js/camunda-jsonforms-chunk.js',
    },
  },
  filenameHashing: false,
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
  transpileDependencies: ['vuetify'],
};
