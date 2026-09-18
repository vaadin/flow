"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _helperPluginUtils = require("@babel/helper-plugin-utils");
var _core = require("@babel/core");
var _default = exports.default = (0, _helperPluginUtils.declare)(api => {
  api.assertVersion(7);
  return {
    name: "transform-instanceof",
    visitor: {
      BinaryExpression(path) {
        const {
          node
        } = path;
        if (node.operator === "instanceof") {
          const helper = this.addHelper("instanceof");
          const isUnderHelper = path.findParent(path => {
            return path.isVariableDeclarator() && path.node.id === helper || path.isFunctionDeclaration() && path.node.id && path.node.id.name === helper.name;
          });
          if (isUnderHelper) {
            return;
          } else {
            path.replaceWith(_core.types.callExpression(helper, [node.left, node.right]));
          }
        }
      }
    }
  };
});

//# sourceMappingURL=index.js.map
