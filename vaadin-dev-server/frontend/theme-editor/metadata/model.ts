export enum EditorType {
  text = 'text',
  range = 'range',
  color = 'color'
}

export interface CssPropertyMetadata {
  propertyName: string;
  displayName: string;
  description?: string;
  editorType?: EditorType;
  presets?: string[];
  icon?: string;
}

export interface ComponentPartMetadata {
  partName: string;
  displayName: string;
  description?: string;
  properties: CssPropertyMetadata[];
}

export interface ComponentMetadata {
  tagName: string;
  displayName: string;
  description?: string;
  properties: CssPropertyMetadata[];
  parts: ComponentPartMetadata[];
}
