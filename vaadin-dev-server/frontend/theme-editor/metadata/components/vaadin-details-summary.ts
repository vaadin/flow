import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties, textProperties } from "./defaults";

export default {
  tagName: 'vaadin-details-summary',
  displayName: 'DetailsSummary',
  elements: [
    {
      selector: 'vaadin-details-summary',
      displayName: 'Summary',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-details-summary::part(toggle)',
      displayName: 'Toggle',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ]
} as ComponentMetadata;