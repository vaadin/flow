import { ComponentMetadata } from '../model';
import {iconProperties, shapeProperties, textProperties} from "./defaults";

export default {
  tagName: 'vaadin-accordion-heading',
  displayName: 'AccordionHeading',
  elements: [
    {
      selector: 'vaadin-accordion-heading',
      displayName: 'Heading',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-accordion-heading::part(toggle)',
      displayName: 'Toggle',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    },
  ]
} as ComponentMetadata;