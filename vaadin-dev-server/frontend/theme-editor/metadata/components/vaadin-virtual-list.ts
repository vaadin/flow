import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-virtual-list',
  displayName: 'VirtualList',
  elements: [
    {
      selector: 'vaadin-virtual-list',
      displayName: 'List',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius
      ]
    }
  ]
} as ComponentMetadata;
