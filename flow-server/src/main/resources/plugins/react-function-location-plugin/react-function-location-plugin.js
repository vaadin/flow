import * as t from '@babel/types';

export function addFunctionComponentSourceLocationBabel() {
  function isReactFunctionName(name) {
    // A React component function always starts with a Capital letter
    return name && name.match(/^[A-Z].*/);
  }

  /**
   * Writes debug info as Name.__debugSourceDefine={...} after the given statement ("path").
   * This is used to make the source location of the function (defined by the loc parameter) available in the browser in development mode.
   * The name __debugSourceDefine is prefixed by __ to mark this is not a public API.
   */
  function addDebugInfo(path, name, filename, loc) {
    const lineNumber = loc.start.line;
    const columnNumber = loc.start.column + 1;
    const debugSourceMember = t.memberExpression(t.identifier(name), t.identifier('__debugSourceDefine'));
    const debugSourceDefine = t.objectExpression([
      t.objectProperty(t.identifier('fileName'), t.stringLiteral(filename)),
      t.objectProperty(t.identifier('lineNumber'), t.numericLiteral(lineNumber)),
      t.objectProperty(t.identifier('columnNumber'), t.numericLiteral(columnNumber))
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
          if (declaration?.init?.body?.loc) {
            addDebugInfo(path, name, filename, declaration.init.body.loc);
          }
        });
      },

      FunctionDeclaration(path, state) {
        // Finds declarations such as
        // functio Foo() { return <div/>; }
        // export function Bar() { return <span>Hello</span>;}

        // and writes a Foo.__debugSourceDefine= {..} after it, referring to the start of the function body
        const node = path.node;
        const name = node?.id?.name;
        if (!isReactFunctionName(name)) {
          return;
        }
        const filename = state.file.opts.filename;
        addDebugInfo(path, name, filename, node.body.loc);
      }
    }
  };
}
