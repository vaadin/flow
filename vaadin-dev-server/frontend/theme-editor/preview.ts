class ThemePreview {
  private _stylesheet?: CSSStyleSheet;
  private _localClassNameMap: Map<HTMLElement, string> = new Map();

  get stylesheet(): CSSStyleSheet {
    this.ensureStylesheet();
    return this._stylesheet!;
  }

  add(css: string) {
    this.ensureStylesheet();
    this._stylesheet!.replaceSync(css);
  }

  clear() {
    this.ensureStylesheet();
    this._stylesheet!.replaceSync('');
  }

  previewLocalClassName(element?: HTMLElement, className?: string) {
    if (!element) {
      return;
    }
    // Remove previously assigned class name if exists
    const previousClassName = this._localClassNameMap.get(element);
    if (previousClassName) {
      element.classList.remove(previousClassName);
      (element as any).overlayClass = null;
    }
    // Update class name
    if (className) {
      element.classList.add(className);
      // Also set classname as overlay class in case the component has an
      // overlay. Should not do any harm if the component doesn't have one.
      (element as any).overlayClass = className;
      this._localClassNameMap.set(element, className);
    } else {
      this._localClassNameMap.delete(element);
    }
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
