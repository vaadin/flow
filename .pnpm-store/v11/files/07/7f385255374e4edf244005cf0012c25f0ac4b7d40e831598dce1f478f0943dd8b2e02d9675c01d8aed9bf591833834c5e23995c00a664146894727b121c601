"use strict";

function _createForOfIteratorHelper(o, allowArrayLike) { var it = typeof Symbol !== "undefined" && o[Symbol.iterator] || o["@@iterator"]; if (!it) { if (Array.isArray(o) || (it = _unsupportedIterableToArray(o)) || allowArrayLike && o && typeof o.length === "number") { if (it) o = it; var i = 0; var F = function F() {}; return { s: F, n: function n() { if (i >= o.length) return { done: true }; return { done: false, value: o[i++] }; }, e: function e(_e) { throw _e; }, f: F }; } throw new TypeError("Invalid attempt to iterate non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); } var normalCompletion = true, didErr = false, err; return { s: function s() { it = it.call(o); }, n: function n() { var step = it.next(); normalCompletion = step.done; return step; }, e: function e(_e2) { didErr = true; err = _e2; }, f: function f() { try { if (!normalCompletion && it.return != null) it.return(); } finally { if (didErr) throw err; } } }; }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) arr2[i] = arr[i]; return arr2; }

module.exports = function bfsTraverseCreator({
  types: t,
  traverse
}) {
  function getFields(path) {
    return t.VISITOR_KEYS[path.type];
  }

  return function bfsTraverse(path, _visitor) {
    if (!path.node) {
      throw new Error("Not a valid path");
    }

    const visitor = traverse.explode(_visitor);
    const queue = [path];
    let current;

    while (queue.length > 0) {
      current = queue.shift(); // call

      if (visitor && visitor[current.type] && Array.isArray(visitor[current.type].enter)) {
        const fns = visitor[current.type].enter;

        var _iterator = _createForOfIteratorHelper(fns),
            _step;

        try {
          for (_iterator.s(); !(_step = _iterator.n()).done;) {
            const fn = _step.value;
            if (typeof fn === "function") fn(current);
          }
        } catch (err) {
          _iterator.e(err);
        } finally {
          _iterator.f();
        }
      }

      const fields = getFields(current);

      var _iterator2 = _createForOfIteratorHelper(fields),
          _step2;

      try {
        for (_iterator2.s(); !(_step2 = _iterator2.n()).done;) {
          const field = _step2.value;
          const child = current.get(field);

          if (Array.isArray(child)) {
            // visit container left to right
            var _iterator3 = _createForOfIteratorHelper(child),
                _step3;

            try {
              for (_iterator3.s(); !(_step3 = _iterator3.n()).done;) {
                const c = _step3.value;
                if (c.node) queue.push(c);
              }
            } catch (err) {
              _iterator3.e(err);
            } finally {
              _iterator3.f();
            }
          } else {
            if (child.node) queue.push(child);
          }
        }
      } catch (err) {
        _iterator2.e(err);
      } finally {
        _iterator2.f();
      }
    }
  };
};