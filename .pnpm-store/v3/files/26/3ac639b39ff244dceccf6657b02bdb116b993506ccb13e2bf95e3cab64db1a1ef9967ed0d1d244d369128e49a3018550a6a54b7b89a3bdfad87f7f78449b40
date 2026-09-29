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
var Checker_exports = {};
__export(Checker_exports, {
  Checker: () => Checker
});
module.exports = __toCommonJS(Checker_exports);
var import_tiny_invariant = __toESM(require("tiny-invariant"), 1);
var import_utils = require("./utils.js");
var import_worker = require("./worker.js");
if (!(import_utils.isMainThread || import_utils.isInVitestEntryThread)) {
  process.stdout.isTTY = true;
}
const _Checker = class {
  static log(...args) {
    for (const fn of _Checker.logger) {
      fn(...args);
    }
  }
  constructor({
    name,
    absFilePath,
    createDiagnostic,
    build
  }) {
    this.name = name;
    this.absFilePath = absFilePath;
    this.build = build;
    this.createDiagnostic = createDiagnostic;
    this.build = build;
  }
  prepare() {
    const script = (0, import_worker.createScript)({
      absFilename: this.absFilePath,
      buildBin: this.build.buildBin,
      serverChecker: { createDiagnostic: this.createDiagnostic }
    });
    this.script = script;
    return script;
  }
  initMainThread() {
    (0, import_tiny_invariant.default)(
      this.script,
      `script should be created in 'prepare', but got ${this.script}`
    );
    if (import_utils.isMainThread || import_utils.isInVitestEntryThread) {
      const createServeAndBuild = this.script.mainScript();
      return createServeAndBuild;
    }
    return;
  }
  initWorkerThread() {
    (0, import_tiny_invariant.default)(
      this.script,
      `script should be created in 'prepare', but got ${this.script}`
    );
    if (!(import_utils.isMainThread || import_utils.isInVitestEntryThread)) {
      this.script.workerScript();
    }
  }
};
let Checker = _Checker;
Checker.logger = [];
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  Checker
});
//# sourceMappingURL=Checker.js.map