import { ComponentMetadata } from '../model';
import {
  errorMessageProperties,
  helperTextProperties,
  inputFieldProperties,
  labelProperties
} from './vaadin-text-field';
import { iconProperties, shapeProperties, textProperties } from './defaults';
import { ComponentReference } from '../../../component-util';
import { hideOverlayMixin, showOverlayMixin } from '../../components/component-overlay-manager';

export default {
  tagName: 'vaadin-date-picker',
  displayName: 'Date Picker',
  elements: [
    {
      selector: 'vaadin-date-picker::part(input-field)',
      displayName: 'Input field',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-date-picker::part(toggle-button)',
      displayName: 'Toggle button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-date-picker::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-date-picker::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-date-picker::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-date-picker-overlay::part(overlay)',
      displayName: 'Overlay content',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-date-picker-overlay vaadin-month-calendar::part(month-header)',
      displayName: 'Month header',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    },
    {
      selector: 'vaadin-date-picker-overlay vaadin-month-calendar::part(weekday)',
      displayName: 'Weekday',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    },
    {
      selector: 'vaadin-date-picker-overlay vaadin-month-calendar::part(date)',
      displayName: 'Day',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle,
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius
      ]
    },
    {
      selector: 'vaadin-date-picker-overlay vaadin-date-picker-year::part(year-number)',
      displayName: 'Year number',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    }
  ],
  openOverlay: (component: ComponentReference) => {
    if (!component || !component.element) {
      return;
    }
    const element = component.element as any;
    showOverlayMixin(element, element, element);
  },
  hideOverlay: (component: ComponentReference) => {
    const element = component.element as any;
    hideOverlayMixin(element, element, element);
  }
} as ComponentMetadata;
