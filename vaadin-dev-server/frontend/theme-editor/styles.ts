import { css, CSSResult } from 'lit';

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

export const editorRowStyles = css`
  .editor-row {
    display: flex;
    align-items: baseline;
    padding: var(--theme-editor-section-horizontal-padding);
    gap: 10px;
  }

  .editor-row > .label {
    flex: 0 0 auto;
    width: 120px;
  }

  .editor-row > .editor {
    flex: 1 1 0;
  }
`;
