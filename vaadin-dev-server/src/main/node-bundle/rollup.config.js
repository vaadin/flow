import { nodeResolve } from "@rollup/plugin-node-resolve";
import commonjs from "rollup-plugin-commonjs";

export default {
  input: "./node-util",
  output: {
    file: "../resources/vaadin-dev-server-node-bundle.js",
    format: "cjs",
    exports: "named",
  },
  plugins: [nodeResolve(), commonjs()],
};
