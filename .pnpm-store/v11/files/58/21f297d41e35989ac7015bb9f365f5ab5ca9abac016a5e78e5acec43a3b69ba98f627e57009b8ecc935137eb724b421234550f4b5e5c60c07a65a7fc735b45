declare module 'construct-style-sheets-polyfill';

interface CSSStyleSheet {
  replace(contents: string): Promise<CSSStyleSheet>;
  replaceSync(contents: string): void;
}

interface Document {
  adoptedStyleSheets: readonly CSSStyleSheet[];
}

interface ShadowRoot {
  adoptedStyleSheets: readonly CSSStyleSheet[];
}
