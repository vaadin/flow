import { ComponentMetadata, EditorType } from '../model';
import { inputFieldProperties } from './vaadin-text-field';
import { fieldProperties, shapeProperties } from './defaults';
import { presets } from './presets';

export default {
  tagName: 'vaadin-message-input',
  displayName: 'Message Input',
  elements: [
    {
      selector: 'vaadin-message-input vaadin-text-area::part(input-field)',
      displayName: 'Text area',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-message-input vaadin-button',
      displayName: 'Button',
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
    }
  ]
} as ComponentMetadata;
