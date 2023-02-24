export enum ThemeEditorState {
  disabled = 'disabled',
  enabled = 'enabled',
  missing_theme = 'missing_theme'
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
  private _tagName: string;
  private properties: PropertyValueMap = {};

  constructor(tagName: string) {
    this._tagName = tagName;
  }

  get tagName(): string {
    return this._tagName;
  }

  public getPropertyValue(partName: string | null, propertyName: string): ThemePropertyValue {
    return this.properties[propertyKey(partName, propertyName)];
  }

  public updatePropertyValue(partName: string | null, propertyName: string, value: string) {
    let propertyValue = this.getPropertyValue(partName, propertyName);
    if (!propertyValue) {
      propertyValue = {
        partName,
        propertyName,
        value
      };
      this.properties[propertyKey(partName, propertyName)] = propertyValue;
    } else {
      propertyValue.value = value;
    }
  }

  public addPropertyValues(values: ThemePropertyValue[]) {
    values.forEach((value) => {
      this.updatePropertyValue(value.partName, value.propertyName, value.value);
    });
  }
}
