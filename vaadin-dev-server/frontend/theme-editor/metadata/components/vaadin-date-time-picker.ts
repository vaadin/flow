import { ComponentMetadata } from '../model';
import { errorMessageProperties, helperTextProperties, labelProperties } from './vaadin-text-field';

export default {
  tagName: 'vaadin-date-time-picker',
  displayName: 'DateTimePicker',
  elements: [
    {
      selector: 'vaadin-date-time-picker::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-date-time-picker::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-date-time-picker::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    }
  ]
} as ComponentMetadata;
