import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { standardButtonProperties } from './vaadin-button';

export default {
  tagName: 'vaadin-text-area',
  displayName: 'Text Area',
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
      properties: standardButtonProperties
    }
  ]
} as ComponentMetadata;
