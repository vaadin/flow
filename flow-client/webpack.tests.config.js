const path = require("path");

module.exports = {
  mode: "development",
  entry: {
    flow: "./src/test/frontend/FlowTests.ts",
    connect: "./src/test/frontend/ConnectTests.ts",
    binder: "./src/test/frontend/BinderTests.ts",
    validator: "./src/test/frontend/FormValidatorTests.ts"
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
            loader: "ts-loader",
            options: {
              transpileOnly: true
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
