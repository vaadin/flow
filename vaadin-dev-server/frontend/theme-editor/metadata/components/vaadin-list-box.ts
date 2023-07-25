import { ComponentMetadata } from '../model';
import {fieldProperties, shapeProperties, textProperties} from "./defaults";

export default {
  tagName: 'vaadin-list-box',
  displayName: 'ListBox',
  elements: [
    {
      selector: 'vaadin-list-box',
      displayName: 'List Box',
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