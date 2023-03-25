import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';

export default {
  tagName: 'vaadin-big-decimal-field',
  displayName: 'BigDecimalField',
  elements: [
    {
      selector: 'vaadin-big-decimal-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-big-decimal-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-big-decimal-field::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-big-decimal-field::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    }
  ]
} as ComponentMetadata;
