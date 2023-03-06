class ThemePreview {
  private _stylesheet: CSSStyleSheet;

  constructor() {
    this._stylesheet = new CSSStyleSheet();
    this._stylesheet.replaceSync('');
    document.adoptedStyleSheets = [...document.adoptedStyleSheets, this._stylesheet];
  }

  get stylesheet(): CSSStyleSheet {
    return this._stylesheet;
  }

  update(css: string) {
    this._stylesheet.replaceSync(css);
  }
}

export const themePreview = new ThemePreview();
