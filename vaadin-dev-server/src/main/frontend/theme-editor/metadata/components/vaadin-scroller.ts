import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-scroller',
  displayName: 'Scroller',
  elements: [
    {
      selector: 'vaadin-scroller',
      displayName: 'Scroller',
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
