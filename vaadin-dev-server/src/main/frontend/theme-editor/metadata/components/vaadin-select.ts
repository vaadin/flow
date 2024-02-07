import { ComponentMetadata } from '../model';
import { fieldProperties, iconProperties, shapeProperties, textProperties } from './defaults';
import { errorMessageProperties, helperTextProperties, labelProperties } from './vaadin-text-field';
import { ComponentReference } from '../../../component-util';
import { hideOverlayMixin, showOverlayMixin } from '../../components/component-overlay-manager';

export default {
  tagName: 'vaadin-select',
  displayName: 'Select',
  elements: [
    {
      selector: 'vaadin-select::part(input-field)',
      displayName: 'Field',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        fieldProperties.height,
        fieldProperties.paddingInline
      ]
    },
    {
      selector: 'vaadin-select vaadin-select-value-button>vaadin-select-item',
      displayName: 'Field text',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-select::part(toggle-button)',
      displayName: 'Field toggle button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-select::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-select::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-select::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-select-overlay::part(overlay)',
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
      selector: 'vaadin-select-overlay vaadin-select-item',
      displayName: 'Overlay items',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-select-overlay vaadin-select-item::part(checkmark)::before',
      displayName: 'Overlay item checkmark',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ],
  async setupElement(select: any) {
    // Apply overlay class name
    select.overlayClass = select.getAttribute('class');
  },
  openOverlay: (component: ComponentReference) => {
    const element = component.element as any;
    showOverlayMixin(element, element, element);
  },
  hideOverlay: (component: ComponentReference) => {
    const element = component.element as any;
    element.opened = false;
    hideOverlayMixin(element, element, element);
  }
} as ComponentMetadata;
