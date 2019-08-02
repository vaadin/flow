const path = require("path");

module.exports = {
  entry: {
    tests: "./src/test/frontend/FlowTests.ts"
  },
  output: {
    filename: "[name].js",
    path: path.resolve(__dirname, "target/frontend-tests")
  },

  resolve: {
    modules: [
      path.resolve(__dirname, "./src/main/resources/META-INF/frontend"),
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
