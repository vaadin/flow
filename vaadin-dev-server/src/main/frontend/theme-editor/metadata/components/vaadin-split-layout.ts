import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-split-layout',
  displayName: 'Split Layout',
  elements: [
    {
      selector: 'vaadin-split-layout',
      displayName: 'Layout',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius
      ]
    },
    {
      selector: 'vaadin-split-layout::part(splitter)',
      displayName: 'Splitter bar',
      properties: [shapeProperties.backgroundColor]
    },
    {
      selector: 'vaadin-split-layout::part(handle)::after',
      displayName: 'Splitter handle',
      properties: [shapeProperties.backgroundColor]
    }
  ]
} as ComponentMetadata;
