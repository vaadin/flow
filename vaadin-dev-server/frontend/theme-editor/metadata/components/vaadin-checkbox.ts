import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';
import { iconProperties, shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-checkbox',
  displayName: 'Checkbox',
  elements: [
    {
      selector: 'vaadin-checkbox::part(checkbox)',
      displayName: 'Checkbox',
      properties: [
        {
          // Should probably use `--vaadin-checkbox-size`, however that can only
          // be defined on the host rather than the checkbox part, and currently
          // there is no default value for the property in the Lumo theme
          propertyName: '--_checkbox-size',
          displayName: 'Size',
          editorType: EditorType.range,
          presets: presets.lumoFontSize,
          icon: 'square'
        },
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius
      ]
    },
    {
      selector: 'vaadin-checkbox[checked]::part(checkbox)',
      stateAttribute: 'checked',
      displayName: 'Checkbox (when checked)',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius
      ]
    },
    {
      selector: 'vaadin-checkbox::part(checkbox)::after',
      displayName: 'Checkmark',
      properties: [iconProperties.iconColor]
    },
    {
      selector: 'vaadin-checkbox label',
      displayName: 'Label',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    }
  ]
} as ComponentMetadata;
