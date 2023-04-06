import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';
import { fieldProperties, shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-button',
  displayName: 'Button',
  elements: [
    {
      selector: 'vaadin-button',
      displayName: 'Host',
      properties: [
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
      ]
    },
    {
      selector: 'vaadin-button::part(label)',
      displayName: 'Label',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    }
  ]
} as ComponentMetadata;
