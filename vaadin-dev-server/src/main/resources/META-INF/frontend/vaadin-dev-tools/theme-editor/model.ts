import { ComponentMetadata } from './metadata/model';

export enum ThemeEditorState {
  disabled = 'disabled',
  enabled = 'enabled',
  missing_theme = 'missing_theme'
}

export interface ThemeEditorRule {
  selector: string;
  property: string;
  value: string;
}

export interface ThemePropertyValue {
  partName: string | null;
  propertyName: string;
  value: string;
}

type PropertyValueMap = { [key: string]: ThemePropertyValue };

function propertyKey(partName: string | null, propertyName: string) {
  return `${partName}|${propertyName}`;
}

export class ComponentTheme {
  private _metadata: ComponentMetadata;
  private _properties: PropertyValueMap = {};

  constructor(metadata: ComponentMetadata) {
    this._metadata = metadata;
  }

  get metadata(): ComponentMetadata {
    return this._metadata;
  }

  get properties(): ThemePropertyValue[] {
    return Object.values(this._properties);
  }

  public getPropertyValue(partName: string | null, propertyName: string): ThemePropertyValue {
    return this._properties[propertyKey(partName, propertyName)] || null;
  }

  public updatePropertyValue(partName: string | null, propertyName: string, value: string) {
    let propertyValue = this.getPropertyValue(partName, propertyName);
    if (!propertyValue) {
      propertyValue = {
        partName,
        propertyName,
        value
      };
      this._properties[propertyKey(partName, propertyName)] = propertyValue;
    } else {
      propertyValue.value = value;
    }
  }

  public addPropertyValues(values: ThemePropertyValue[]) {
    values.forEach((value) => {
      this.updatePropertyValue(value.partName, value.propertyName, value.value);
    });
  }

  public getPropertyValuesForPart(partName: string | null) {
    return this.properties.filter((property) => property.partName === partName);
  }

  static combine(...themes: ComponentTheme[]) {
    if (themes.length < 2) {
      throw new Error('Must provide at least two themes');
    }

    const resultTheme = new ComponentTheme(themes[0].metadata);
    themes.forEach((theme) => resultTheme.addPropertyValues(theme.properties));

    return resultTheme;
  }
}

function generateSelector(tagName: string, partName: string | null) {
  return partName ? `${tagName}::part(${partName})` : tagName;
}

export function generateThemeRule(tagName: string, partName: string | null, propertyName: string, value: string) {
  const selector = generateSelector(tagName, partName);
  return {
    selector,
    property: propertyName,
    value: value
  };
}

type ComponentThemeMap = { [key: string]: ComponentTheme };

export class Theme {
  private _componentThemes: ComponentThemeMap = {};

  get componentThemes(): ComponentTheme[] {
    return Object.values(this._componentThemes);
  }

  getComponentTheme(tagName: string) {
    return this._componentThemes[tagName] || null;
  }

  getOrCreateComponentTheme(metadata: ComponentMetadata) {
    let componentTheme = this.getComponentTheme(metadata.tagName);
    if (!componentTheme) {
      componentTheme = new ComponentTheme(metadata);
      this._componentThemes[metadata.tagName] = componentTheme;
    }
    return componentTheme;
  }

  updateComponentTheme(updatedTheme: ComponentTheme) {
    const componentTheme = this.getOrCreateComponentTheme(updatedTheme.metadata);
    componentTheme.addPropertyValues(updatedTheme.properties);
  }

  clone() {
    const resultTheme = new Theme();
    this.componentThemes.forEach((componentTheme) => resultTheme.updateComponentTheme(componentTheme));
    return resultTheme;
  }
}
