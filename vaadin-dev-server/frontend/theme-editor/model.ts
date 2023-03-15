import { ComponentMetadata } from './metadata/model';
import { ServerCssRule } from './api';
import { ComponentReference } from '../component-util';

export enum ThemeEditorState {
  disabled = 'disabled',
  enabled = 'enabled',
  missing_theme = 'missing_theme'
}

export enum ThemeScope {
  local = 'local',
  global = 'globals'
}

export interface ThemeContext {
  scope: ThemeScope;
  metadata: ComponentMetadata;
  component: ComponentReference;
  accessible?: boolean;
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

    const hostRule = rules.find((rule) => !rule.partName);
    if (hostRule) {
      metadata.properties.forEach((property) => {
        const value = hostRule.properties[property.propertyName];
        if (value) {
          theme.updatePropertyValue(null, property.propertyName, value, true);
        }
      });
    }

    metadata.parts.forEach((part) => {
      const partRule = rules.find((rule) => rule.partName === part.partName);

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

export function generateThemeRule(
  tagName: string,
  partName: string | null,
  propertyName: string,
  value: string
): ServerCssRule {
  const properties = { [propertyName]: value };
  return {
    tagName,
    partName,
    properties
  };
}
