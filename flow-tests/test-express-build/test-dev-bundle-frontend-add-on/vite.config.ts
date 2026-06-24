/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { PluginOption, UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

function addCssToIndex(): PluginOption {
  return {
    name: 'generate-css',
    transformIndexHtml: (_html, _conf) => {
      const tags = [
        {
          tag: 'link',
          attrs: {
            rel: 'stylesheet',
            href: '/imported-by-vite-plugin.css'
          }
        }
      ];

      return tags;
    }
  };
}

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  plugins: [addCssToIndex()]
});

export default overrideVaadinConfig(customConfig);
