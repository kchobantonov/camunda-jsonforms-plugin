module.exports = {
  css: {
    extract: {
      filename: 'css/common-jsonforms.css',
      chunkFilename: 'css/common-jsonforms-chunk.css',
    },
  },
  configureWebpack: {
    output: {
      filename: 'js/common-jsonforms.js',
      chunkFilename: 'js/common-jsonforms-chunk.js',
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
  transpileDependencies: ['vuetify', '@jsonforms/core', '@jsonforms/vue2'],
};
