"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = getImportMetaResolve;

function _module() {
  const data = require("module");

  _module = function () {
    return data;
  };

  return data;
}

var _importMetaResolve = require("../../vendor/import-meta-resolve");

let import_;

try {
  import_ = require("./import").default;
} catch (_unused) {}

const resolveP = import_ && !Object.hasOwnProperty.call(global, "jest-symbol-do-not-touch") ? import_("data:text/javascript,export default import.meta.resolve").then(m => m.default || _importMetaResolve.resolve, () => _importMetaResolve.resolve) : Promise.resolve(_importMetaResolve.resolve);

function getImportMetaResolve() {
  return resolveP;
}