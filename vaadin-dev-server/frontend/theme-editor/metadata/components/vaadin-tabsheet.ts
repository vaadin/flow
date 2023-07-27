import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-tabsheet',
  displayName: 'Sheet',
  elements: [
    {
      selector: 'vaadin-tabsheet',
      displayName: 'Sheet',
      properties: [
        shapeProperties.padding,
        shapeProperties.backgroundColor,
        shapeProperties.borderWidth,
        shapeProperties.borderColor,
        shapeProperties.borderRadius
      ]
    }
  ]
} as ComponentMetadata;
