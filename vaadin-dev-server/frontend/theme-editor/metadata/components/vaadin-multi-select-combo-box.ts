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
  tagName: 'vaadin-multi-select-combo-box',
  displayName: 'Multi-Select Combo Box',
  elements: [
    {
      selector: 'vaadin-multi-select-combo-box::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-multi-select-combo-box::part(toggle-button)',
      displayName: 'Toggle button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-multi-select-combo-box::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-multi-select-combo-box::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-multi-select-combo-box::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-multi-select-combo-box-overlay::part(overlay)',
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
      selector: 'vaadin-multi-select-combo-box-overlay vaadin-multi-select-combo-box-item',
      displayName: 'Overlay items',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-multi-select-combo-box-overlay vaadin-multi-select-combo-box-item::part(checkmark)::before',
      displayName: 'Overlay item checkmark',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-multi-select-combo-box vaadin-multi-select-combo-box-chip',
      displayName: 'Chip',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-multi-select-combo-box vaadin-multi-select-combo-box-chip',
      displayName: 'Chip label',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-multi-select-combo-box vaadin-multi-select-combo-box-chip::part(remove-button)',
      displayName: 'Chip remove button',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-multi-select-combo-box vaadin-multi-select-combo-box-chip::part(remove-button)::before',
      displayName: 'Chip remove button icon',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ],
  async setupElement(comboBox: any) {
    // Apply overlay class name
    comboBox.overlayClass = comboBox.getAttribute('class');
    // Setup items
    comboBox.items = [{ label: 'Item', value: 'value' }];
    // Select value
    comboBox.value = 'value';
    // Open overlay
    comboBox.opened = true;
    // Wait for overlay to open
    await new Promise((resolve) => setTimeout(resolve, 10));
  },
  async cleanupElement(comboBox: any) {
    comboBox.opened = false;
  },
  openOverlay: defaultShowOverlay,
  hideOverlay: defaultHideOverlay
} as ComponentMetadata;
