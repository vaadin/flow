import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { standardButtonProperties } from './vaadin-button';

export default {
  tagName: 'vaadin-email-field',
  displayName: 'Email Field',
  elements: [
    {
      selector: 'vaadin-email-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-email-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-email-field::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-email-field::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-email-field::part(clear-button)',
      displayName: 'Clear button',
      properties: standardButtonProperties
    }
  ]
} as ComponentMetadata;
