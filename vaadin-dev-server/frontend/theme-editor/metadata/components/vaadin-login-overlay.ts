import { ComponentMetadata } from '../model';
import { shapeProperties, standardIconProperties, standardShapeProperties, standardTextProperties } from './defaults';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { standardButtonProperties } from './vaadin-button';

export default {
  tagName: 'vaadin-login-overlay',
  displayName: 'Login Overlay',
  elements: [
    {
      selector: 'vaadin-login-overlay-wrapper::part(backdrop)',
      displayName: 'Overlay backdrop / modality curtain',
      properties: [shapeProperties.backgroundColor]
    },
    {
      selector: 'vaadin-login-overlay-wrapper::part(card)',
      displayName: 'Overlay card',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper::part(brand)',
      displayName: 'Card header',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper::part(title)',
      displayName: 'Title',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper::part(description)',
      displayName: 'Description',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper',
      displayName: 'Login form',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper::part(form-title)',
      displayName: 'Form title',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper::part(error-message)',
      displayName: 'Error message section',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper::part(error-message-title)',
      displayName: 'Error message heading',
      properties: standardTextProperties
    },
    {
      selector:
        'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper::part(error-message-description)',
      displayName: 'Error message description',
      properties: standardTextProperties
    },
    {
      selector:
        'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper [required]::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper [required]::part(label)',
      displayName: 'Input field label',
      properties: labelProperties
    },
    {
      selector:
        'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper [required]::part(helper-text)',
      displayName: 'Input field helper text',
      properties: helperTextProperties
    },
    {
      selector:
        'vaadin-login-overlay-wrapper vaadin-login-form vaadin-login-form-wrapper [required]::part(error-message)',
      displayName: 'Input field error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form vaadin-password-field::part(reveal-button)',
      displayName: 'Password field reveal button',
      properties: standardIconProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form vaadin-button[theme~="submit"]',
      displayName: 'Log In Button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form vaadin-button[theme~="submit"]::part(label)',
      displayName: 'Log In Button Label',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form [slot="forgot-password"]',
      displayName: 'Forgot password button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-login-overlay-wrapper vaadin-login-form [slot="forgot-password"]::part(label)',
      displayName: 'Forgot password button label',
      properties: standardTextProperties
    }
  ]
} as ComponentMetadata;
