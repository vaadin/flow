import * as t from '@babel/types';

export function addFunctionComponentSourceLocationBabel() {
  function isReactFunctionName(name) {
    // A React component function always starts with a Capital letter
    return name && name.match(/^[A-Z].*/);
  }

  /**
   * Collects React function component locations during AST traversal and
   * appends all `Name.__debugSourceDefine = {...}` statements at the end of
   * the file in Program.exit. Appending at the end (rather than after each
   * function) avoids shifting line numbers for subsequent transforms, so JSX
   * source locations reported by OXC remain correct.
   *
   * Must be combined with retainLines:true in Babel options so Babel's
   * printer doesn't shift lines when regenerating the file.
   */
  return {
    visitor: {
      Program: {
        exit(path, state) {
          const filename = state.file.opts.filename;
          const collected = [];

          path.traverse({
            FunctionDeclaration(p) {
              // Matches: function Foo() { ... }
              const name = p.node?.id?.name;
              if (!isReactFunctionName(name)) return;
              if (p.node.body.loc) {
                collected.push({ name, loc: p.node.body.loc });
              }
            },
            VariableDeclaration(p) {
              // Matches: const Foo = () => <div/>
              p.node.declarations.forEach((d) => {
                if (d.id.type !== 'Identifier') return;
                const name = d.id.name;
                if (!isReactFunctionName(name)) return;
                if (d.init?.body?.loc) {
                  collected.push({ name, loc: d.init.body.loc });
                }
              });
            }
          });

          for (const { name, loc } of collected) {
            const debugSourceMember = t.memberExpression(t.identifier(name), t.identifier('__debugSourceDefine'));
            const debugSourceDefine = t.objectExpression([
              t.objectProperty(t.identifier('fileName'), t.stringLiteral(filename)),
              t.objectProperty(t.identifier('lineNumber'), t.numericLiteral(loc.start.line)),
              t.objectProperty(t.identifier('columnNumber'), t.numericLiteral(loc.start.column + 1))
            ]);
            const assignment = t.expressionStatement(t.assignmentExpression('=', debugSourceMember, debugSourceDefine));
            const condition = t.binaryExpression(
              '===',
              t.unaryExpression('typeof', t.identifier(name)),
              t.stringLiteral('function')
            );
            path.pushContainer('body', t.ifStatement(condition, t.blockStatement([assignment])));
          }
        }
      }
    }
  };
}
