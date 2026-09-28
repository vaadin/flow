// Loaded by Vite's checker workers. Observe launches without changing them.
const childProcess = require('node:child_process');
const { syncBuiltinESMExports, createRequire } = require('node:module');
const { appendFileSync, existsSync } = require('node:fs');
const path = require('node:path');

const projectRequire = createRequire(path.join(process.cwd(), 'package.json'));
let nativeRoot = path.dirname(projectRequire.resolve('@typescript/native'));
while (!existsSync(path.join(nativeRoot, 'package.json'))) {
  const parent = path.dirname(nativeRoot);
  if (parent === nativeRoot) throw new Error('Native compiler package not found');
  nativeRoot = parent;
}
const nativeCompiler = path.join(nativeRoot, 'bin', 'tsc');
const typescript = projectRequire('typescript');
const spawn = childProcess.spawn;
childProcess.spawn = function (command, args, options) {
  if (args?.includes(nativeCompiler)) {
    appendFileSync(path.join(process.cwd(), 'target/compiler-launches.jsonl'), JSON.stringify({
      nativeCompiler: nativeCompiler.split(path.sep).join('/'),
      typescriptVersion: typescript.version,
      hasCompilerApi: typeof typescript.createProgram === 'function'
    }) + '\n');
  }
  return spawn.apply(this, arguments);
};
syncBuiltinESMExports();
