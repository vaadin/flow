import { Plugin } from 'vite';
import { extractGlobalCSSRules } from './extract-global-css-rules';

let counter = 0;

function isFlowCSSImportRequest(id: string): boolean {
  return /\?.*flow-css-import/.test(id);
}

export default function flowCSSImportPlugin(): Plugin[] {
  return [
    {
      name: 'vaadin:flow-css-import:resolve',
      enforce: 'pre',
      async resolveId(id) {
        if (!isFlowCSSImportRequest(id)) {
          return;
        }

        return `${id}&inline`;
      }
    },
    {
      name: 'vaadin:flow-css-import:transform',
      enforce: 'post',
      async transform(code, id) {
        if (!isFlowCSSImportRequest(id)) {
          return;
        }

        const css = JSON.parse(code.replace('export default ', ''));
        const queryParams = new URLSearchParams(id.split('?')[1]);

        // @deprecated
        if (queryParams.has('themeFor') || queryParams.has('moduleId')) {
          const themeFor = queryParams.get('themeFor') ?? '';
          const moduleId = queryParams.get('moduleId') ?? `flow_css_mod_${counter++}`;
          const include = queryParams.get('include');

          return `
            import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';

            registerStyles('${themeFor}', unsafeCSS(${JSON.stringify(css)}), {
              moduleId: '${moduleId}',
              ${include ? `include: '${include}'` : ''}
            });
          `;
        }

        // @deprecated
        if (queryParams.has('include')) {
          const include = queryParams.get('include');

          return `
            const style = document.createElement('style');
            style.textContent = ${JSON.stringify(css)};
            style.setAttribute('include', '${include}');
            document.head.appendChild(style);
          `;
        }

        let globalCSS = css;
        let exportedWebComponentCSS;
        let exportedWebComponentSelector;

        if (queryParams.has('exportedWebComponent')) {
          const exportedWebComponent = queryParams.get('exportedWebComponent');

          globalCSS = extractGlobalCSSRules(css);
          exportedWebComponentCSS = css;
          exportedWebComponentSelector = exportedWebComponent;
        }

        return `
          import { injectGlobalCSS, injectExportedWebComponentCSS } from 'Frontend/generated/jar-resources/flow-css-import.js';

          const removers = [
            ${globalCSS ? `
              injectGlobalCSS('${id}', ${JSON.stringify(globalCSS)}),
            ` : ''}

            ${exportedWebComponentCSS ? `
              injectExportedWebComponentCSS('${id}', ${JSON.stringify(exportedWebComponentCSS)}, {
                selector: '${exportedWebComponentSelector}'
              }),
            `: ''}
          ];

          if (import.meta.hot) {
            import.meta.hot.accept();
            import.meta.hot.prune(() => {
              removers.forEach((remove) => remove());
            });
          }
        `;
      }
    },
  ]
}
