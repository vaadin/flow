import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';
import { shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-avatar',
  displayName: 'Avatar',
  elements: [
    {
      selector: 'vaadin-avatar',
      displayName: 'Root element',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        {
          propertyName: '--vaadin-avatar-size',
          displayName: 'Size',
          editorType: EditorType.range,
          presets: presets.lumoSize,
          icon: 'square'
        }
      ]
    },
    {
      selector: 'vaadin-avatar::part(abbr)',
      displayName: 'Abbreviation',
      properties: [textProperties.textColor, textProperties.fontWeight]
    }
  ]
} as ComponentMetadata;
