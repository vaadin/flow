/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {
  PluginOption,
  UserConfigFn
} from 'vite';
import { overrideVaadinConfig } from './vite.generated';

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
