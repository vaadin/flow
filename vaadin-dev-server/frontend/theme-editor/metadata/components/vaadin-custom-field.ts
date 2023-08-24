import { ComponentMetadata } from '../model';
import { errorMessageProperties, helperTextProperties, labelProperties } from './vaadin-text-field';

export default {
  tagName: 'vaadin-custom-field',
  displayName: 'Custom Field',
  elements: [
    {
      selector: 'vaadin-custom-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-custom-field::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-custom-field::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    }
  ]
} as ComponentMetadata;
