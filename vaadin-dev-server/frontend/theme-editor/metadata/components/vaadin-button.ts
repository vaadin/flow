import { ComponentMetadata, CssPropertyMetadata, EditorType } from '../model';
import { presets } from './presets';
import { fieldProperties, shapeProperties, standardTextProperties } from './defaults';

export const standardButtonProperties: CssPropertyMetadata[] = [
  shapeProperties.backgroundColor,
  shapeProperties.borderColor,
  shapeProperties.borderWidth,
  shapeProperties.borderRadius,
  {
    propertyName: '--lumo-button-size',
    displayName: 'Size',
    editorType: EditorType.range,
    presets: presets.lumoSize,
    icon: 'square'
  },
  fieldProperties.paddingInline
];

export default {
  tagName: 'vaadin-button',
  displayName: 'Button',
  elements: [
    {
      selector: 'vaadin-button',
      displayName: 'Root element',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-button::part(label)',
      displayName: 'Label',
      properties: standardTextProperties
    }
  ]
} as ComponentMetadata;
