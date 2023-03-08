export enum EditorType {
  text,
  range
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
