import { ComponentElementMetadata, ComponentMetadata } from './metadata/model';
import { ServerCssRule } from './api';
import { ComponentReference } from '../component-util';

export enum ThemeEditorState {
  disabled = 'disabled',
  enabled = 'enabled',
  missing_theme = 'missing_theme'
}

export enum ThemeScope {
  local = 'local',
  global = 'global'
}

export interface SelectorScope {
  themeScope: ThemeScope;
  localClassName?: string;
}

export interface ThemeContext {
  scope: ThemeScope;
  metadata?: ComponentMetadata;
  component: ComponentReference;
  accessible?: boolean;
  localClassName?: string;
  suggestedClassName?: string;
}

export interface ThemePropertyValue {
  elementSelector: string;
  propertyName: string;
  value: string;
  modified: boolean;
}

type PropertyValueMap = { [key: string]: ThemePropertyValue };

function propertyKey(elementSelector: string, propertyName: string) {
  return `${elementSelector}|${propertyName}`;
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

  public getPropertyValue(elementSelector: string, propertyName: string): ThemePropertyValue {
    return this._properties[propertyKey(elementSelector, propertyName)] || null;
  }

  public updatePropertyValue(elementSelector: string, propertyName: string, value: string, modified?: boolean) {
    // Remove property when value is an empty string
    if (!value) {
      delete this._properties[propertyKey(elementSelector, propertyName)];
      return;
    }

    let propertyValue = this.getPropertyValue(elementSelector, propertyName);
    if (!propertyValue) {
      propertyValue = {
        elementSelector,
        propertyName,
        value,
        modified: modified || false
      };
      this._properties[propertyKey(elementSelector, propertyName)] = propertyValue;
    } else {
      propertyValue.value = value;
      propertyValue.modified = modified || false;
    }
  }

  public addPropertyValues(values: ThemePropertyValue[]) {
    values.forEach((value) => {
      this.updatePropertyValue(value.elementSelector, value.propertyName, value.value, value.modified);
    });
  }

  public getPropertyValuesForElement(elementSelector: string) {
    return this.properties.filter((property) => property.elementSelector === elementSelector);
  }

  static combine(...themes: ComponentTheme[]) {
    if (themes.length < 2) {
      throw new Error('Must provide at least two themes');
    }

    const resultTheme = new ComponentTheme(themes[0].metadata);
    themes.forEach((theme) => resultTheme.addPropertyValues(theme.properties));

    return resultTheme;
  }

  static fromServerRules(metadata: ComponentMetadata, scope: SelectorScope, rules: ServerCssRule[]) {
    const theme = new ComponentTheme(metadata);

    metadata.elements.forEach((element) => {
      const scopedSelector = createScopedSelector(element, scope);
      const elementRule = rules.find((rule) => rule.selector === scopedSelector.replace(/ > /g, '>'));

      if (elementRule) {
        element.properties.forEach((property) => {
          const value = elementRule.properties[property.propertyName];
          if (value) {
            theme.updatePropertyValue(element.selector, property.propertyName, value, true);
          }
        });
      }
    });

    return theme;
  }
}

export function createScopedSelector(element: ComponentElementMetadata, scope: SelectorScope) {
  const baseSelector = element.selector;

  // Use base selector for global scope
  if (scope.themeScope === ThemeScope.global) {
    return baseSelector;
  }

  // Local scope needs a classname
  if (!scope.localClassName) {
    throw new Error('Can not build local scoped selector without instance class name');
  }

  // Insert classname into selector
  const tagNameMatch = baseSelector.match(/^[\w\d-_]+/);
  const tagName = tagNameMatch && tagNameMatch[0];

  if (!tagName) {
    throw new Error(`Selector does not start with a tag name: ${baseSelector}`);
  }

  return `${tagName}.${scope.localClassName}${baseSelector.substring(tagName.length, baseSelector.length)}`;
}

export function generateThemeRule(
  element: ComponentElementMetadata,
  scope: SelectorScope,
  propertyName: string,
  value: string
): ServerCssRule {
  const scopedSelector = createScopedSelector(element, scope);
  const properties = { [propertyName]: value };

  // Individual property handling

  // Enable border style when setting a border width
  if (propertyName === 'border-width') {
    if (parseInt(value) > 0) {
      properties['border-style'] = 'solid';
    } else {
      properties['border-style'] = '';
    }
  }

  return {
    selector: scopedSelector,
    properties
  };
}

export function generateThemeRuleCss(rule: ServerCssRule) {
  const propertyCss = Object.entries(rule.properties)
    .map(([name, value]) => `${name}: ${value};`)
    .join(' ');

  return `${rule.selector} { ${propertyCss} }`;
}
