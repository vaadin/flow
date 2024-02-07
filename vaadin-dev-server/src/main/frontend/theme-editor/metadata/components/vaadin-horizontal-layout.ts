import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-horizontal-layout',
  displayName: 'Horizontal Layout',
  elements: [
    {
      selector: 'vaadin-horizontal-layout',
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
