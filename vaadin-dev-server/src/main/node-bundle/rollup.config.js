/*
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
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
