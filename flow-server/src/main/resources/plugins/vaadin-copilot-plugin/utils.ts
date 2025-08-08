import ts from 'typescript';
import * as path from 'path';
import fs from 'fs';
import type { TsContext } from './types';

/**
 * Parses the tsconfig.json and get parsed command line object. Throws an exception if tsconfig.json is unable to parsed.
 */
function resolveTsConfigAndGetParsed() {
  const configPath = ts.findConfigFile('./', ts.sys.fileExists, 'tsconfig.json');
  if (!configPath) throw new Error('tsconfig.json not found.');
  const config = ts.readConfigFile(configPath, ts.sys.readFile);
  return ts.parseJsonConfigFileContent(config.config, ts.sys, path.dirname(configPath));
}

/**
 * Recursively searches the TypeScript AST for the most specific node
 * at a given position that satisfies a provided filter function.
 *
 * @param node - The root node from which to start the search.
 * @param pos - The position (as a character index) in the source file to search for.
 * @param filter - A predicate function to determine whether a node is a valid match.
 * @returns The deepest `ts.Node` that contains the position and passes the filter,
 *          or `undefined` if no matching node is found.
 */
export function findNodeAt(node: ts.Node, pos: number, filter: (filterNode: ts.Node) => boolean): ts.Node | undefined {
  // If a child matches, it is always a better match
  try {
    for (const child of node.getChildren()) {
      const match = findNodeAt(child, pos, filter);
      if (match) {
        return match;
      }
    }
  } catch (err) {
    console.error(err);
  }

  // If no child matches, this can be the best match
  // Note that the position is so that the previous node ends at the same positioon the next starts so
  // if we are looking for pos 100 we should match the node that starts at 100 and not the one that ends at 100
  if (node.getStart() <= pos && node.getEnd() > pos && filter(node)) {
    return node;
  }

  return undefined;
}

const filePathLastModifiedTimestampCacheMap = new Map<string, number>();
const filePathTsContextPath = new Map<string, TsContext>();

/**
 * Creates a ts program for the given file using the Typescript Compiler API and returns the context that is required for any kind of TS manipulation.
 *
 * @param filePath fully qualified file
 * @param useCache TS Context would be served from the cache if the given file last modified date is the same as in the cache. It is true by default
 * @throws Error when source file is not found
 * @throws Error when ts.config file is not found
 */
export function getTsContext(filePath: string, useCache = true): TsContext {
  if (useCache) {
    const fileLastModifiedTimeInMs = getFileLastModifiedTimeInMs(filePath);
    if (
      filePathLastModifiedTimestampCacheMap.has(filePath) &&
      filePathLastModifiedTimestampCacheMap.get(filePath) === fileLastModifiedTimeInMs &&
      filePathTsContextPath.has(filePath) &&
      filePathTsContextPath.get(filePath)
    ) {
      // The cached file is up-to-date
      return filePathTsContextPath.get(filePath)!;
    }
  }
  const parsedCommandLine = resolveTsConfigAndGetParsed();
  const program = ts.createProgram({
    rootNames: parsedCommandLine.fileNames,
    options: parsedCommandLine.options,
  });
  const sourceFile = program.getSourceFile(filePath);
  if (!sourceFile) throw new Error('source file not found.');
  const context = { program, checker: program.getTypeChecker(), sourceFile, config: parsedCommandLine, filePath };
  if (useCache) {
    filePathLastModifiedTimestampCacheMap.set(filePath, getFileLastModifiedTimeInMs(filePath));
    filePathTsContextPath.set(filePath, context);
  }
  return context;
}

/**
 * A generic response wrapper for API results.
 *
 * This interface is designed to standardize API responses by encapsulating
 * the success or failure state of a request along with the result body.
 *
 * @typeParam T - The type of the data returned in the response body.
 *
 * @property error - Indicates whether an error occurred. If omitted, it's assumed to be `false`.
 * @property errorMessage - Optional descriptive message if an error occurred.
 * @property body - The response payload when the request is successful.
 *
 * @example
 * ```ts
 * const successResponse: GenericResponse<User> = {
 *   body: { id: 1, name: "Alice" }
 * };
 *
 * const errorResponse: GenericResponse<null> = {
 *   error: true,
 *   errorMessage: "User not found"
 * };
 * ```
 */
export interface GenericResponse<T> {
  error?: boolean;
  errorMessage?: string;
  body?: T;
}

/**
 * Utility method that helps the user to know the type of object while developing.
 * It does byte-wise operations for the given type and returns a string
 * @param type
 */
export function describeType(type: ts.Type): string {
  const flags = ts.TypeFlags;
  const objFlags = ts.ObjectFlags;

  if (type.flags & flags.Any) return 'Any';
  if (type.flags & flags.Unknown) return 'Unknown';
  if (type.flags & flags.String) return 'String';
  if (type.flags & flags.Number) return 'Number';
  if (type.flags & flags.Boolean) return 'Boolean';
  if (type.flags & flags.BigInt) return 'BigInt';
  if (type.flags & flags.StringLiteral) return 'StringLiteral';
  if (type.flags & flags.NumberLiteral) return 'NumberLiteral';
  if (type.flags & flags.BooleanLiteral) return 'BooleanLiteral';
  if (type.flags & flags.BigIntLiteral) return 'BigIntLiteral';
  if (type.flags & flags.Enum) return 'Enum';
  if (type.flags & flags.EnumLiteral) return 'EnumLiteral';
  if (type.flags & flags.ESSymbol) return 'Symbol';
  if (type.flags & flags.UniqueESSymbol) return 'UniqueSymbol';
  if (type.flags & flags.Void) return 'Void';
  if (type.flags & flags.Undefined) return 'Undefined';
  if (type.flags & flags.Null) return 'Null';
  if (type.flags & flags.Never) return 'Never';
  if (type.flags & flags.NonPrimitive) return 'NonPrimitive';
  if (type.flags & flags.Object) {
    const objType = type as ts.ObjectType;

    if (objType.objectFlags & objFlags.Class) return 'Class';
    if (objType.objectFlags & objFlags.Interface) return 'Interface';
    if (objType.objectFlags & objFlags.Anonymous) return 'Anonymous Object';
    if (objType.objectFlags & objFlags.Reference) return 'Generic Instance';
    if (objType.objectFlags & objFlags.Tuple) return 'Tuple';
    if (objType.objectFlags & objFlags.Mapped) return 'Mapped Type';
    if (objType.objectFlags & objFlags.Instantiated) return 'Instantiated Type';
    if (objType.objectFlags & objFlags.ReverseMapped) return 'ReverseMapped Type';
    return 'Object';
  }
  if (type.flags & flags.Union) return 'Union';
  if (type.flags & flags.Intersection) return 'Intersection';
  if (type.flags & flags.Index) return 'Index Type';
  if (type.flags & flags.IndexedAccess) return 'IndexedAccess Type';
  if (type.flags & flags.TypeParameter) return 'Type Parameter';
  if (type.flags & flags.TypeVariable) return 'Type Variable';

  return 'Other';
}

/**
 * Describes a symbol using the TypeScript compiler API. A utility method for debugging
 *
 * @param symbol - The TypeScript symbol to describe.
 * @param checker - The type checker instance from your program.
 * @returns An object containing information about the symbol.
 */
// @ts-expect-error
function describeSymbol(symbol: ts.Symbol, checker: ts.TypeChecker) {
  const description: {
    name: string;
    aliasedTo?: string;
    declarations: Array<{
      file: string;
      line: number;
      kind: string;
    }>;
    isImported: boolean;
    typeString?: string;
  } = {
    name: symbol.getName(),
    declarations: [],
    isImported: false,
  };

  // Resolve alias
  if (symbol.flags & ts.SymbolFlags.Alias) {
    const target = checker.getAliasedSymbol(symbol);
    description.aliasedTo = target.getName();
    symbol = target;
  }

  for (const decl of symbol.declarations ?? []) {
    const file = decl.getSourceFile().fileName;
    const { line } = ts.getLineAndCharacterOfPosition(decl.getSourceFile(), decl.getStart());

    const kind = ts.SyntaxKind[decl.kind]; // Human-readable kind like "FunctionDeclaration"
    description.declarations.push({ file, line: line + 1, kind });

    if (
      ts.isImportSpecifier(decl) ||
      ts.isImportClause(decl) ||
      ts.isNamespaceImport(decl) ||
      ts.isImportEqualsDeclaration(decl)
    ) {
      description.isImported = true;
    }
  }

  try {
    const type = checker.getTypeOfSymbolAtLocation(symbol, symbol.valueDeclaration ?? symbol.declarations?.[0]!);
    description.typeString = checker.typeToString(type);
  } catch {
    description.typeString = 'Unknown';
  }

  return description;
}

/**
 * Describe a TypeScript AST node with relevant debug information.
 */
// @ts-expect-error
function describeNode(node: ts.Node): Record<string, any> {
  const kind = ts.SyntaxKind[node.kind];
  const result: Record<string, any> = {
    kind,
    type: '',
    tagName: '',
    props: {},
    text: node.getText(),
  };

  // JSX Opening or Self-Closing Element
  if (ts.isJsxOpeningLikeElement(node)) {
    result.type = 'JsxOpeningLikeElement';
    result.tagName = getTagNameFromJsx(node);
    result.props = {
      attributes: node.attributes?.properties?.map((attr: any) => attr.getText()) || [],
    };
  }

  // JSX Element (with children)
  if (ts.isJsxElement(node)) {
    result.type = 'JsxElement';
    result.tagName = getTagNameFromJsx(node.openingElement);
    result.props = {
      attributes: node.openingElement.attributes?.properties?.map((attr: any) => attr.getText()) || [],
      childrenCount: node.children.length,
    };
  }

  // JSX Self-closing element (redundant but explicit)
  if (ts.isJsxSelfClosingElement(node)) {
    result.type = 'JsxSelfClosingElement';
    result.tagName = getTagNameFromJsx(node);
    result.props = {
      attributes: node.attributes?.properties?.map((attr: any) => attr.getText()) || [],
    };
  }

  // Identifier
  if (ts.isIdentifier(node)) {
    result.type = 'Identifier';
    result.tagName = node.text;
  }

  // Call Expression
  if (ts.isCallExpression(node)) {
    result.type = 'CallExpression';
    result.props = {
      expression: node.expression.getText(),
      arguments: node.arguments.map((arg: any) => arg.getText()),
    };
  }

  // Variable Declaration
  if (ts.isVariableStatement(node)) {
    result.type = 'VariableStatement';
    result.props = {
      declarations: node.declarationList.declarations.map((d: any) => d.name.getText()),
    };
  }

  return result;
}

/**
 * Finds the function, method, or arrow function that renders the given JSX node.
 *
 * @param node - A JsxOpeningLikeElement node (e.g., <Button />)
 * @returns The enclosing function/method/arrow declaration, or undefined if none found.
 */
export function getEnclosingRenderFunction(node: ts.Node): ts.FunctionLikeDeclaration | undefined {
  let current: ts.Node | undefined = node;
  let lastFunction: ts.FunctionLikeDeclaration | undefined;

  while (current) {
    // Stop if we reach the source file
    if (ts.isSourceFile(current)) break;

    // Capture every function as a candidate, but don’t return early
    if (
      ts.isArrowFunction(current) ||
      ts.isFunctionDeclaration(current) ||
      ts.isFunctionExpression(current) ||
      ts.isMethodDeclaration(current) ||
      ts.isGetAccessorDeclaration(current) ||
      ts.isSetAccessorDeclaration(current)
    ) {
      lastFunction = current;
    }

    current = current.parent;
  }

  return lastFunction;
}
/**
 * Safely converts a JsxOpeningLikeElement's tagName to a string.
 * Handles identifiers, property accesses, namespaced names, and 'this'.
 */
export function getTagNameFromJsx(element: ts.JsxOpeningLikeElement): string {
  const tagName = element.tagName;

  if (ts.isIdentifier(tagName)) {
    return tagName.text;
  }

  if (ts.isPropertyAccessExpression(tagName)) {
    return tagName.getText(); // Like "this.Component"
  }

  if (ts.isThisTypeNode(tagName)) {
    return 'this';
  }

  if (ts.isJsxNamespacedName(tagName)) {
    // For namespaced JSX like <foo:bar>
    return `${tagName.namespace.text}:${tagName.name.text}`;
  }

  // Fallback for unexpected cases
  throw new Error(`Unsupported JSX tagName kind: ${ts.SyntaxKind[tagName.kind]}`);
}

/**
 * Generic AST traversal utility that visits all nodes in a SourceFile
 * and applies a user-defined visitor function.
 *
 * @param sourceFile - The TypeScript source file to traverse.
 * @param visitNode - A function that is called for each node.
 */
export function traverseAst(sourceFile: ts.SourceFile, visitNode: (node: ts.Node) => boolean): void {
  function visit(node: ts.Node): void {
    const stop = visitNode(node);
    if (stop) {
      return;
    }
    ts.forEachChild(node, visit);
  }

  visit(sourceFile);
}

/**
 * Traverses a TypeScript source file and collects all `JsxOpeningLikeElement` nodes.
 *
 * This function walks the AST of the given `sourceFile`, identifies nodes
 * that are JSX opening-like elements (e.g., `<div>` or self-closing `<input />`),
 * and returns them in an array.
 *
 * @param sourceFile - The TypeScript source file to traverse.
 * @returns An array of `ts.JsxOpeningLikeElement` nodes found in the source file.
 */
export function traverseToFindOpeningLikeElements(sourceFile: ts.SourceFile): ts.JsxOpeningLikeElement[] {
  const elements: ts.JsxOpeningLikeElement[] = [];

  traverseAst(sourceFile, (node) => {
    if (ts.isJsxOpeningLikeElement(node)) {
      elements.push(node);
    }
    return false;
  });

  return elements;
}

/**
 * Returns the base name (i.e., file name with extension) of a given file path.
 *
 * For example, given `/some/path/file.ts`, it returns `file.ts`.
 *
 * @param filePath - The full path of the file.
 * @returns The base name of the file.
 */
export function getFileBaseName(filePath: string) {
  return path.basename(filePath);
}

/**
 * Retrieves the last modified time of a file, in milliseconds since the Unix epoch.
 *
 * This function reads the file system stats for the given path and extracts the
 * `mtimeMs` value, which indicates when the file was last modified.
 *
 * @param filePath - The full path to the file.
 * @returns The last modified time of the file in milliseconds.
 * @throws Will throw an error if the file does not exist or cannot be accessed.
 */
export function getFileLastModifiedTimeInMs(filePath: string) {
  const stats = fs.statSync(filePath);
  return stats.mtimeMs;
}
