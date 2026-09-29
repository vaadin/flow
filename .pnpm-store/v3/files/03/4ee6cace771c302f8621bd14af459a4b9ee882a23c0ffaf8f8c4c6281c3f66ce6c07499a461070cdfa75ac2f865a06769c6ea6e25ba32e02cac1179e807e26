var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);
var client_exports = {};
__export(client_exports, {
  RUNTIME_CLIENT_ENTRY_PATH: () => RUNTIME_CLIENT_ENTRY_PATH,
  RUNTIME_CLIENT_RUNTIME_PATH: () => RUNTIME_CLIENT_RUNTIME_PATH,
  WS_CHECKER_ERROR_EVENT: () => WS_CHECKER_ERROR_EVENT,
  WS_CHECKER_RECONNECT_EVENT: () => WS_CHECKER_RECONNECT_EVENT,
  composePreambleCode: () => composePreambleCode,
  runtimeCode: () => runtimeCode,
  runtimeSourceFilePath: () => runtimeSourceFilePath,
  wrapVirtualPrefix: () => wrapVirtualPrefix
});
module.exports = __toCommonJS(client_exports);
var getImportMetaUrl = () => typeof document === "undefined" ? new URL("file:" + __filename).href : document.currentScript && document.currentScript.src || new URL("main.js", document.baseURI).href;
var importMetaUrl = /* @__PURE__ */ getImportMetaUrl();
var import_node_fs = __toESM(require("node:fs"), 1);
var import_node_module = require("node:module");
const _require = (0, import_node_module.createRequire)(importMetaUrl);
const RUNTIME_CLIENT_RUNTIME_PATH = "/@vite-plugin-checker-runtime";
const RUNTIME_CLIENT_ENTRY_PATH = "/@vite-plugin-checker-runtime-entry";
const wrapVirtualPrefix = (id) => `virtual:${id.slice("/".length)}`;
const composePreambleCode = ({
  baseWithOrigin = "/",
  overlayConfig
}) => `
import { inject } from "${baseWithOrigin}${RUNTIME_CLIENT_RUNTIME_PATH.slice(1)}";
inject({
  overlayConfig: ${JSON.stringify(overlayConfig)},
  base: "${baseWithOrigin}",
});
`;
const WS_CHECKER_ERROR_EVENT = "vite-plugin-checker:error";
const WS_CHECKER_RECONNECT_EVENT = "vite-plugin-checker:reconnect";
const runtimeSourceFilePath = importMetaUrl.endsWith(".ts") ? (
  // for development only, maybe should use NODE_ENV to distinguish
  _require.resolve("../@runtime/main.js")
) : _require.resolve("../../@runtime/main.js");
const runtimeCode = `${import_node_fs.default.readFileSync(runtimeSourceFilePath, "utf-8")};`;
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  RUNTIME_CLIENT_ENTRY_PATH,
  RUNTIME_CLIENT_RUNTIME_PATH,
  WS_CHECKER_ERROR_EVENT,
  WS_CHECKER_RECONNECT_EVENT,
  composePreambleCode,
  runtimeCode,
  runtimeSourceFilePath,
  wrapVirtualPrefix
});
//# sourceMappingURL=index.js.map