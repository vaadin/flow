import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-progress-bar',
  displayName: 'Progress Bar',
  elements: [
    {
      selector: 'vaadin-progress-bar::part(bar)',
      displayName: 'Bar',
      properties: [shapeProperties.backgroundColor]
    },
    {
      selector: 'vaadin-progress-bar::part(value)',
      displayName: 'Value',
      properties: [shapeProperties.backgroundColor]
    }
  ]
} as ComponentMetadata;
