export enum EditorType {
  text = 'text',
  checkbox = 'checkbox',
  range = 'range',
  color = 'color'
}

export interface CssPropertyMetadata {
  propertyName: string;
  displayName: string;
  defaultValue?: string;
  description?: string;
  editorType?: EditorType;
  presets?: string[];
  icon?: string;
  checkedValue?: string;
}

export interface ComponentElementMetadata {
  selector: string;
  stateAttribute?: string;
  stateElementSelector?: string;
  displayName: string;
  description?: string;
  properties: CssPropertyMetadata[];
}

export interface ComponentMetadata {
  tagName: string;
  displayName: string;
  description?: string;
  elements: ComponentElementMetadata[];
  setupElement?: (element: any) => Promise<void>;
  cleanupElement?: (element: any) => Promise<void>;
}
