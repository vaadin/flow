import { createRequire } from "node:module";
import fs from "node:fs";
//#region src/client/index.ts
const _require = createRequire(import.meta.url);
const RUNTIME_CLIENT_RUNTIME_PATH = "/@vite-plugin-checker-runtime";
const RUNTIME_CLIENT_ENTRY_PATH = "/@vite-plugin-checker-runtime-entry";
const wrapVirtualPrefix = (id) => `virtual:${id.slice(1)}`;
const composePreambleCode = ({ baseWithOrigin = "/", overlayConfig, useBase = true }) => `
import { inject } from "${useBase ? baseWithOrigin : "/"}${RUNTIME_CLIENT_RUNTIME_PATH.slice(1)}";
inject({
  overlayConfig: ${JSON.stringify(overlayConfig)},
  base: "${baseWithOrigin}",
});
`;
const WS_CHECKER_ERROR_EVENT = "vite-plugin-checker:error";
const WS_CHECKER_RECONNECT_EVENT = "vite-plugin-checker:reconnect";
const runtimeSourceFilePath = _require.resolve("../@runtime/main.js");
const runtimeCode = `${fs.readFileSync(runtimeSourceFilePath, "utf-8")};`;
//#endregion
export { RUNTIME_CLIENT_ENTRY_PATH, RUNTIME_CLIENT_RUNTIME_PATH, WS_CHECKER_ERROR_EVENT, WS_CHECKER_RECONNECT_EVENT, composePreambleCode, runtimeCode, runtimeSourceFilePath, wrapVirtualPrefix };

//# sourceMappingURL=index.js.map