import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-accordion',
  displayName: 'Accordion',
  elements: [
    {
      selector: 'vaadin-accordion > vaadin-accordion-panel > vaadin-accordion-heading',
      displayName: 'Heading',
      properties: [textProperties.textColor, textProperties.fontSize, shapeProperties.padding]
    },
    {
      selector: 'vaadin-accordion > vaadin-accordion-panel > vaadin-accordion-heading::part(toggle)',
      displayName: 'Toggle',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
    {
      selector: 'vaadin-accordion > vaadin-accordion-panel',
      displayName: 'Panel',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius
      ]
    }
  ]
} as ComponentMetadata;
