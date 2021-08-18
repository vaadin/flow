/* eslint-disable @typescript-eslint/no-var-requires,import/no-extraneous-dependencies */
const { esbuildPlugin } = require('@web/dev-server-esbuild');
const { puppeteerLauncher } = require('@web/test-runner-puppeteer');

const tsExtPattern = /\.ts$/;

module.exports = {
  rootDir: '.',
  nodeResolve: true,
  browserStartTimeout: 60000, // default 30000
  testsStartTimeout: 60000, // default 10000
  testsFinishTimeout: 60000, // default 20000
  plugins: [
    esbuildPlugin({ ts: true, target: 'auto' }),
    {
      name: 'fix-node-resolve-issue',
      transformImport({ source }) {
        return source.includes('wds-outside-root') && source.endsWith('.ts')
          ? source.replace(tsExtPattern, '.js')
          : source;
      },
    },
  ],
  browsers: [
    puppeteerLauncher({
      launchOptions: {
        args: ['--no-sandbox', '--disable-setuid-sandbox'],
      },
    }),
  ],
};
