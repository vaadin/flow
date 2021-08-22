const path = require("path");

module.exports = {
  mode: "development",
  entry: {
    flow: "./src/test/frontend/FlowTests.ts",
    connect: "./src/test/frontend/ConnectTests.ts",
    authentication: "./src/test/frontend/AuthenticationTests.ts",
    connection_indicator: "./src/test/frontend/ConnectionIndicatorTests.ts",
    connection_state: "./src/test/frontend/ConnectionStateTests.ts",
    gizmo: "./src/test/frontend/VaadinDevmodeGizmoTests.js",
    form_binder: "./src/test/frontend/form/BinderTests.ts",
    form_field: "./src/test/frontend/form/FieldTests.ts",
    form_index: "./src/test/frontend/form/IndexTests.ts",
    form_validation: "./src/test/frontend/form/ValidationTests.ts",
    form_validators: "./src/test/frontend/form/ValidatorsTests.ts",
    form_model: "./src/test/frontend/form/ModelTests.ts"
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
              target: "es2019",
            },
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
