import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { iconProperties, shapeProperties, textProperties } from './defaults';
import { hideOverlayMixin, showOverlayMixin } from '../../components/component-overlay-manager';
import { ComponentReference } from '../../../component-util';

export default {
  tagName: 'vaadin-combo-box',
  displayName: 'Combo Box',
  elements: [
    {
      selector: 'vaadin-combo-box::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-combo-box::part(toggle-button)',
      displayName: 'Toggle button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-combo-box::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-combo-box::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-combo-box::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-combo-box-overlay::part(overlay)',
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
      selector: 'vaadin-combo-box-overlay vaadin-combo-box-item',
      displayName: 'Overlay items',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-combo-box-overlay vaadin-combo-box-item::part(checkmark)::before',
      displayName: 'Overlay item checkmark',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ],
  async setupElement(comboBox: any) {
    // Apply overlay class name
    comboBox.overlayClass = comboBox.getAttribute('class');
  },
  openOverlay: (component: ComponentReference) => {
    const element = component.element as any;
    const overlay = element.$.overlay;
    showOverlayMixin(element, element, overlay);
  },
  hideOverlay: (component: ComponentReference) => {
    const element = component.element as any;
    const overlay = element.$.overlay;
    hideOverlayMixin(element, element, overlay);
  }
} as ComponentMetadata;
