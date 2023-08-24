import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-icon',
  displayName: 'Icon',
  elements: [
    {
      selector: 'vaadin-icon',
      displayName: 'Icon',
      properties: [
        iconProperties.iconColor,
        iconProperties.iconSize,
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius
      ]
    }
  ]
} as ComponentMetadata;
