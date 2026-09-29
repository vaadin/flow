var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
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
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);
var FileDiagnosticManager_exports = {};
__export(FileDiagnosticManager_exports, {
  FileDiagnosticManager: () => FileDiagnosticManager
});
module.exports = __toCommonJS(FileDiagnosticManager_exports);
class FileDiagnosticManager {
  constructor() {
    this.diagnostics = [];
  }
  /**
   * Initialize and reset the diagnostics array
   */
  initWith(diagnostics) {
    this.diagnostics = [...diagnostics];
  }
  getDiagnostics(fileName) {
    if (fileName) {
      return this.diagnostics.filter((f) => f.id === fileName);
    }
    return this.diagnostics;
  }
  updateByFileId(fileId, next) {
    this.diagnostics = this.diagnostics.filter((d) => d.id !== fileId);
    if (next == null ? void 0 : next.length) {
      this.diagnostics.push(...next);
    }
  }
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  FileDiagnosticManager
});
//# sourceMappingURL=FileDiagnosticManager.js.map