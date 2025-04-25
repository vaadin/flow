/// <reference types="node" />
import { Plugin } from 'vite';
import { extractGlobalCSSRules } from './extract-global-css-rules.js';

let counter = 0;

export default function flowCSSImportPlugin(): Plugin[] {
  return [
    {
      name: 'vaadin:flow-css-import:extract-global-css',
      transform(code, id) {
        if (!/\?.*extract-global-css/.test(id)) {
          return;
        }

        return { code: extractGlobalCSSRules(code) }
      },
    },
    {
      name: 'vaadin:flow-css-import:inject-css-import',
      resolveId(id) {
        if (!id.includes('virtual:flow-css-import')) {
          return;
        }

        return `\0${id}`;
      },
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
            import shadowCSSContent from '${cssPath}?inline';
            import globalCSSContent from '${cssPath}?inline&extract-global-css';
            import { injectGlobalCSS, injectExportedWebComponentCSS } from 'Frontend/generated/jar-resources/flow-css-import.js';

            injectExportedWebComponentCSS('${cssId}', shadowCSSContent.toString(), {
              selector: '${exportedWebComponent}'
            });

            if (globalCSSContent) {
              injectGlobalCSS('${cssId}', globalCSSContent.toString());
            }

            import.meta.hot?.accept();
          `
        }

        return `
          import cssContent from '${cssPath}?inline';
          import { injectGlobalCSS } from 'Frontend/generated/jar-resources/flow-css-import.js';

          injectGlobalCSS('${cssId}', cssContent.toString());

          import.meta.hot?.accept();
        `
      }
    },
  ]
}
