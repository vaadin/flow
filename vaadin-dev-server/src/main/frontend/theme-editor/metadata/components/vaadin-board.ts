import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-board',
  displayName: 'Board',
  elements: [
    {
      selector: 'vaadin-board',
      displayName: 'Layout',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-board vaadin-board-row',
      displayName: 'Row',
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
