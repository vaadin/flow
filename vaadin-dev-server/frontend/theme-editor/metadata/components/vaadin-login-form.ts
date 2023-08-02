import { ComponentMetadata } from '../model';
import { standardIconProperties, standardShapeProperties, standardTextProperties } from './defaults';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { standardButtonProperties } from './vaadin-button';

export default {
  tagName: 'vaadin-login-form',
  displayName: 'Login',
  elements: [
    {
      selector: 'vaadin-login-form',
      displayName: 'Login form root component',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper',
      displayName: 'Login form',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper::part(form-title)',
      displayName: 'Form title',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper::part(error-message)',
      displayName: 'Error message section',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper::part(error-message-title)',
      displayName: 'Error message heading',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper::part(error-message-description)',
      displayName: 'Error message description',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper [required]::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper [required]::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper [required]::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-login-form vaadin-login-form-wrapper [required]::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-login-form vaadin-password-field::part(reveal-button)',
      displayName: 'Reveal button',
      properties: standardIconProperties
    },
    {
      selector: 'vaadin-login-form vaadin-button[theme~="submit"]',
      displayName: 'Log In Button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-login-form vaadin-button[theme~="submit"]::part(label)',
      displayName: 'Log In Button Label',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-login-form [slot="forgot-password"]',
      displayName: 'Forgot password button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-login-form [slot="forgot-password"]::part(label)',
      displayName: 'Forgot password button label',
      properties: standardTextProperties
    }
  ]
} as ComponentMetadata;
