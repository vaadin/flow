/// <reference types="node" />
import { Plugin } from 'vite';
import { extractGlobalCSSRules } from './extract-global-css-rules.js';

let counter = 0;

export default function flowCSSImportPlugin(): Plugin[] {
  return [
    {
      name: 'vaadin:flow-css-import:resolve',
      enforce: 'pre',
      async resolveId(id, importer, options) {
        if (!id.includes('virtual:flow-css-import')) {
          return;
        }

        const queryParams = new URLSearchParams(id.split('?')[1]);
        if (['theme-for', 'module-id', 'include', 'exported-web-component'].some((param) => queryParams.has(param))) {
          // Proceed to the `load` hook to generate the Flow's CSS import code.
          return `\0${id}`;
        }

        // Otherwise, return the path of the CSS file itself to let Vite handle it.
        const resolution = await this.resolve(queryParams.get('path')!, importer, options);
        if (resolution) {
          return resolution.id;
        }

        return;
      }
    },
    {
      name: 'vaadin:flow-css-import',
      load(id) {
        if (!id.includes('virtual:flow-css-import')) {
          return;
        }

        const queryParams = new URLSearchParams(id.split('?')[1]);
        const cssPath = queryParams.get('path');
        const cssId = this.environment.mode === 'dev' ? id : counter++;

        // TODO: Remove in Vaadin 25
        if (queryParams.has('theme-for') || queryParams.has('module-id')) {
          const themeFor = queryParams.get('theme-for') ?? '';
          const moduleId = queryParams.get('module-id') ?? `flow_css_mod_${counter++}`;
          const include = queryParams.get('include');

          return `
            import cssContent from '${cssPath}?inline';
            import { registerStyles, unsafeCSS } from '@vaadin/vaadin-themable-mixin';

            registerStyles('${themeFor}', unsafeCSS(cssContent.toString()), {
              moduleId: '${moduleId}',
              ${include ? `include: '${include}'` : ''}
            });
          `;
        }

        // TODO: Remove in Vaadin 25
        if (queryParams.has('include')) {
          const include = queryParams.get('include');

          return `
            import cssContent from '${cssPath}?inline';

            const style = document.createElement('style');
            style.textContent = cssContent.toString();
            style.setAttribute('include', '${include}');
            document.head.appendChild(style);
          `;
        }

        const exportedWebComponent = queryParams.get('exported-web-component');
        if (exportedWebComponent) {
          return `
            import '${cssPath}?global-css-only';
            import cssContent from '${cssPath}?inline';
            import { injectExportedWebComponentCSS } from 'Frontend/generated/jar-resources/flow-css-import.js';

            injectExportedWebComponentCSS('${cssId}', cssContent.toString(), {
              selector: '${exportedWebComponent}'
            });

            // if (globalCSSContent) {
            //   injectGlobalCSS('${cssId}', globalCSSContent.toString());
            // }

            import.meta.hot?.accept();
          `
        }

        return;
      }
    },
    {
      name: 'vaadin:flow-css-import:global-css-only',
      transform(code, id) {
        if (!/\?.*global-css-only/.test(id)) {
          return;
        }

        return { code: extractGlobalCSSRules(code) }
      },
    },
  ]
}
