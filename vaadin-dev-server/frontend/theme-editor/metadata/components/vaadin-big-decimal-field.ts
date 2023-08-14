import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { standardButtonProperties } from './vaadin-button';

export default {
  tagName: 'vaadin-big-decimal-field',
  displayName: 'BigDecimal Field',
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
    },
    {
      selector: 'vaadin-big-decimal-field::part(clear-button)',
      displayName: 'Clear button',
      properties: standardButtonProperties
    }
  ]
} as ComponentMetadata;
