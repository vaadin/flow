class ThemePreview {
  private _stylesheet?: CSSStyleSheet;

  get stylesheet(): CSSStyleSheet {
    this.ensureStylesheet();
    return this._stylesheet!;
  }

  update(css: string) {
    this.ensureStylesheet();
    this._stylesheet!.replaceSync(css);
  }

  private ensureStylesheet() {
    if (this._stylesheet) {
      return;
    }
    this._stylesheet = new CSSStyleSheet();
    this._stylesheet.replaceSync('');
    document.adoptedStyleSheets = [...document.adoptedStyleSheets, this._stylesheet];
  }
}

export const themePreview = new ThemePreview();
