import { ComponentMetadata } from '../model';
import {
  clearButtonProperties,
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';

export default {
  tagName: 'vaadin-text-area',
  displayName: 'TextArea',
  elements: [
    {
      selector: 'vaadin-text-area::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-text-area::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-text-area::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-text-area::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-text-area::part(clear-button)',
      displayName: 'Clear button',
      properties: clearButtonProperties
    }
  ]
} as ComponentMetadata;
