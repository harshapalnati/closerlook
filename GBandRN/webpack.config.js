const path = require('path');
// Use absolute require so this config works when invoked from any CWD
const appDirectory = path.resolve(__dirname);
const HtmlWebpackPlugin = require(path.resolve(appDirectory, 'node_modules/html-webpack-plugin'));


// Packages that need to be transpiled by Babel
const babelLoaderConfiguration = {
  test: /\.(js|jsx|ts|tsx)$/,
  include: [
    path.resolve(appDirectory, 'index.web.js'),
    path.resolve(appDirectory, 'App.tsx'),
    path.resolve(appDirectory, 'src'),
    path.resolve(appDirectory, 'node_modules/react-native-web'),
    path.resolve(appDirectory, 'node_modules/@react-navigation'),
    path.resolve(appDirectory, 'node_modules/react-native-screens'),
    path.resolve(appDirectory, 'node_modules/react-native-safe-area-context'),
    path.resolve(appDirectory, 'node_modules/@react-native-async-storage'),
    path.resolve(appDirectory, 'node_modules/react-native-svg'),
    path.resolve(appDirectory, 'node_modules/@react-native-community'),
    path.resolve(appDirectory, 'node_modules/zustand'),
  ],
  use: {
    loader: path.resolve(appDirectory, 'node_modules/babel-loader/lib/index.js'),
    options: {
      cacheDirectory: true,
      presets: [path.resolve(appDirectory, 'node_modules/@react-native/babel-preset')],
      plugins: [path.resolve(appDirectory, 'node_modules/babel-plugin-react-native-web')],
    },
  },
};

// Image/asset loader
const imageLoaderConfiguration = {
  test: /\.(gif|jpe?g|png|svg)$/,
  use: {
    loader: path.resolve(appDirectory, 'node_modules/url-loader/dist/index.js'),
    options: { name: '[name].[ext]' },
  },
};

module.exports = {
  entry: path.resolve(appDirectory, 'index.web.js'),
  output: {
    filename: 'bundle.web.js',
    path: path.resolve(appDirectory, 'web-build'),
  },
  resolveLoader: {
    modules: [path.resolve(appDirectory, 'node_modules')],
  },
  resolve: {
    extensions: ['.web.tsx', '.web.ts', '.web.js', '.tsx', '.ts', '.js'],
    alias: {
      // Map react-native → react-native-web
      'react-native$': 'react-native-web',
      // Stub out native-only modules
      'react-native-screens': path.resolve(appDirectory, 'src/web-stubs/react-native-screens.js'),
      'react-native-safe-area-context': path.resolve(appDirectory, 'src/web-stubs/react-native-safe-area-context.js'),
      '@react-native-async-storage/async-storage': path.resolve(appDirectory, 'src/web-stubs/async-storage.js'),
    },
  },
  module: {
    rules: [babelLoaderConfiguration, imageLoaderConfiguration],
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(appDirectory, 'public/index.html'),
    }),
  ],
  devServer: {
    port: 3000,
    hot: true,
    open: true,
    historyApiFallback: true,
  },
  mode: 'development',
};
