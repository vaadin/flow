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

        const cssContent = JSON.parse(code.replace('export default ', ''));
        const queryParams = new URLSearchParams(id.split('?')[1]);

        // @deprecated
        if (queryParams.has('themeFor') || queryParams.has('moduleId')) {
          const themeFor = queryParams.get('themeFor') ?? '';
          const moduleId = queryParams.get('moduleId') ?? `flow_css_mod_${counter++}`;
          const include = queryParams.get('include');

          return `
            import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';

            registerStyles('${themeFor}', unsafeCSS(${JSON.stringify(cssContent)}), {
              moduleId: '${moduleId}',
              ${include ? `include: '${include}'` : ''}
            });
          `.trim();
        }

        // @deprecated
        if (queryParams.has('include')) {
          const include = queryParams.get('include');

          return `
            const style = document.createElement('style');
            style.textContent = ${JSON.stringify(cssContent)};
            style.setAttribute('include', '${include}');
            document.head.appendChild(style);
          `.trim();
        }

        let shadowCSS, globalCSS;

        switch (queryParams.get('scope')) {
          case 'global':
            shadowCSS = null;
            globalCSS = cssContent;
            break;
          case 'shadow':
            shadowCSS = cssContent;
            globalCSS = extractGlobalCSSRules(cssContent);
            break;
        }

        return `
          import { injectCSS } from 'Frontend/generated/jar-resources/flow-css-import.js';

          ${globalCSS ?
            `injectCSS('${id}', ${JSON.stringify(globalCSS)}, { scope: 'global' });` : ''}
          ${shadowCSS ?
            `injectCSS('${id}', ${JSON.stringify(shadowCSS)}, { scope: 'shadow' });` : ''}

          if (import.meta.hot) {
            import.meta.hot.accept();
          }
        `.trim();
      }
    },
  ]
}
