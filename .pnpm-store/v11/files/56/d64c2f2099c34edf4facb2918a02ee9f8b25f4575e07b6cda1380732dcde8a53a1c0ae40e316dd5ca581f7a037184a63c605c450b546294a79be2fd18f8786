"use strict";

function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _iterableToArrayLimit(arr, i) { var _i = arr == null ? null : typeof Symbol !== "undefined" && arr[Symbol.iterator] || arr["@@iterator"]; if (_i == null) return; var _arr = []; var _n = true; var _d = false; var _s, _e; try { for (_i = _i.call(arr); !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

function _createForOfIteratorHelper(o, allowArrayLike) { var it = typeof Symbol !== "undefined" && o[Symbol.iterator] || o["@@iterator"]; if (!it) { if (Array.isArray(o) || (it = _unsupportedIterableToArray(o)) || allowArrayLike && o && typeof o.length === "number") { if (it) o = it; var i = 0; var F = function F() {}; return { s: F, n: function n() { if (i >= o.length) return { done: true }; return { done: false, value: o[i++] }; }, e: function e(_e2) { throw _e2; }, f: F }; } throw new TypeError("Invalid attempt to iterate non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); } var normalCompletion = true, didErr = false, err; return { s: function s() { it = it.call(o); }, n: function n() { var step = it.next(); normalCompletion = step.done; return step; }, e: function e(_e3) { didErr = true; err = _e3; }, f: function f() { try { if (!normalCompletion && it.return != null) it.return(); } finally { if (didErr) throw err; } } }; }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) arr2[i] = arr[i]; return arr2; }

const isPlainObject = require("lodash/isPlainObject"); // the flat plugin map
// This is to prevent dynamic requires - require('babel-plugin-' + name);
// as it suffers during bundling of this code with webpack/browserify
// sorted by option name
// prettier-ignore


const PLUGINS = [// [optionname,         plugin,                                                          default],
["booleans", require("babel-plugin-transform-minify-booleans"), true], ["builtIns", require("babel-plugin-minify-builtins"), true], ["consecutiveAdds", require("babel-plugin-transform-inline-consecutive-adds"), true], ["deadcode", require("babel-plugin-minify-dead-code-elimination"), true], ["evaluate", require("babel-plugin-minify-constant-folding"), true], ["flipComparisons", require("babel-plugin-minify-flip-comparisons"), true], ["guards", require("babel-plugin-minify-guarded-expressions"), true], ["infinity", require("babel-plugin-minify-infinity"), true], ["mangle", require("babel-plugin-minify-mangle-names"), true], ["memberExpressions", require("babel-plugin-transform-member-expression-literals"), true], ["mergeVars", require("babel-plugin-transform-merge-sibling-variables"), true], ["numericLiterals", require("babel-plugin-minify-numeric-literals"), true], ["propertyLiterals", require("babel-plugin-transform-property-literals"), true], ["regexpConstructors", require("babel-plugin-transform-regexp-constructors"), true], ["removeConsole", require("babel-plugin-transform-remove-console"), false], ["removeDebugger", require("babel-plugin-transform-remove-debugger"), false], ["removeUndefined", require("babel-plugin-transform-remove-undefined"), true], ["replace", require("babel-plugin-minify-replace"), true], ["simplify", require("babel-plugin-minify-simplify"), true], ["simplifyComparisons", require("babel-plugin-transform-simplify-comparison-operators"), true], ["typeConstructors", require("babel-plugin-minify-type-constructors"), true], ["undefinedToVoid", require("babel-plugin-transform-undefined-to-void"), true]];
const PROXIES = {
  keepFnName: ["mangle", "deadcode"],
  keepClassName: ["mangle", "deadcode"],
  tdz: ["builtIns", "evaluate", "deadcode", "removeUndefined"]
};
module.exports = preset;

function preset(context, _opts = {}) {
  const opts = isPlainObject(_opts) ? _opts : {}; // validate options

  const validOptions = [...PLUGINS.map(p => p[0]), ...Object.keys(PROXIES)];

  for (let name in opts) {
    if (validOptions.indexOf(name) < 0) {
      throw new Error(`Invalid option "${name}"`);
    }
  } // build a plugins map from the plugin table above


  const pluginsMap = PLUGINS.reduce((acc, [name, plugin, defaultValue]) => Object.assign(acc, {
    [name]: {
      plugin,
      options: null,
      enabled: defaultValue
    }
  }), {}); // handle plugins and their options

  var _iterator = _createForOfIteratorHelper(PLUGINS),
      _step;

  try {
    for (_iterator.s(); !(_step = _iterator.n()).done;) {
      const _step$value = _slicedToArray(_step.value, 1),
            name = _step$value[0];

      if (isPlainObject(opts[name])) {
        // for plugins disabled by default
        pluginsMap[name].enabled = true;
        pluginsMap[name].options = opts[name];
      } else if (opts[name] !== void 0) {
        pluginsMap[name].enabled = !!opts[name];
      }
    } // handle proxies

  } catch (err) {
    _iterator.e(err);
  } finally {
    _iterator.f();
  }

  for (let proxyname in PROXIES) {
    if (opts[proxyname] !== void 0) {
      var _iterator2 = _createForOfIteratorHelper(PROXIES[proxyname]),
          _step2;

      try {
        for (_iterator2.s(); !(_step2 = _iterator2.n()).done;) {
          const to = _step2.value;

          if (!pluginsMap[to].options) {
            pluginsMap[to].options = {};
          }

          if (!hop(pluginsMap[to].options, proxyname)) {
            pluginsMap[to].options[proxyname] = opts[proxyname];
          }
        }
      } catch (err) {
        _iterator2.e(err);
      } finally {
        _iterator2.f();
      }
    }
  } // get the array of plugins


  const plugins = Object.keys(pluginsMap).map(name => pluginsMap[name]).filter(plugin => plugin.enabled).map(plugin => plugin.options ? [plugin.plugin, plugin.options] : plugin.plugin);
  return {
    minified: true,
    presets: [{
      plugins
    }],
    passPerPreset: true
  };
}

function hop(o, key) {
  return Object.prototype.hasOwnProperty.call(o, key);
}