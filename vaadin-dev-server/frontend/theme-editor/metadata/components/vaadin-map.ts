import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-map',
  displayName: 'Map',
  elements: [
    {
      selector: 'vaadin-map',
      displayName: 'Map',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    }
  ]
} as ComponentMetadata;
