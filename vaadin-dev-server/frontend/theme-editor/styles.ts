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
  }

  .editor-row > .label {
    flex: 0 0 auto;
    width: 100px;
  }

  .editor-row > .editor {
    flex: 1 1 0;
  }

  .editor-row .input {
    width: 100%;
    box-sizing: border-box;
    padding: 0.25rem 0.375rem;
    color: inherit;
    background: rgba(0, 0, 0, 0.2);
    border-radius: 0.25rem;
    border: none;
  }
`;
