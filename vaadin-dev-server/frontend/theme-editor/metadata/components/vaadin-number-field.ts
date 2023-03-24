import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';

export default {
  tagName: 'vaadin-number-field',
  displayName: 'NumberField',
  elements: [
    {
      selector: 'vaadin-number-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-number-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
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
    }
  ]
} as ComponentMetadata;
