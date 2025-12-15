import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';

import tailwindCss from './tailwind.css?inline';

let cleanup = () => {};
function applyTailwindCss(css) {
  const injected = injectGlobalCss(css.toString(), 'CSSImport end', document);
  if (!injected) {
    return;
  }

  cleanup();
  cleanup = injected;
}
applyTailwindCss(tailwindCss);

if (import.meta.hot) {
  import.meta.hot.accept('./tailwind.css?inline', (module) => {
    applyTailwindCss(module.default);
  });
}
