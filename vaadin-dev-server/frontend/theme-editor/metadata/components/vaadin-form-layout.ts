import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-form-layout',
  displayName: 'Form Layout',
  elements: [
    {
      selector: 'vaadin-form-layout',
      displayName: 'Layout',
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
