import { ComponentTheme } from './model';

class ThemePreview {
  private stylesheet: CSSStyleSheet;

  constructor() {
    this.stylesheet = new CSSStyleSheet();
    this.stylesheet.replaceSync('');
    document.adoptedStyleSheets = [...document.adoptedStyleSheets, this.stylesheet];
  }

  update(theme: ComponentTheme) {
    const rules: string[] = [];
    const uniquePartNames = theme.metadata.parts.map((part) => part.partName);

    uniquePartNames.forEach((partName) => {
      const selector = `${theme.metadata.tagName}::part(${partName})`;
      const propertyValues = theme.getPropertyValuesForPart(partName);
      const propertyDeclarations = propertyValues.map((value) => `${value.propertyName}: ${value.value}`).join(';');
      const rule = `${selector} { ${propertyDeclarations} }`;
      rules.push(rule);
    });

    const themeCss = rules.join('\n');
    this.stylesheet.replaceSync(themeCss);
  }
}

export const themePreview = new ThemePreview();
