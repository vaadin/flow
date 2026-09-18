import {template,types}from'@babel/core';import {isModule,addNamed}from'@babel/helper-module-imports';import debug from'debug';let _ = t => t,
  _t;
const optOutCommentIdentifier = /(^|\s)@no(Use|Track)Signals(\s|$)/;
const optInCommentIdentifier = /(^|\s)@(use|track)Signals(\s|$)/;
const dataNamespace = "@preact/signals-react-transform";
const defaultImportSource = "@preact/signals-react/runtime";
const importName = "useSignals";
const getHookIdentifier = "getHookIdentifier";
const maybeUsesSignal = "maybeUsesSignal";
const containsJSX = "containsJSX";
const alreadyTransformed = "alreadyTransformed";
const jsxIdentifiers = "jsxIdentifiers";
const jsxObjects = "jsxObjects";
const UNMANAGED = "0";
const MANAGED_COMPONENT = "1";
const MANAGED_HOOK = "2";
const logger = {
  verbose: debug("signals:react-transform:verbose"),
  fnSearch: debug("signals:react-transform:fn-search"),
  transformed: debug("signals:react-transform:transformed"),
  skipped: debug("signals:react-transform:skipped")
};
const get = (pass, name) => pass.get(`${dataNamespace}/${name}`);
const set = (pass, name, v) => pass.set(`${dataNamespace}/${name}`, v);
const setNodeData = (node, name, value) => node.setData(`${dataNamespace}/${name}`, value);
const getNodeData = (node, name) => node.getData(`${dataNamespace}/${name}`);
/**
 * Returns the containing component or hook function path. Examples:
 * ```
 * function App() {               <- returns this path
 *   <div>{signal.value}</div>    <- starting from this
 * }
 *
 * function useCustomHook() {     <- returns this path
 *   <div>{signal.value}</div>    <- starting from this
 * }
 * ```
 *
 * It will return `null` if the function is passed as a parameter
 * to a custom hook function. Example:
 * ```
 * function Component() {
 *   useCustomHook(() => {
 *     <div>{signal.value}</div>    <- returns null
 *   });
 * }
 * ```
 */
function findParentComponentOrHook(path, filename) {
  const parentFunctionScope = path.scope.getFunctionParent();
  if (!parentFunctionScope) {
    logger.fnSearch("No higher function scope found in %s", filename);
    return null;
  }
  const parentFunctionPath = parentFunctionScope.path;
  const fnName = getFunctionName(parentFunctionPath, filename);
  logger.fnSearch('Checking parent function "%s" in %s', fnName, filename);
  if (isComponentName(fnName) || isCustomHookName(fnName)) {
    logger.fnSearch('Found parent function "%s" in %s', fnName, filename);
    return parentFunctionPath;
  } else if (isCustomHookCallback(parentFunctionPath)) {
    logger.fnSearch('Function "%s" is a hook callback arg, stopping', fnName);
    return null;
  } else if (!parentFunctionPath.parentPath) {
    logger.fnSearch('Function "%s" has no parent, stopping', fnName, filename);
    return null;
  }
  return findParentComponentOrHook(parentFunctionPath.parentPath, filename);
}
function setOnParentComponentOrHook(path, key, value, filename) {
  const parentFn = findParentComponentOrHook(path, filename);
  if (parentFn) {
    if (logger.verbose.enabled) {
      const fnName = getFunctionName(parentFn, filename);
      logger.verbose(`Setting "${key}" on "${fnName}" to "${value}"`);
    }
    setNodeData(parentFn, key, value);
  }
}
/**
 * Simple "best effort" to get the base name of a file path. Not fool proof but
 * works in browsers and servers. Good enough for our purposes.
 */
function basename(filename) {
  return filename == null ? void 0 : filename.split(/[\\/]/).pop();
}
const DefaultExportSymbol = Symbol("DefaultExportSymbol");
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
    const left = parentPath.node.left;
    if (left.type === "Identifier") {
      return left.name;
    } else if (left.type === "MemberExpression") {
      let property = left.property;
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
function getFunctionName(path, filename) {
  let fnName = getFunctionNodeName(path.node);
  if (fnName) {
    return fnName;
  }
  const nameFromParent = getFunctionNameFromParent(path.parentPath);
  if (nameFromParent === DefaultExportSymbol) {
    var _basename;
    fnName = (_basename = basename(filename)) != null ? _basename : null;
  } else {
    fnName = nameFromParent;
  }
  return fnName;
}
function isComponentName(name) {
  return (name == null ? void 0 : name.match(/^[A-Z]/)) != null;
}
function isCustomHookName(name) {
  return (name == null ? void 0 : name.match(/^use[A-Z]/)) != null;
}
/** Returns if the given function path is a parameter passed to a custom hook function */
function isCustomHookCallback(path) {
  const parent = path.parent;
  return parent.type === "CallExpression" && parent.callee.type === "Identifier" && isCustomHookName(parent.callee.name);
}
function hasLeadingComment(path, comment) {
  var _comments$some;
  const comments = path.node.leadingComments;
  return (_comments$some = comments == null ? void 0 : comments.some(c => c.value.match(comment) !== null)) != null ? _comments$some : false;
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
function shouldTransform(path, functionName, options) {
  // This function should only be called after a function's body has been parsed
  // and containsJSX and maybeUsesSignal could be set
  function isComponentFunction(path, functionName) {
    return getNodeData(path, containsJSX) === true &&
    // Function contains JSX
    isComponentName(functionName) // Function name indicates it's a component
    ;
  }
  // Opt-out takes first precedence
  if (isOptedOutOfSignalTracking(path)) return false;
  // Opt-in opts in to transformation regardless of mode
  if (isOptedIntoSignalTracking(path)) return true;
  if (options.mode === "all") {
    return isComponentFunction(path, functionName);
  }
  if (options.mode == null || options.mode === "auto") {
    return getNodeData(path, maybeUsesSignal) === true && (
    // Function appears to use signals;
    isComponentFunction(path, functionName) || isCustomHookName(functionName));
  }
  return false;
}
function isValueMemberExpression(path) {
  return path.node.property.type === "Identifier" && path.node.property.name === "value" || path.node.property.type === "StringLiteral" && path.node.property.value === "value";
}
function isJSXAlternativeCall(path, state) {
  const jsxIdentifierSet = get(state, jsxIdentifiers);
  const jsxObjectMap = get(state, jsxObjects);
  const callee = path.get("callee");
  // Check direct function calls like _jsx("div", props) or createElement("div", props)
  if (callee.isIdentifier()) {
    var _jsxIdentifierSet$has;
    return (_jsxIdentifierSet$has = jsxIdentifierSet == null ? void 0 : jsxIdentifierSet.has(callee.node.name)) != null ? _jsxIdentifierSet$has : false;
  }
  // Check member expression calls like React.createElement("div", props) or jsxRuntime.jsx("div", props)
  if (callee.isMemberExpression()) {
    const object = callee.get("object");
    const property = callee.get("property");
    if (object.isIdentifier() && property.isIdentifier()) {
      var _allowedMethods$inclu;
      const objectName = object.node.name;
      const methodName = property.node.name;
      const allowedMethods = jsxObjectMap == null ? void 0 : jsxObjectMap.get(objectName);
      return (_allowedMethods$inclu = allowedMethods == null ? void 0 : allowedMethods.includes(methodName)) != null ? _allowedMethods$inclu : false;
    }
  }
  return false;
}
function isSignalCall(path) {
  const callee = path.get("callee");
  // Check direct calls to APIs that accept debug names.
  if (callee.isIdentifier()) {
    const name = callee.node.name;
    return name === "signal" || name === "computed" || name === "effect" || name === "useSignal" || name === "useComputed" || name === "useSignalEffect";
  }
  return false;
}
function getStaticName(node, computed = false) {
  if (!computed && node.type === "Identifier") {
    return node.name;
  } else if (!computed && node.type === "PrivateName") {
    return `#${node.id.name}`;
  } else if (node.type === "StringLiteral" || node.type === "NumericLiteral") {
    return String(node.value);
  }
  return null;
}
function hasComputedKey(node) {
  return "computed" in node && node.computed === true;
}
function getAssignmentName(node) {
  if (node.type === "Identifier") {
    return node.name;
  } else if (node.type === "MemberExpression") {
    return getStaticName(node.property, node.computed);
  }
  return null;
}
function getFunctionExpressionName(path) {
  const parentPath = path.parentPath;
  if (!parentPath) return null;
  if (parentPath.isVariableDeclarator()) {
    return parentPath.node.id.type === "Identifier" ? parentPath.node.id.name : null;
  } else if (parentPath.isAssignmentExpression()) {
    return getAssignmentName(parentPath.node.left);
  } else if (parentPath.isObjectProperty() || parentPath.isClassProperty() || parentPath.isClassPrivateProperty()) {
    return getStaticName(parentPath.node.key, hasComputedKey(parentPath.node));
  }
  return null;
}
function getSignalNameFromContext(path) {
  let currentPath = path.parentPath;
  while (currentPath) {
    if (currentPath.isArrowFunctionExpression() || currentPath.isFunctionExpression() && !currentPath.node.id) {
      const name = getFunctionExpressionName(currentPath);
      if (name) return name;
      break;
    } else if (currentPath.isVariableDeclarator() && currentPath.node.id.type === "Identifier") {
      return currentPath.node.id.name;
    } else if (currentPath.isAssignmentExpression()) {
      const name = getAssignmentName(currentPath.node.left);
      if (name) return name;
      break;
    } else if (currentPath.isObjectProperty()) {
      const name = getStaticName(currentPath.node.key, hasComputedKey(currentPath.node));
      if (name) return name;
      break;
    } else if (currentPath.isClassProperty() || currentPath.isClassPrivateProperty()) {
      const name = getStaticName(currentPath.node.key, hasComputedKey(currentPath.node));
      if (name) return name;
      break;
    } else if ((currentPath.isFunctionDeclaration() || currentPath.isFunctionExpression()) && currentPath.node.id) {
      return currentPath.node.id.name;
    } else if (currentPath.isObjectMethod() || currentPath.isClassMethod() || currentPath.isClassPrivateMethod()) {
      const name = getStaticName(currentPath.node.key, hasComputedKey(currentPath.node));
      if (name) return name;
      break;
    }
    currentPath = currentPath.parentPath;
  }
  return null;
}
function getSignalName(path, filename) {
  var _path$node$loc;
  const contextName = getSignalNameFromContext(path);
  const baseName = basename(filename);
  const lineNumber = (_path$node$loc = path.node.loc) == null ? void 0 : _path$node$loc.start.line;
  if (baseName && lineNumber) {
    return contextName ? `${contextName} (${baseName}:${lineNumber})` : `${baseName}:${lineNumber}`;
  }
  return contextName;
}
function shouldSkipNameInjection(t, args) {
  if (args.length < 2) return false;
  const optionsArg = args[1];
  if (!optionsArg.isObjectExpression()) {
    // Non-literal options cannot be safely extended without changing semantics.
    return true;
  }
  return optionsArg.node.properties.some(prop => {
    if (t.isSpreadElement(prop)) return true;
    if (!t.isObjectProperty(prop)) return false;
    const key = getStaticName(prop.key, prop.computed);
    return key === "name" || key === null && prop.computed;
  });
}
function injectSignalName(t, path, nameValue) {
  const args = path.get("arguments");
  const name = t.stringLiteral(nameValue);
  if (args.length === 0) {
    // No arguments, add both value and options
    const nameOption = t.objectExpression([t.objectProperty(t.identifier("name"), name)]);
    path.node.arguments.push(t.identifier("undefined"), nameOption);
  } else if (args.length === 1) {
    // One argument (value), add options object
    const nameOption = t.objectExpression([t.objectProperty(t.identifier("name"), name)]);
    path.node.arguments.push(nameOption);
  } else if (args.length >= 2) {
    // Two or more arguments, modify existing literal options
    const optionsArg = args[1];
    if (optionsArg.isObjectExpression()) {
      optionsArg.node.properties.push(t.objectProperty(t.identifier("name"), name));
    }
  }
}
function hasValuePropertyInPattern(pattern) {
  for (const property of pattern.properties) {
    if (types.isObjectProperty(property)) {
      const key = property.key;
      if (types.isIdentifier(key, {
        name: "value"
      })) {
        return true;
      }
    }
  }
  return false;
}
const tryCatchTemplate = template.statements(_t || (_t = _`var STORE_IDENTIFIER = HOOK_CALL;
try {
	BODY
} finally {
	STORE_IDENTIFIER.f();
}`));
function wrapInTryFinally(t, path, state, hookUsage, options, functionName) {
  var _options$experimental;
  const stopTrackingIdentifier = path.scope.generateUidIdentifier("effect");
  // Build the useSignals call with optional component name
  const args = [t.numericLiteral(parseInt(hookUsage))];
  if ((_options$experimental = options.experimental) != null && _options$experimental.debug && functionName) {
    args.push(t.stringLiteral(functionName));
  }
  const hookCall = t.callExpression(get(state, getHookIdentifier)(), args);
  const statements = tryCatchTemplate({
    STORE_IDENTIFIER: stopTrackingIdentifier,
    HOOK_CALL: hookCall,
    BODY: t.isBlockStatement(path.node.body) ? path.node.body.body : t.returnStatement(path.node.body)
  });
  return t.blockStatement(statements);
}
function prependUseSignals(t, path, state, options, functionName) {
  var _options$experimental2;
  const args = [];
  if ((_options$experimental2 = options.experimental) != null && _options$experimental2.debug && functionName) {
    // useSignals(usage, componentName)
    // When debug is enabled, we need to pass undefined for usage and the component name
    args.push(t.identifier("undefined"));
    args.push(t.stringLiteral(functionName));
  }
  const body = t.blockStatement([t.expressionStatement(t.callExpression(get(state, getHookIdentifier)(), args))]);
  if (t.isBlockStatement(path.node.body)) {
    // TODO: Is it okay to elide the block statement here?
    body.body.push(...path.node.body.body);
  } else {
    body.body.push(t.returnStatement(path.node.body));
  }
  return body;
}
function transformFunction(t, options, path, functionName, state) {
  var _options$experimental3;
  const isHook = isCustomHookName(functionName);
  const isComponent = isComponentName(functionName);
  const hookUsage = (_options$experimental3 = options.experimental) != null && _options$experimental3.noTryFinally ? UNMANAGED : isHook ? MANAGED_HOOK : isComponent ? MANAGED_COMPONENT : UNMANAGED;
  let newBody;
  if (hookUsage !== UNMANAGED) {
    newBody = wrapInTryFinally(t, path, state, hookUsage, options, functionName);
  } else {
    newBody = prependUseSignals(t, path, state, options, functionName);
  }
  setNodeData(path, alreadyTransformed, true);
  path.get("body").replaceWith(newBody);
}
function createImportLazily(types, pass, path, importName, source) {
  return () => {
    if (isModule(path)) {
      let reference = get(pass, `imports/${importName}`);
      if (reference) return types.cloneNode(reference);
      reference = addNamed(path, importName, source, {
        importedInterop: "uncompiled",
        importPosition: "after"
      });
      set(pass, `imports/${importName}`, reference);
      /** Helper function to determine if an import declaration's specifier matches the given importName  */
      const matchesImportName = s => {
        if (s.type !== "ImportSpecifier") return false;
        return s.imported.type === "Identifier" && s.imported.name === importName || s.imported.type === "StringLiteral" && s.imported.value === importName;
      };
      for (let statement of path.get("body")) {
        if (statement.isImportDeclaration() && statement.node.source.value === source && statement.node.specifiers.some(matchesImportName)) {
          path.scope.registerDeclaration(statement);
          break;
        }
      }
      return reference;
    } else {
      // This code originates from
      // https://github.com/XantreDev/preact-signals/blob/%40preact-signals/safe-react%400.6.1/packages/react/src/babel.ts#L390-L400
      let reference = get(pass, `requires/${importName}`);
      if (reference) {
        reference = types.cloneNode(reference);
      } else {
        reference = addNamed(path, importName, source, {
          importedInterop: "uncompiled"
        });
        set(pass, `requires/${importName}`, reference);
      }
      return reference;
    }
  };
}
function detectJSXAlternativeImports(path, state) {
  const jsxIdentifierSet = new Set();
  const jsxObjectMap = new Map();
  const jsxPackages = {
    "react/jsx-runtime": ["jsx", "jsxs"],
    "react/jsx-dev-runtime": ["jsxDEV"],
    react: ["createElement"]
  };
  path.traverse({
    ImportDeclaration(importPath) {
      const packageName = importPath.node.source.value;
      const jsxMethods = jsxPackages[packageName];
      if (!jsxMethods) {
        return;
      }
      for (const specifier of importPath.node.specifiers) {
        if (specifier.type === "ImportSpecifier" && specifier.imported.type === "Identifier") {
          // Check if this is a function we care about
          if (jsxMethods.includes(specifier.imported.name)) {
            jsxIdentifierSet.add(specifier.local.name);
          }
        } else if (specifier.type === "ImportDefaultSpecifier") {
          // Handle default imports - add to objects map for member access
          jsxObjectMap.set(specifier.local.name, jsxMethods);
        }
      }
    },
    VariableDeclarator(varPath) {
      const init = varPath.get("init");
      if (init.isCallExpression()) {
        const callee = init.get("callee");
        const args = init.get("arguments");
        if (callee.isIdentifier() && callee.node.type === "Identifier" && callee.node.name === "require" && args.length > 0 && args[0].isStringLiteral()) {
          const packageName = args[0].node.value;
          const jsxMethods = jsxPackages[packageName];
          if (jsxMethods) {
            if (varPath.node.id.type === "Identifier") {
              // Handle CJS require like: const React = require("react")
              jsxObjectMap.set(varPath.node.id.name, jsxMethods);
            } else if (varPath.node.id.type === "ObjectPattern") {
              // Handle destructured CJS require like: const { createElement } = require("react")
              for (const prop of varPath.node.id.properties) {
                if (prop.type === "ObjectProperty" && prop.key.type === "Identifier" && prop.value.type === "Identifier" && jsxMethods.includes(prop.key.name)) {
                  jsxIdentifierSet.add(prop.value.name);
                }
              }
            }
          }
        }
      }
    }
  });
  logger.verbose("Using JSX alternatives: %o", {
    identifiers: Array.from(jsxIdentifierSet),
    objects: Array.from(jsxObjectMap.entries())
  });
  set(state, jsxIdentifiers, jsxIdentifierSet);
  set(state, jsxObjects, jsxObjectMap);
}
function log(transformed, path, functionName, currentFile) {
  var _currentFile$replace, _path$node$loc2;
  if (!logger.transformed.enabled && !logger.skipped.enabled) return;
  let cwd = "";
  if (typeof process !== undefined && typeof process.cwd == "function") {
    cwd = process.cwd().replace(/\\([^ ])/g, "/$1");
    cwd = cwd.endsWith("/") ? cwd : cwd + "/";
  }
  const relativePath = (_currentFile$replace = currentFile == null ? void 0 : currentFile.replace(cwd, "")) != null ? _currentFile$replace : "";
  const lineNum = (_path$node$loc2 = path.node.loc) == null ? void 0 : _path$node$loc2.start.line;
  functionName = functionName != null ? functionName : "<anonymous>";
  if (transformed) {
    logger.transformed(`${functionName} (${relativePath}:${lineNum})`);
  } else {
    var _getNodeData, _getNodeData2;
    logger.skipped(`${functionName} (${relativePath}:${lineNum}) %o`, {
      hasSignals: (_getNodeData = getNodeData(path, maybeUsesSignal)) != null ? _getNodeData : false,
      hasJSX: (_getNodeData2 = getNodeData(path, containsJSX)) != null ? _getNodeData2 : false
    });
  }
}
function signalsTransform({
  types: t
}, options) {
  // TODO: Consider alternate implementation, where on enter of a function
  // expression, we run our own manual scan the AST to determine if the
  // function uses signals and is a component. This manual scan once upon
  // seeing a function would probably be faster than running an entire
  // babel pass with plugins on components twice.
  const visitFunction = {
    exit(path, state) {
      if (getNodeData(path, alreadyTransformed) === true) return false;
      const functionName = getFunctionName(path, this.filename);
      const isComponentLike = !getNodeData(path, alreadyTransformed) && isComponentName(functionName);
      if (shouldTransform(path, functionName, state.opts)) {
        transformFunction(t, state.opts, path, functionName, state);
        log(true, path, functionName, this.filename);
      } else if (isComponentLike) {
        log(false, path, functionName, this.filename);
      }
    }
  };
  return {
    name: "@preact/signals-transform",
    visitor: {
      Program: {
        enter(path, state) {
          var _options$importSource;
          // Following the pattern of babel-plugin-transform-react-jsx, we
          // lazily create the import statement for the useSignalTracking hook.
          // We create a function and store it in the PluginPass object, so that
          // on the first usage of the hook, we can create the import statement.
          set(state, getHookIdentifier, createImportLazily(t, state, path, importName, (_options$importSource = options.importSource) != null ? _options$importSource : defaultImportSource));
          if (options.detectTransformedJSX) {
            detectJSXAlternativeImports(path, state);
          }
        }
      },
      ArrowFunctionExpression: visitFunction,
      FunctionExpression: visitFunction,
      FunctionDeclaration: visitFunction,
      ObjectMethod: visitFunction,
      CallExpression(path, state) {
        var _options$experimental4;
        if (options.detectTransformedJSX) {
          if (isJSXAlternativeCall(path, state)) {
            setOnParentComponentOrHook(path, containsJSX, true, this.filename);
          }
        }
        // Handle signal naming
        if ((_options$experimental4 = options.experimental) != null && _options$experimental4.debug && isSignalCall(path)) {
          const args = path.get("arguments");
          // Only inject name if it doesn't already have one
          if (!shouldSkipNameInjection(t, args)) {
            const signalName = getSignalName(path, this.filename);
            if (signalName) injectSignalName(t, path, signalName);
          }
        }
      },
      MemberExpression(path) {
        if (isValueMemberExpression(path)) {
          setOnParentComponentOrHook(path, maybeUsesSignal, true, this.filename);
        }
      },
      ObjectPattern(path) {
        if (hasValuePropertyInPattern(path.node)) {
          setOnParentComponentOrHook(path, maybeUsesSignal, true, this.filename);
        }
      },
      JSXElement(path) {
        setOnParentComponentOrHook(path, containsJSX, true, this.filename);
      },
      JSXFragment(path) {
        setOnParentComponentOrHook(path, containsJSX, true, this.filename);
      }
    }
  };
}export{signalsTransform as default};//# sourceMappingURL=signals-transform.mjs.map
