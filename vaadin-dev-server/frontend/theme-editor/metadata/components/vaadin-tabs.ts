import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-tabs',
  displayName: 'Tabs',
  elements: [
    {
      selector: 'vaadin-tabs',
      displayName: 'Tabs',
      properties: [shapeProperties.padding]
    },
    {
      selector: 'vaadin-tabs vaadin-tab',
      displayName: 'Tab item',
      properties: [
        textProperties.fontSize,
        textProperties.fontWeight,
        shapeProperties.backgroundColor,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-tabs::part(back-button)',
      displayName: 'Back button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-tabs::part(forward-button)',
      displayName: 'Forward button',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ]
} as ComponentMetadata;
