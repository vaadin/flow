import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-vertical-layout',
  displayName: 'Vertical Layout',
  elements: [
    {
      selector: 'vaadin-vertical-layout',
      displayName: 'Layout',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding,
        shapeProperties.gap
      ]
    }
  ]
} as ComponentMetadata;
