import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { iconProperties } from './defaults';
import { standardButtonProperties } from './vaadin-button';

export default {
  tagName: 'vaadin-number-field',
  displayName: 'Number Field',
  elements: [
    {
      selector: 'vaadin-number-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-number-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-number-field::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-number-field::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-number-field::part(clear-button)',
      displayName: 'Clear button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-number-field::part(increase-button)',
      displayName: 'Increase button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-number-field::part(decrease-button)',
      displayName: 'Decrease button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ],
  setupElement(numberField) {
    numberField.stepButtonsVisible = true;
  }
} as ComponentMetadata;
