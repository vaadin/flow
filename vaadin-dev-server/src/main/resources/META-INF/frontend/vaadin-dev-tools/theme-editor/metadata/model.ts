export enum CssPropertyEditorType {
  text,
  colorPicker,
  slider
}

export interface CssPropertyMetadata {
  propertyName: string;
  displayName: string;
  description?: string;
  preferredValues?: string[]
  editor?: CssPropertyEditorType
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
  parts: ComponentPartMetadata[];
  properties: CssPropertyMetadata[];
}
