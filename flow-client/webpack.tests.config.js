const path = require("path");

module.exports = {
  mode: "development",
  entry: {
    flow: "./src/test/frontend/FlowTests.ts",
    connect: "./src/test/frontend/ConnectTests.ts",
    gizmo: "./src/test/frontend/VaadinDevmodeGizmoTests.js",
    form_binder: "./src/test/frontend/form/BinderTests.ts",
    form_field: "./src/test/frontend/form/FieldTests.ts",
    form_validation: "./src/test/frontend/form/ValidationTests.ts",
    form_validators: "./src/test/frontend/form/ValidatorsTests.ts"
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
