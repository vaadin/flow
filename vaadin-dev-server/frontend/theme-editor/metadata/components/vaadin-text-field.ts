import { ComponentMetadata, CssPropertyMetadata } from '../model';
import { fieldProperties, shapeProperties, standardIconProperties, textProperties } from './defaults';

export const inputFieldProperties: CssPropertyMetadata[] = [
  shapeProperties.backgroundColor,
  shapeProperties.borderColor,
  shapeProperties.borderWidth,
  shapeProperties.borderRadius,
  fieldProperties.height,
  fieldProperties.paddingInline,
  textProperties.textColor,
  textProperties.fontSize,
  textProperties.fontWeight
];

export const labelProperties: CssPropertyMetadata[] = [
  textProperties.textColor,
  textProperties.fontSize,
  textProperties.fontWeight
];

export const helperTextProperties: CssPropertyMetadata[] = [
  textProperties.textColor,
  textProperties.fontSize,
  textProperties.fontWeight
];

export const errorMessageProperties: CssPropertyMetadata[] = [
  textProperties.textColor,
  textProperties.fontSize,
  textProperties.fontWeight
];

export default {
  tagName: 'vaadin-text-field',
  displayName: 'Text Field',
  elements: [
    {
      selector: 'vaadin-text-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-text-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
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
    },
    {
      selector: 'vaadin-text-field::part(clear-button)',
      displayName: 'Clear button',
      properties: standardIconProperties
    }
  ]
} as ComponentMetadata;
