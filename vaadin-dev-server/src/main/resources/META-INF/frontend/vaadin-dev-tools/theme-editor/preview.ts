import { ComponentTheme, Theme } from './model';

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
    let rules: string[] = [];

    theme.componentThemes.forEach((componentTheme) => {
      const hostRule = this.createRule(componentTheme, null);
      const partRules = componentTheme.metadata.parts.map((part) => this.createRule(componentTheme, part.partName));

      rules = [...rules, hostRule, ...partRules];
    });

    const themeCss = rules.join('\n');
    this._stylesheet.replaceSync(themeCss);
  }

  private createRule(componentTheme: ComponentTheme, partName: string | null) {
    const selector = partName
      ? // Part name selector
        `${componentTheme.metadata.tagName}::part(${partName})`
      : // Host selector
        componentTheme.metadata.tagName;
    const propertyValues = componentTheme.getPropertyValuesForPart(partName);
    const propertyDeclarations = propertyValues.map((value) => `${value.propertyName}: ${value.value}`).join(';');
    return `${selector} { ${propertyDeclarations} }`;
  }

  reset() {
    this._stylesheet.replaceSync('');
  }
}

export const themePreview = new ThemePreview();
