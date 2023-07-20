import { ComponentMetadata } from '../model';
import { shapeProperties } from "./defaults";

export default {
  tagName: 'vaadin-accordion-panel',
  displayName: 'AccordionPanel',
  elements: [
    {
      selector: 'vaadin-accordion-panel',
      displayName: 'Panel',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
      ]
    }
  ]
} as ComponentMetadata;