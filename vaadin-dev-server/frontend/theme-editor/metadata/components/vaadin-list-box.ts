import { ComponentMetadata } from '../model';
import { fieldProperties, shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-list-box',
  displayName: 'List Box',
  elements: [
    {
      selector: 'vaadin-list-box',
      displayName: 'Root element',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        fieldProperties.paddingInline
      ]
    },
    {
      selector: 'vaadin-list-box > hr',
      displayName: 'Divider',
      properties: [shapeProperties.backgroundColor]
    },
    {
      selector: 'vaadin-list-box > vaadin-item',
      displayName: 'Item',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        fieldProperties.paddingInline
      ]
    }
  ]
} as ComponentMetadata;
