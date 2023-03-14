import { ComponentMetadata } from './metadata/model';
import { ServerCssRule } from './api';

export enum ThemeEditorState {
  disabled = 'disabled',
  enabled = 'enabled',
  missing_theme = 'missing_theme'
}

export interface ThemePropertyValue {
  partName: string | null;
  propertyName: string;
  value: string;
  modified: boolean;
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

  public updatePropertyValue(partName: string | null, propertyName: string, value: string, modified?: boolean) {
    let propertyValue = this.getPropertyValue(partName, propertyName);
    if (!propertyValue) {
      propertyValue = {
        partName,
        propertyName,
        value,
        modified: modified || false
      };
      this._properties[propertyKey(partName, propertyName)] = propertyValue;
    } else {
      propertyValue.value = value;
      propertyValue.modified = modified || false;
    }
  }

  public addPropertyValues(values: ThemePropertyValue[]) {
    values.forEach((value) => {
      this.updatePropertyValue(value.partName, value.propertyName, value.value, value.modified);
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

  static fromServerRules(metadata: ComponentMetadata, rules: ServerCssRule[]) {
    const theme = new ComponentTheme(metadata);

    const hostSelector = generateSelector(metadata.tagName, null);
    const hostRule = rules.find((rule) => rule.selector === hostSelector);
    if (hostRule) {
      metadata.properties.forEach((property) => {
        const value = hostRule.properties[property.propertyName];
        if (value) {
          theme.updatePropertyValue(null, property.propertyName, value, true);
        }
      });
    }

    metadata.parts.forEach((part) => {
      const partSelector = generateSelector(metadata.tagName, part.partName);
      const partRule = rules.find((rule) => rule.selector === partSelector);

      if (partRule) {
        part.properties.forEach((property) => {
          const value = partRule.properties[property.propertyName];
          if (value) {
            theme.updatePropertyValue(part.partName, property.propertyName, value, true);
          }
        });
      }
    });

    return theme;
  }
}

function generateSelector(tagName: string, partName: string | null) {
  return partName ? `${tagName}::part(${partName})` : tagName;
}

export function generateThemeRule(tagName: string, partName: string | null, propertyName: string, value: string) {
  const selector = generateSelector(tagName, partName);
  const properties = { [propertyName]: value };
  return {
    selector,
    properties
  };
}
