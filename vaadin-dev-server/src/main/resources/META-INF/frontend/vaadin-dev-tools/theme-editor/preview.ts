import { Theme } from './model';

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

  update(theme: Theme) {
    const rules: string[] = [];

    theme.componentThemes.forEach((componentTheme) => {
      componentTheme.metadata.parts
        .map((part) => part.partName)
        .forEach((partName) => {
          const selector = `${componentTheme.metadata.tagName}::part(${partName})`;
          const propertyValues = componentTheme.getPropertyValuesForPart(partName);
          const propertyDeclarations = propertyValues.map((value) => `${value.propertyName}: ${value.value}`).join(';');
          const rule = `${selector} { ${propertyDeclarations} }`;
          rules.push(rule);
        });
    });

    const themeCss = rules.join('\n');
    this._stylesheet.replaceSync(themeCss);
  }

  reset() {
    this._stylesheet.replaceSync('');
  }
}

export const themePreview = new ThemePreview();
