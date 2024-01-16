import * as t from '@babel/types';

export function addFunctionComponentSourceLocationBabel() {
  function isReactFunctionName(name) {
    return name && name.match(/^[A-Z].*/);
  }

  function addDebugInfo(path, name, filename, loc) {
    const lineNumber = loc.start.line;
    const columnNumber = loc.start.column + 1;
    const debugSourceMember = t.memberExpression(t.identifier(name), t.identifier('__debugSourceDefine'));
    const debugSourceDefine = t.objectExpression([
      t.objectProperty(t.identifier('fileName'), t.stringLiteral(filename)),
      t.objectProperty(t.identifier('lineNumber'), t.numericLiteral(lineNumber)),
      t.objectProperty(t.identifier('columnNumber'), t.numericLiteral(columnNumber)),
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
        const node = path.node;
        const name = node?.id?.name;
        if (!isReactFunctionName(name)) {
          return;
        }
        const filename = state.file.opts.filename;
        addDebugInfo(path, name, filename, node.body.loc);
      },
    },
  };
}
