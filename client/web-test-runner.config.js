/* eslint-disable @typescript-eslint/no-var-requires,import/no-extraneous-dependencies */
const { esbuildPlugin } = require('@web/dev-server-esbuild');

module.exports = {
  rootDir: '.',
  nodeResolve: true,
  browserStartTimeout: 60000, // default 30000
  testsStartTimeout: 60000, // default 10000
  testsFinishTimeout: 60000, // default 20000
  plugins: [esbuildPlugin({ ts: true })],
};
