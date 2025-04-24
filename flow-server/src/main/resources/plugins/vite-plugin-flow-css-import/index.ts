import { Plugin } from 'vite';

let counter = 0;

export default function flowCSSImportPlugin(): Plugin {
  return {
    name: 'vaadin:flow-css-import',
    enforce: 'pre',
    resolveId(id) {
      if (id.includes('virtual:flow-css-import')) {
        return `\0${id}`
      }

      return;
    },
    load(id) {
      if (!id.includes('virtual:flow-css-import')) {
        return;
      }

      const [path, query] = id.replace(/\0virtual:flow-css-import\//, '').split('?');
      const queryParams = new URLSearchParams(query);

      if (queryParams.has('themeFor') || queryParams.has('moduleId')) {
        const themeFor = queryParams.get('themeFor') ?? '';
        const moduleId = queryParams.get('moduleId') ?? `flow_css_mod_${counter++}`;
        const include = queryParams.get('include');

        return `
          import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';
          import content from '${path}.css?inline';

          registerStyles('${themeFor}', unsafeCSS(content), {
            moduleId: '${moduleId}',
            ${include ? `include: '${include}',` : ''}
          });
        `;
      }

      // if (queryParams.has('include')) {
      //   const include = queryParams.get('include');

      //   return `
      //     import content from '${path}.css?inline';

      //     const style = document.createElement('style');
      //     style.textContent = content;
      //     style.setAttribute('include', '${include}');
      //     document.head.appendChild(style);
      //   `;
      // }

      const scope = queryParams.get('scope') ?? 'global';

      return `
        import { injectCSS } from 'Frontend/generated/jar-resources/flow-css-import.js';
        import content from '${path}.css?inline';

        injectCSS('${id}', content, { scope: '${scope}' });

        if (import.meta.hot) {
          import.meta.hot.accept();
        }
      `;
    },
  }
}
