import {
  PluginOption,
  UserConfigFn
} from 'vite';
import { overrideVaadinConfig } from './vite.generated.ts';
import path from 'node:path';
import { mkdirSync, writeFileSync } from 'node:fs';
import workerThreads from 'node:worker_threads';
import { syncBuiltinESMExports } from 'node:module';

// Observe the checker's real compiler launch inside its worker thread.
mkdirSync('target', { recursive: true });
writeFileSync('target/compiler-launches.jsonl', '');
const Worker = workerThreads.Worker;
workerThreads.Worker = class extends Worker {
  constructor(filename: string | URL, options: workerThreads.WorkerOptions = {}) {
    super(filename, {
      ...options,
      execArgv: [...(options.execArgv ?? process.execArgv), '--require', path.resolve('compiler-observer.cjs')]
    });
  }
};
syncBuiltinESMExports();

/**
 * Dumps effective contents of config.optimizeDeps for tests
 */
function dumpOptimizeDepsPlugin(): PluginOption {
  let config;

  return {
    name: 'dump-optimize-deps',
    configResolved(_config) {
      config = _config;
    },
    transformIndexHtml(html) {
      return [
        {
          injectTo: 'head',
          tag: 'script',
          children: `window.ViteConfigOptimizeDeps = ${JSON.stringify(config.optimizeDeps)};`
        }
      ];
    }
  }
}

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  plugins: [
    dumpOptimizeDepsPlugin()
  ]
});

export default overrideVaadinConfig(customConfig);
