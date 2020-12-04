const path = require("path");

module.exports = {
  mode: "development",
  entry: {
    flow: "./src/test/frontend/FlowTests.ts",
    connect: "./src/test/frontend/ConnectTests.ts",
    authentication: "./src/test/frontend/AuthenticationTests.ts",
    offline: "./src/test/frontend/OfflineTests.ts",
    connection_indicator: "./src/test/frontend/ConnectionIndicatorTests.ts",
    connection_state: "./src/test/frontend/ConnectionStateTests.ts",
    gizmo: "./src/test/frontend/VaadinDevmodeGizmoTests.js",
    form_binder: "./src/test/frontend/form/BinderTests.ts",
    form_field: "./src/test/frontend/form/FieldTests.ts",
    form_index: "./src/test/frontend/form/IndexTests.ts",
    form_validation: "./src/test/frontend/form/ValidationTests.ts",
    form_validators: "./src/test/frontend/form/ValidatorsTests.ts",
    form_model: "./src/test/frontend/form/ModelTests.ts",
    deferrable_endpoint: "./src/test/frontend/DeferrableEndpointTest.ts"
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
