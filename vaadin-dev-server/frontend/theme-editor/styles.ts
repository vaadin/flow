import { CSSResult } from 'lit';

let globalStylesheet: CSSStyleSheet;
let globalCss: string = '';
const registeredModules: Set<string> = new Set();

export function injectGlobalCss(moduleName: string, css: CSSResult) {
  if (registeredModules.has(moduleName)) {
    return;
  }
  if (!globalStylesheet) {
    globalStylesheet = new CSSStyleSheet();
    document.adoptedStyleSheets = [...document.adoptedStyleSheets, globalStylesheet];
  }

  globalCss += css.cssText;
  globalStylesheet.replaceSync(globalCss);
  registeredModules.add(moduleName);
}
