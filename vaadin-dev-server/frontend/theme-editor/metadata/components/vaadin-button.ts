import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';

export default {
  tagName: 'vaadin-button',
  displayName: 'Button',
  properties: [
    {
      propertyName: 'background-color',
      displayName: 'Background color',
      editorType: EditorType.color
    },
    {
      propertyName: 'color',
      displayName: 'Text color',
      editorType: EditorType.color,
      presets: presets.lumoTextColor
    },
    {
      propertyName: '--lumo-button-size',
      displayName: 'Size',
      editorType: EditorType.range,
      presets: presets.lumoSize,
      icon: 'square'
    },
    {
      propertyName: 'padding-inline',
      displayName: 'Padding',
      editorType: EditorType.range,
      presets: presets.lumoSpace,
      icon: 'square'
    }
  ],
  parts: [
    {
      partName: 'label',
      displayName: 'Label',
      properties: [
        {
          propertyName: 'font-size',
          displayName: 'Font size',
          editorType: EditorType.range,
          presets: presets.lumoFontSize,
          icon: 'font'
        }
      ]
    }
  ]
} as ComponentMetadata;
