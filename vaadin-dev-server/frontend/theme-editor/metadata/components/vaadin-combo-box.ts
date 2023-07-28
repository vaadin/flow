import {ComponentMetadata} from '../model';
import {errorMessageProperties, helperTextProperties, inputFieldProperties, labelProperties} from './vaadin-text-field';
import {iconProperties, shapeProperties, textProperties} from './defaults';
import {ComponentReference} from "../../../component-util";

export default {
  tagName: 'vaadin-combo-box',
  displayName: 'ComboBox',
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
    // Setup items
    comboBox.items = [{ label: 'Item', value: 'value' }];
    // Select value
    comboBox.value = 'value';
    // Wait for overlay to open
    await new Promise((resolve) => setTimeout(resolve, 10));
  },
  openOverlay(component: ComponentReference){
    if(!component || !component.element){
      return;
    }
    const element = component.element as any;
    //opening overlay
    element.open();
    //preventing should remove focus to keep overlay opened.
    element._storedShouldRemoveFocus = element._shouldRemoveFocus;
    element._shouldRemoveFocus = (event: FocusEvent) => {
      event.preventDefault();
      event.stopImmediatePropagation();
      return false;
    }
    // preventing overlay to be closed while editing
    element.overlayCloseOverrideEvent = element._overlayElement.addEventListener('vaadin-overlay-close', (e: CustomEvent) => {
      e.preventDefault();
    });
  },
  hideOverlay(component: ComponentReference) {
    if(component && component.element){
      // restoring overridden listeners and methods.
      const element = component.element as any;
      element._shouldRemoveFocus = element._storedShouldRemoveFocus;
      delete element._storedShouldRemoveFocus;
      element.removeEventListener('vaadin-overlay-close', element.overlayCloseOverrideEvent);
      delete element.overlayCloseOverrideEvent;
      element.close();
    }
  }
} as ComponentMetadata;
