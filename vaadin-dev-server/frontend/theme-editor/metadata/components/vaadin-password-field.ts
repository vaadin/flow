import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { iconProperties } from './defaults';

export default {
  tagName: 'vaadin-password-field',
  displayName: 'PasswordField',
  elements: [
    {
      selector: 'vaadin-password-field::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-password-field::part(reveal-button)',
      displayName: 'Reveal button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
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
    }
  ]
} as ComponentMetadata;
