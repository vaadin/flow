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
  tagName: 'vaadin-password-field',
  displayName: 'Password Field',
  elements: [
    {
      selector: 'vaadin-password-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-password-field::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-password-field::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-password-field::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-password-field::part(clear-button)',
      displayName: 'Clear button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-password-field::part(reveal-button)',
      displayName: 'Reveal button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ]
} as ComponentMetadata;
