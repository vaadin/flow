const path = require("path");

module.exports = {
  mode: "development",
  entry: {
    flow: "./src/test/frontend/FlowTests.ts"
  },
  output: {
    filename: "[name].spec.js",
    path: path.resolve(__dirname, "target/frontend-tests")
  },

  resolve: {
    modules: [
      path.resolve(__dirname, "./src/test/frontend"),
      "node_modules/"
    ],
    extensions: [".ts", ".js"]
  },

  module: {
    rules: [
      {
        test: /\.ts$/,
        use: [
          {
            loader: "esbuild-loader",
            options: {
              loader: "ts",
              target: "es2019"
            }
          }
        ]
      }
    ]
  },

  node: {
    process: false,
    global: false,
    fs: "empty"
  }
};
