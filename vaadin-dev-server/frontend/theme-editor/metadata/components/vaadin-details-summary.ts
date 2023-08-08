import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-details-summary',
  displayName: 'Details Summary',
  elements: [
    {
      selector: 'vaadin-details-summary',
      displayName: 'Summary',
      properties: [textProperties.textColor, textProperties.fontSize, shapeProperties.padding]
    },
    {
      selector: 'vaadin-details-summary::part(toggle)',
      displayName: 'Toggle',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ]
} as ComponentMetadata;
