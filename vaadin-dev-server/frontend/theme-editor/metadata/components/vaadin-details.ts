import { ComponentMetadata } from '../model';
import { standardShapeProperties } from './defaults';

export default {
  tagName: 'vaadin-details',
  displayName: 'Details',
  elements: [
    {
      selector: 'vaadin-details',
      displayName: 'Root element',
      properties: standardShapeProperties
    }
  ]
} as ComponentMetadata;
