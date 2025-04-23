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
          import cssText from '${path}.css?inline';

          registerStyles('${themeFor}', unsafeCSS(cssText), {
            moduleId: '${moduleId}',
            ${include ? `include: '${include}',` : ''}
          });
        `;
      }

      // if (queryParams.has('include')) {
      //   const include = queryParams.get('include');

      //   return `
      //     import cssText from '${path}.css?inline';

      //     const style = document.createElement('style');
      //     style.textContent = cssText;
      //     style.setAttribute('include', '${include}');
      //     document.head.appendChild(style);
      //   `;
      // }

      if (queryParams.get('type') === 'exportedWebComponent') {
        return `
          import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';
          import cssText from '${path}.css?inline';

          registerStyles('', unsafeCSS(cssText), {
            moduleId: 'exported-web-component-${counter++}'
          });
        `;
      }

      return `
        import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';
        import cssText from '${path}.css?inline';

        injectGlobalCss(cssText, 'CSSImport end', document);
      `
    },
  }
}
