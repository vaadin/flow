import ts from 'typescript';
import {
  getEnclosingRenderFunction,
  getFileBaseName,
  getTagNameFromJsx,
  getTsContext,
  traverseAst,
  traverseToFindOpeningLikeElements,
} from './utils';
import type { NodeSource, TsContext } from './types';

interface NodeInfo {
  nodeName: string;
  sourceInfo?: NodeSource;
  createLocationFilePath: string | null;
  createLocationMethodName: string | null;
  activeLevel?: string;
  customComponentFilePath?: string;
  type?: 'EXTERNAL' | 'IN_PROJECT';
  vaadinComponent?: boolean;
  htmlComponent?: boolean;
  routeView?: boolean;
}

/**
 * Traverse through AST tree of files that are parsed by Typescript Compiler API and returns the information about the node instances.
 * @param filePaths fully qualified file paths to analyse
 */
export function getNodesInfosInFiles(filePaths: string[]): NodeInfo[] {
  const nodes: NodeInfo[] = [];
  filePaths.forEach((filePath) => {
    nodes.push(...analyzeSource(filePath));
  });
  return nodes;
}

function findImportDeclaration(
  node: ts.JsxOpeningLikeElement,
  symbol: ts.Symbol,
  tsContext: TsContext,
): NodeInfo | undefined {
  const { sourceFile, filePath } = tsContext;
  const nodeTagAsString = getTagNameFromJsx(node);
  const enclosingRenderFunction = getEnclosingRenderFunction(node);
  if (!enclosingRenderFunction) {
    return undefined;
  }
  const methodName =
    (enclosingRenderFunction as ts.FunctionDeclaration | ts.MethodDeclaration)?.name?.getText() ?? null;
  if (!methodName) {
    return undefined;
  }
  const { line, character } = sourceFile.getLineAndCharacterOfPosition(node.getStart(sourceFile));
  const result: NodeInfo = {
    nodeName: nodeTagAsString,
    createLocationFilePath: filePath,
    createLocationMethodName: methodName,
    sourceInfo: {
      fileName: sourceFile.fileName,
      columnNumber: character + 1,
      lineNumber: line + 1,
    },
  };
  for (const decl of symbol.declarations ?? []) {
    const file = decl.getSourceFile().fileName;
    if (file.indexOf('@vaadin') !== -1) {
      result.vaadinComponent = true;
      break;
    }
    if (file.indexOf('node_modules/react-router') !== -1) {
      // Ignore Outlet and other react router related components
      break;
    }
    if (file.indexOf('node_modules/@types/react') !== -1) {
      // Native html components
      result.htmlComponent = true;
      break;
    }
    if (file.indexOf('node_modules') !== -1) {
      result.type = 'EXTERNAL';
      result.customComponentFilePath = file;
      result.activeLevel = nodeTagAsString;
      break;
    }
    result.type = 'IN_PROJECT';
    result.customComponentFilePath = file;

    result.activeLevel = `${getFileBaseName(filePath)}#${nodeTagAsString}`;
  }
  return result;
}

function findRouteViewDeclaration(context: TsContext) {
  let routeNode: NodeInfo | undefined;
  if (context.sourceFile.fileName.indexOf('@layout') !== -1) {
    // @layout.tsx may have a view config so excluding that.
    return undefined;
  }
  traverseAst(context.sourceFile, (node) => {
    if (
      ts.isFunctionDeclaration(node) &&
      node.modifiers?.some((mod) => mod.kind === ts.SyntaxKind.ExportKeyword) &&
      node.modifiers?.some((mod) => mod.kind === ts.SyntaxKind.DefaultKeyword)
    ) {
      const funcDec = node as ts.FunctionDeclaration;
      const functionName = funcDec.name?.getText() ?? '?';
      routeNode = {
        nodeName: functionName,
        routeView: true,
        customComponentFilePath: context.filePath,
        type: 'IN_PROJECT',
        createLocationFilePath: null,
        createLocationMethodName: null,
        activeLevel: `${getFileBaseName(context.sourceFile.fileName)}#${functionName}`,
      };
      return true;
    }
    return false;
  });
  return routeNode;
}

function findImportOfNode(node: ts.JsxOpeningLikeElement, tsContext: TsContext) {
  const { checker } = tsContext;
  const symbol = checker.getSymbolAtLocation(node.tagName);
  if (!symbol || !symbol.declarations) {
    return;
  }
  // Resolve alias only if it's an alias
  const finalSymbol = symbol.flags & ts.SymbolFlags.Alias ? checker.getAliasedSymbol(symbol) : symbol;
  return findImportDeclaration(node, finalSymbol, tsContext);
}

function analyzeSource(filePath: string): NodeInfo[] {
  const tsContext = getTsContext(filePath);
  const { sourceFile } = tsContext;
  const openingLikeElements = traverseToFindOpeningLikeElements(sourceFile);
  const nodeDeclarations = openingLikeElements
    .map((node) => findImportOfNode(node, tsContext))
    .filter((nodeInfo) => !!nodeInfo);
  const routeViewDeclaration = findRouteViewDeclaration(tsContext);
  if (routeViewDeclaration) {
    nodeDeclarations.push(routeViewDeclaration);
  }
  return nodeDeclarations;
}
