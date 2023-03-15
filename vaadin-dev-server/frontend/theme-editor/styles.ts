import { CSSResult } from 'lit';

let globalStylesheet: CSSStyleSheet;
let globalCss: string = '';

export function injectGlobalCss(css: CSSResult) {
    if (!globalStylesheet) {
        globalStylesheet = new CSSStyleSheet();
        document.adoptedStyleSheets = [...document.adoptedStyleSheets, globalStylesheet];
    }

    globalCss += css.cssText;
    globalStylesheet.replaceSync(globalCss);
}
