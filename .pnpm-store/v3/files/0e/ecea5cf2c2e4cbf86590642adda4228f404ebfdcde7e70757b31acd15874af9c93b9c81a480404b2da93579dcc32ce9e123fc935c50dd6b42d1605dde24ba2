import {template}from'@babel/core';import {isModule,addNamed}from'@babel/helper-module-imports';import debug from'debug';function _taggedTemplateLiteralLoose(strings, raw) {
  if (!raw) {
    raw = strings.slice(0);
  }
  strings.raw = raw;
  return strings;
}
function _unsupportedIterableToArray(o, minLen) {
  if (!o) return;
  if (typeof o === "string") return _arrayLikeToArray(o, minLen);
  var n = Object.prototype.toString.call(o).slice(8, -1);
  if (n === "Object" && o.constructor) n = o.constructor.name;
  if (n === "Map" || n === "Set") return Array.from(o);
  if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen);
}
function _arrayLikeToArray(arr, len) {
  if (len == null || len > arr.length) len = arr.length;
  for (var i = 0, arr2 = new Array(len); i < len; i++) arr2[i] = arr[i];
  return arr2;
}
function _createForOfIteratorHelperLoose(o, allowArrayLike) {
  var it = typeof Symbol !== "undefined" && o[Symbol.iterator] || o["@@iterator"];
  if (it) return (it = it.call(o)).next.bind(it);
  if (Array.isArray(o) || (it = _unsupportedIterableToArray(o)) || allowArrayLike && o && typeof o.length === "number") {
    if (it) o = it;
    var i = 0;
    return function () {
      if (i >= o.length) return {
        done: true
      };
      return {
        done: false,
        value: o[i++]
      };
    };
  }
  throw new TypeError("Invalid attempt to iterate non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.");
}var _templateObject;
var optOutCommentIdentifier = /(^|\s)@no(Use|Track)Signals(\s|$)/;
var optInCommentIdentifier = /(^|\s)@(use|track)Signals(\s|$)/;
var dataNamespace = "@preact/signals-react-transform";
var defaultImportSource = "@preact/signals-react/runtime";
var importName = "useSignals";
var getHookIdentifier = "getHookIdentifier";
var maybeUsesSignal = "maybeUsesSignal";
var containsJSX = "containsJSX";
var alreadyTransformed = "alreadyTransformed";
var UNMANAGED = "0";
var MANAGED_COMPONENT = "1";
var MANAGED_HOOK = "2";
var logger = {
  transformed: debug("signals:react-transform:transformed"),
  skipped: debug("signals:react-transform:skipped")
};
var get = function get(pass, name) {
  return pass.get(dataNamespace + "/" + name);
};
var set = function set(pass, name, v) {
  return pass.set(dataNamespace + "/" + name, v);
};
var setData = function setData(node, name, value) {
  return node.setData(dataNamespace + "/" + name, value);
};
var getData = function getData(node, name) {
  return node.getData(dataNamespace + "/" + name);
};
function getComponentFunctionDeclaration(path, filename, prev) {
  var functionScope = path.scope.getFunctionParent();
  if (functionScope) {
    var parent = functionScope.path.parent;
    var functionName = getFunctionName(functionScope.path);
    if (functionName === DefaultExportSymbol) {
      functionName = filename || null;
    }
    if (isComponentFunction(functionScope.path, functionName)) {
      return functionScope;
    } else if (parent.type === "CallExpression" && parent.callee.type === "Identifier" && parent.callee.name.startsWith("use") && parent.callee.name[3] === parent.callee.name[3].toUpperCase()) {
      return null;
    }
    return getComponentFunctionDeclaration(functionScope.parent.path, filename, functionScope);
  } else {
    return prev || null;
  }
}
function setOnFunctionScope(path, key, value, filename) {
  var functionScope = getComponentFunctionDeclaration(path, filename);
  if (functionScope) {
    setData(functionScope, key, value);
  }
}
/**
 * Simple "best effort" to get the base name of a file path. Not fool proof but
 * works in browsers and servers. Good enough for our purposes.
 */
function basename(filename) {
  return filename == null ? void 0 : filename.split(/[\\/]/).pop();
}
var DefaultExportSymbol = Symbol("DefaultExportSymbol");
function getObjectPropertyKey(node) {
  if (node.key.type === "Identifier") {
    return node.key.name;
  } else if (node.key.type === "StringLiteral") {
    return node.key.value;
  }
  return null;
}
/**
 * If the function node has a name (i.e. is a function declaration with a
 * name), return that. Else return null.
 */
function getFunctionNodeName(node) {
  if ((node.type === "FunctionDeclaration" || node.type === "FunctionExpression") && node.id) {
    return node.id.name;
  } else if (node.type === "ObjectMethod") {
    return getObjectPropertyKey(node);
  }
  return null;
}
/**
 * Given a function path's parent path, determine the "name" associated with the
 * function. If the function is an inline default export (e.g. `export default
 * () => {}`), returns a symbol indicating it is a default export. If the
 * function is an anonymous function wrapped in higher order functions (e.g.
 * memo(() => {})) we'll climb through the higher order functions to find the
 * name of the variable that the function is assigned to, if any. Other cases
 * handled too (see implementation). Else returns null.
 */
function getFunctionNameFromParent(parentPath) {
  if (parentPath.node.type === "VariableDeclarator" && parentPath.node.id.type === "Identifier") {
    return parentPath.node.id.name;
  } else if (parentPath.node.type === "AssignmentExpression") {
    var left = parentPath.node.left;
    if (left.type === "Identifier") {
      return left.name;
    } else if (left.type === "MemberExpression") {
      var property = left.property;
      while (property.type === "MemberExpression") {
        property = property.property;
      }
      if (property.type === "Identifier") {
        return property.name;
      } else if (property.type === "StringLiteral") {
        return property.value;
      }
      return null;
    } else {
      return null;
    }
  } else if (parentPath.node.type === "ObjectProperty") {
    return getObjectPropertyKey(parentPath.node);
  } else if (parentPath.node.type === "ExportDefaultDeclaration") {
    return DefaultExportSymbol;
  } else if (parentPath.node.type === "CallExpression" && parentPath.parentPath != null) {
    // If our parent is a Call Expression, then this function expression is
    // wrapped in some higher order functions. Recurse through the higher order
    // functions to determine if this expression is assigned to a name we can
    // use as the function name
    return getFunctionNameFromParent(parentPath.parentPath);
  } else {
    return null;
  }
}
/* Determine the name of a function */
function getFunctionName(path) {
  var nodeName = getFunctionNodeName(path.node);
  if (nodeName) {
    return nodeName;
  }
  return getFunctionNameFromParent(path.parentPath);
}
function isComponentName(name) {
  var _ref;
  return (_ref = (name == null ? void 0 : name.match(/^[A-Z]/)) != null) != null ? _ref : false;
}
function isCustomHookName(name) {
  var _ref2;
  return (_ref2 = (name == null ? void 0 : name.match(/^use[A-Z]/)) != null) != null ? _ref2 : null;
}
function hasLeadingComment(path, comment) {
  var _comments$some;
  var comments = path.node.leadingComments;
  return (_comments$some = comments == null ? void 0 : comments.some(function (c) {
    return c.value.match(comment) !== null;
  })) != null ? _comments$some : false;
}
function hasLeadingOptInComment(path) {
  return hasLeadingComment(path, optInCommentIdentifier);
}
function hasLeadingOptOutComment(path) {
  return hasLeadingComment(path, optOutCommentIdentifier);
}
function isOptedIntoSignalTracking(path) {
  if (!path) return false;
  switch (path.node.type) {
    case "ArrowFunctionExpression":
    case "FunctionExpression":
    case "FunctionDeclaration":
    case "ObjectMethod":
    case "ObjectExpression":
    case "VariableDeclarator":
    case "VariableDeclaration":
    case "AssignmentExpression":
    case "CallExpression":
      return hasLeadingOptInComment(path) || isOptedIntoSignalTracking(path.parentPath);
    case "ExportDefaultDeclaration":
    case "ExportNamedDeclaration":
    case "ObjectProperty":
    case "ExpressionStatement":
      return hasLeadingOptInComment(path);
    default:
      return false;
  }
}
function isOptedOutOfSignalTracking(path) {
  if (!path) return false;
  switch (path.node.type) {
    case "ArrowFunctionExpression":
    case "FunctionExpression":
    case "FunctionDeclaration":
    case "ObjectMethod":
    case "ObjectExpression":
    case "VariableDeclarator":
    case "VariableDeclaration":
    case "AssignmentExpression":
    case "CallExpression":
      return hasLeadingOptOutComment(path) || isOptedOutOfSignalTracking(path.parentPath);
    case "ExportDefaultDeclaration":
    case "ExportNamedDeclaration":
    case "ObjectProperty":
    case "ExpressionStatement":
      return hasLeadingOptOutComment(path);
    default:
      return false;
  }
}
function isComponentFunction(path, functionName) {
  return getData(path.scope, containsJSX) === true &&
  // Function contains JSX
  isComponentName(functionName) // Function name indicates it's a component
  ;
}

function shouldTransform(path, functionName, options) {
  // Opt-out takes first precedence
  if (isOptedOutOfSignalTracking(path)) return false;
  // Opt-in opts in to transformation regardless of mode
  if (isOptedIntoSignalTracking(path)) return true;
  if (options.mode === "all") {
    return isComponentFunction(path, functionName);
  }
  if (options.mode == null || options.mode === "auto") {
    return getData(path.scope, maybeUsesSignal) === true && (
    // Function appears to use signals;
    isComponentFunction(path, functionName) || isCustomHookName(functionName));
  }
  return false;
}
function isValueMemberExpression(path) {
  return path.node.property.type === "Identifier" && path.node.property.name === "value" || path.node.property.type === "StringLiteral" && path.node.property.value === "value";
}
var tryCatchTemplate = template.statements(_templateObject || (_templateObject = _taggedTemplateLiteralLoose(["var STORE_IDENTIFIER = HOOK_IDENTIFIER(HOOK_USAGE);\ntry {\n\tBODY\n} finally {\n\tSTORE_IDENTIFIER.f();\n}"])));
function wrapInTryFinally(t, path, state, hookUsage) {
  var stopTrackingIdentifier = path.scope.generateUidIdentifier("effect");
  return t.blockStatement(tryCatchTemplate({
    STORE_IDENTIFIER: stopTrackingIdentifier,
    HOOK_IDENTIFIER: get(state, getHookIdentifier)(),
    HOOK_USAGE: hookUsage,
    BODY: t.isBlockStatement(path.node.body) ? path.node.body.body // TODO: Is it okay to elide the block statement here?
    : t.returnStatement(path.node.body)
  }));
}
function prependUseSignals(t, path, state) {
  var body = t.blockStatement([t.expressionStatement(t.callExpression(get(state, getHookIdentifier)(), []))]);
  if (t.isBlockStatement(path.node.body)) {
    var _body$body;
    // TODO: Is it okay to elide the block statement here?
    (_body$body = body.body).push.apply(_body$body, path.node.body.body);
  } else {
    body.body.push(t.returnStatement(path.node.body));
  }
  return body;
}
function transformFunction(t, options, path, functionName, state) {
  var _options$experimental;
  var isHook = isCustomHookName(functionName);
  var isComponent = isComponentName(functionName);
  var hookUsage = (_options$experimental = options.experimental) != null && _options$experimental.noTryFinally ? UNMANAGED : isHook ? MANAGED_HOOK : isComponent ? MANAGED_COMPONENT : UNMANAGED;
  var newBody;
  if (hookUsage !== UNMANAGED) {
    newBody = wrapInTryFinally(t, path, state, hookUsage);
  } else {
    newBody = prependUseSignals(t, path, state);
  }
  setData(path, alreadyTransformed, true);
  path.get("body").replaceWith(newBody);
}
function createImportLazily(types, pass, path, importName, source) {
  return function () {
    if (isModule(path)) {
      var reference = get(pass, "imports/" + importName);
      if (reference) return types.cloneNode(reference);
      reference = addNamed(path, importName, source, {
        importedInterop: "uncompiled",
        importPosition: "after"
      });
      set(pass, "imports/" + importName, reference);
      /** Helper function to determine if an import declaration's specifier matches the given importName  */
      var matchesImportName = function matchesImportName(s) {
        if (s.type !== "ImportSpecifier") return false;
        return s.imported.type === "Identifier" && s.imported.name === importName || s.imported.type === "StringLiteral" && s.imported.value === importName;
      };
      for (var _iterator = _createForOfIteratorHelperLoose(path.get("body")), _step; !(_step = _iterator()).done;) {
        var statement = _step.value;
        if (statement.isImportDeclaration() && statement.node.source.value === source && statement.node.specifiers.some(matchesImportName)) {
          path.scope.registerDeclaration(statement);
          break;
        }
      }
      return reference;
    } else {
      // This code originates from
      // https://github.com/XantreDev/preact-signals/blob/%40preact-signals/safe-react%400.6.1/packages/react/src/babel.ts#L390-L400
      var _reference = get(pass, "requires/" + importName);
      if (_reference) {
        _reference = types.cloneNode(_reference);
      } else {
        _reference = addNamed(path, importName, source, {
          importedInterop: "uncompiled"
        });
        set(pass, "requires/" + importName, _reference);
      }
      return _reference;
    }
  };
}
function log(transformed, path, functionName, currentFile) {
  var _currentFile$replace, _path$node$loc, _functionName;
  if (!logger.transformed.enabled && !logger.skipped.enabled) return;
  var cwd = "";
  if (typeof process !== undefined && typeof process.cwd == "function") {
    cwd = process.cwd().replace(/\\([^ ])/g, "/$1");
    cwd = cwd.endsWith("/") ? cwd : cwd + "/";
  }
  var relativePath = (_currentFile$replace = currentFile == null ? void 0 : currentFile.replace(cwd, "")) != null ? _currentFile$replace : "";
  var lineNum = (_path$node$loc = path.node.loc) == null ? void 0 : _path$node$loc.start.line;
  functionName = (_functionName = functionName) != null ? _functionName : "<anonymous>";
  if (transformed) {
    logger.transformed(functionName + " (" + relativePath + ":" + lineNum + ")");
  } else {
    var _getData, _getData2;
    logger.skipped(functionName + " (" + relativePath + ":" + lineNum + ") %o", {
      hasSignals: (_getData = getData(path.scope, maybeUsesSignal)) != null ? _getData : false,
      hasJSX: (_getData2 = getData(path.scope, containsJSX)) != null ? _getData2 : false
    });
  }
}
function isComponentLike(path, functionName) {
  return !getData(path, alreadyTransformed) && isComponentName(functionName);
}
function signalsTransform(_ref3, options) {
  var t = _ref3.types;
  // TODO: Consider alternate implementation, where on enter of a function
  // expression, we run our own manual scan the AST to determine if the
  // function uses signals and is a component. This manual scan once upon
  // seeing a function would probably be faster than running an entire
  // babel pass with plugins on components twice.
  var visitFunction = {
    exit: function exit(path, state) {
      if (getData(path, alreadyTransformed) === true) return false;
      var functionName = getFunctionName(path);
      if (functionName === DefaultExportSymbol) {
        var _basename;
        functionName = (_basename = basename(this.filename)) != null ? _basename : null;
      }
      if (shouldTransform(path, functionName, state.opts)) {
        transformFunction(t, state.opts, path, functionName, state);
        log(true, path, functionName, this.filename);
      } else if (isComponentLike(path, functionName)) {
        log(false, path, functionName, this.filename);
      }
    }
  };
  return {
    name: "@preact/signals-transform",
    visitor: {
      Program: {
        enter: function enter(path, state) {
          var _options$importSource;
          // Following the pattern of babel-plugin-transform-react-jsx, we
          // lazily create the import statement for the useSignalTracking hook.
          // We create a function and store it in the PluginPass object, so that
          // on the first usage of the hook, we can create the import statement.
          set(state, getHookIdentifier, createImportLazily(t, state, path, importName, (_options$importSource = options.importSource) != null ? _options$importSource : defaultImportSource));
        }
      },
      ArrowFunctionExpression: visitFunction,
      FunctionExpression: visitFunction,
      FunctionDeclaration: visitFunction,
      ObjectMethod: visitFunction,
      MemberExpression: function MemberExpression(path) {
        if (isValueMemberExpression(path)) {
          setOnFunctionScope(path, maybeUsesSignal, true, this.filename);
        }
      },
      JSXElement: function JSXElement(path) {
        setOnFunctionScope(path, containsJSX, true, this.filename);
      },
      JSXFragment: function JSXFragment(path) {
        setOnFunctionScope(path, containsJSX, true, this.filename);
      }
    }
  };
}export{signalsTransform as default};//# sourceMappingURL=signals-transform.module.js.map
