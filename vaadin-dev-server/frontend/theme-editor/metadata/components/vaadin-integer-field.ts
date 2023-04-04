import { ComponentMetadata } from '../model';
import {
  clearButtonProperties,
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { iconProperties } from './defaults';

export default {
  tagName: 'vaadin-integer-field',
  displayName: 'IntegerField',
  elements: [
    {
      selector: 'vaadin-integer-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-integer-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-integer-field::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-integer-field::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-integer-field::part(clear-button)',
      displayName: 'Clear button',
      properties: clearButtonProperties
    },
    {
      selector: 'vaadin-integer-field::part(decrease-button)',
      displayName: 'Decrease button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-integer-field::part(increase-button)',
      displayName: 'Increase button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ],
  setupElement(integerField) {
    integerField.stepButtonsVisible = true;
  }
} as ComponentMetadata;
