import { Plugin } from 'vite';

let counter = 0;

function isFlowCSSImportRequest(id: string): boolean {
  return /\?.*(flow-css-import)/.test(id);
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

        const cssContent = JSON.parse(code.replace('export default ', ''));
        const queryParams = new URLSearchParams(id.split('?')[1]);

        if (queryParams.has('themeFor') || queryParams.has('moduleId')) {
          const themeFor = queryParams.get('themeFor') ?? '';
          const moduleId = queryParams.get('moduleId') ?? `flow_css_mod_${counter++}`;
          const include = queryParams.get('include');

          return `
            import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';

            registerStyles('${themeFor}', unsafeCSS(${JSON.stringify(cssContent)}), {
              moduleId: '${moduleId}',
              ${include ? `include: '${include}',` : ''}
            });
          `.trim();
        }

        if (queryParams.has('include')) {
          const include = queryParams.get('include');

          return `
            const style = document.createElement('style');
            style.textContent = ${JSON.stringify(cssContent)};
            style.setAttribute('include', '${include}');
            document.head.appendChild(style);
          `.trim();
        }

        const scope = queryParams.get('scope') ?? 'global';

        return `
          import { injectCSS } from 'Frontend/generated/jar-resources/flow-css-import.js';

          injectCSS('${id}', ${JSON.stringify(cssContent)}, {
            scope: '${scope}'
          });

          if (import.meta.hot) {
            import.meta.hot.accept();
          }
        `.trim();
      }
    },
  ]
}
