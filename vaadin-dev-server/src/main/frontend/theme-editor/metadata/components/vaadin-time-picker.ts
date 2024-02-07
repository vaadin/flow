import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { iconProperties, shapeProperties, textProperties } from './defaults';
import { defaultHideOverlay, defaultShowOverlay } from '../../components/component-overlay-manager';

export default {
  tagName: 'vaadin-time-picker',
  displayName: 'Time Picker',
  elements: [
    {
      selector: 'vaadin-time-picker::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-time-picker::part(toggle-button)',
      displayName: 'Toggle button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-time-picker::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-time-picker::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-time-picker::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-time-picker-overlay::part(overlay)',
      displayName: 'Overlay',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-time-picker-overlay vaadin-time-picker-item',
      displayName: 'Overlay items',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-time-picker-overlay vaadin-time-picker-item::part(checkmark)::before',
      displayName: 'Overlay item checkmark',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ],
  async setupElement(timePicker: any) {
    // Apply overlay class name
    timePicker.overlayClass = timePicker.getAttribute('class');
    // Select value
    timePicker.value = '00:00';
    // Wait for overlay to open
    await new Promise((resolve) => setTimeout(resolve, 10));
  },
  openOverlay: defaultShowOverlay,
  hideOverlay: defaultHideOverlay
} as ComponentMetadata;
