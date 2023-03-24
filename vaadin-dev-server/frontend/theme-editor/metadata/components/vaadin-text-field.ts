import { ComponentMetadata, CssPropertyMetadata, EditorType } from '../model';
import { presets } from './presets';

export const labelProperties: CssPropertyMetadata[] = [
  {
    propertyName: 'color',
    displayName: 'Text color',
    editorType: EditorType.color,
    presets: presets.lumoTextColor
  },
  {
    propertyName: 'font-size',
    displayName: 'Font size',
    editorType: EditorType.range,
    presets: presets.lumoFontSize,
    icon: 'font'
  },
  {
    propertyName: 'background-color',
    displayName: 'Background color',
    editorType: EditorType.color
  }
];

export const inputFieldProperties: CssPropertyMetadata[] = [
  {
    propertyName: 'color',
    displayName: 'Text color',
    editorType: EditorType.color,
    presets: presets.lumoTextColor
  },
  {
    propertyName: 'font-size',
    displayName: 'Font size',
    editorType: EditorType.range,
    presets: presets.lumoFontSize,
    icon: 'font'
  },
  {
    propertyName: 'background-color',
    displayName: 'Background color',
    editorType: EditorType.color
  }
];

export const helperTextProperties: CssPropertyMetadata[] = [
  {
    propertyName: 'color',
    displayName: 'Text color',
    editorType: EditorType.color,
    presets: presets.lumoTextColor
  },
  {
    propertyName: 'font-size',
    displayName: 'Font size',
    editorType: EditorType.range,
    presets: presets.lumoFontSize,
    icon: 'font'
  },
  {
    propertyName: 'background-color',
    displayName: 'Background color',
    editorType: EditorType.color
  }
];

export const errorMessageProperties: CssPropertyMetadata[] = [
  {
    propertyName: 'color',
    displayName: 'Text color',
    editorType: EditorType.color,
    presets: presets.lumoTextColor
  },
  {
    propertyName: 'font-size',
    displayName: 'Font size',
    editorType: EditorType.range,
    presets: presets.lumoFontSize,
    icon: 'font'
  },
  {
    propertyName: 'background-color',
    displayName: 'Background color',
    editorType: EditorType.color
  }
];

export default {
  tagName: 'vaadin-text-field',
  displayName: 'TextField',
  elements: [
    {
      selector: 'vaadin-text-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-text-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-text-field::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-text-field::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    }
  ]
} as ComponentMetadata;
