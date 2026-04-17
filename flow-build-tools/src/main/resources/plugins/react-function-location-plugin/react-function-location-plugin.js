import * as t from '@babel/types';
import { readFileSync } from 'fs';

export function addFunctionComponentSourceLocationBabel() {
  function isReactFunctionName(name) {
    // A React component function always starts with a Capital letter
    return name && name.match(/^[A-Z].*/);
  }

  // Cache original file contents for finding correct line numbers.
  // When running after OXC (enforce: 'post'), Babel's AST positions
  // refer to the transformed code, not the original source. We read
  // the original file to get correct positions.
  const originalSources = {};

  function getOriginalLines(filename) {
    if (!originalSources[filename]) {
      try {
        originalSources[filename] = readFileSync(filename, 'utf-8').split('\n');
      } catch {
        originalSources[filename] = [];
      }
    }
    return originalSources[filename];
  }

  function findFunctionLine(filename, functionName) {
    const lines = getOriginalLines(filename);
    for (let i = 0; i < lines.length; i++) {
      // Match "function FunctionName(" or "const/let/var FunctionName ="
      const funcMatch = lines[i].match(new RegExp(`\\bfunction\\s+${functionName}\\s*\\(`));
      const constMatch = lines[i].match(new RegExp(`\\b(?:const|let|var)\\s+${functionName}\\s*=`));
      if (funcMatch) {
        // Find the opening brace of the function body
        const braceCol = lines[i].indexOf('{', funcMatch.index + funcMatch[0].length);
        if (braceCol >= 0) {
          return { line: i + 1, column: braceCol + 1 };
        }
        // Brace might be on a following line
        for (let j = i + 1; j < lines.length; j++) {
          const bc = lines[j].indexOf('{');
          if (bc >= 0) return { line: j + 1, column: bc + 1 };
        }
      }
      if (constMatch) {
        // Find arrow function body: look for => and then the body start
        for (let j = i; j < Math.min(i + 5, lines.length); j++) {
          const arrowIdx = lines[j].indexOf('=>');
          if (arrowIdx >= 0) {
            const afterArrow = lines[j].substring(arrowIdx + 2);
            const trimmed = afterArrow.trim();
            if (trimmed.length > 0) {
              // Body starts on same line as =>
              const bodyStart = arrowIdx + 2 + afterArrow.indexOf(trimmed.charAt(0));
              return { line: j + 1, column: bodyStart + 1 };
            }
            // Body starts on next line
            if (j + 1 < lines.length) {
              return { line: j + 2, column: 1 };
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Writes debug info as Name.__debugSourceDefine={...} after the given statement ("path").
   * This is used to make the source location of the function available in the browser in development mode.
   * The name __debugSourceDefine is prefixed by __ to mark this is not a public API.
   *
   * Uses the original source file to determine correct line numbers,
   * since Babel may be running on OXC-transformed code with different positions.
   */
  function addDebugInfo(path, name, filename) {
    const loc = findFunctionLine(filename, name);
    if (!loc) {
      return;
    }
    const debugSourceMember = t.memberExpression(t.identifier(name), t.identifier('__debugSourceDefine'));
    const debugSourceDefine = t.objectExpression([
      t.objectProperty(t.identifier('fileName'), t.stringLiteral(filename)),
      t.objectProperty(t.identifier('lineNumber'), t.numericLiteral(loc.line)),
      t.objectProperty(t.identifier('columnNumber'), t.numericLiteral(loc.column))
    ]);
    const assignment = t.expressionStatement(t.assignmentExpression('=', debugSourceMember, debugSourceDefine));
    const condition = t.binaryExpression(
      '===',
      t.unaryExpression('typeof', t.identifier(name)),
      t.stringLiteral('function')
    );
    const ifFunction = t.ifStatement(condition, t.blockStatement([assignment]));
    path.insertAfter(ifFunction);
  }

  return {
    visitor: {
      VariableDeclaration(path, state) {
        // Finds declarations such as
        // const Foo = () => <div/>
        // export const Bar = () => <span/>

        // and writes a Foo.__debugSourceDefine= {..} after it, referring to the start of the function body
        path.node.declarations.forEach((declaration) => {
          if (declaration.id.type !== 'Identifier') {
            return;
          }
          const name = declaration?.id?.name;
          if (!isReactFunctionName(name)) {
            return;
          }

          const filename = state.file.opts.filename;
          addDebugInfo(path, name, filename);
        });
      },

      FunctionDeclaration(path, state) {
        // Finds declarations such as
        // function Foo() { return <div/>; }
        // export function Bar() { return <span>Hello</span>;}

        // and writes a Foo.__debugSourceDefine= {..} after it, referring to the start of the function body
        const node = path.node;
        const name = node?.id?.name;
        if (!isReactFunctionName(name)) {
          return;
        }
        const filename = state.file.opts.filename;
        addDebugInfo(path, name, filename);
      }
    }
  };
}
