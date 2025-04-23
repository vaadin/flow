import { Plugin } from 'vite';

export default function injectCSSPlugin(): Plugin {
  return {
    name: 'vaadin:inject-css',
    enforce: 'pre',
    resolveId(id) {
      if (id.includes('virtual:inject-css')) {
        return `\0${id}`
      }

      return;
    },
    load(id) {
      if (!id.includes('virtual:inject-css')) {
        return;
      }

      const [path, query] = id.replace(/\0virtual:inject-css\//, '').split('?');
      const queryParams = new URLSearchParams(query);

      return `
        import css from '${path}.css?inline';
        import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';
        injectGlobalCss(css, 'CSSImport end', document);
      `
    },
  }
}
