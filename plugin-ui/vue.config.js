module.exports = {
  configureWebpack: {
    output: {
      filename: 'js/camunda-jsonforms.js',
    },
    optimization: {
      splitChunks: false,
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
