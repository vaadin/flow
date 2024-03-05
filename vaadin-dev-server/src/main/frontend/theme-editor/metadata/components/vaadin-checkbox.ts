import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';
import { iconProperties, shapeProperties, textProperties } from './defaults';

export const hostElement = {
  selector: 'vaadin-checkbox',
  displayName: 'Checkbox',
  properties: [
    {
      propertyName: '--vaadin-checkbox-size',
      displayName: 'Checkbox size',
      defaultValue: 'var(--lumo-font-size-l)',
      editorType: EditorType.range,
      presets: presets.lumoFontSize,
      icon: 'square'
    }
  ]
};

export const checkboxElement = {
  selector: 'vaadin-checkbox::part(checkbox)',
  displayName: 'Checkbox box',
  properties: [
    shapeProperties.backgroundColor,
    shapeProperties.borderColor,
    shapeProperties.borderWidth,
    shapeProperties.borderRadius
  ]
};

export const checkedCheckboxElement = {
  selector: 'vaadin-checkbox[checked]::part(checkbox)',
  stateAttribute: 'checked',
  displayName: 'Checkbox box (when checked)',
  properties: [
    shapeProperties.backgroundColor,
    shapeProperties.borderColor,
    shapeProperties.borderWidth,
    shapeProperties.borderRadius
  ]
};

export const checkmarkElement = {
  selector: 'vaadin-checkbox::part(checkbox)::after',
  displayName: 'Checkmark',
  properties: [iconProperties.iconColor]
};

export const labelElement = {
  selector: 'vaadin-checkbox label',
  displayName: 'Label',
  properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight, textProperties.fontStyle]
};

export default {
  tagName: 'vaadin-checkbox',
  displayName: 'Checkbox',
  elements: [hostElement, checkboxElement, checkedCheckboxElement, checkmarkElement, labelElement]
} as ComponentMetadata;
