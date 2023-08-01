import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-spreadsheet',
  displayName: 'Spreadsheet',
  elements: [
    {
      selector: 'vaadin-spreadsheet',
      displayName: 'Spreadsheet',
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
