import { ComponentMetadata } from '../model';
import { shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-message-list',
  displayName: 'MessageList',
  elements: [
    {
      selector: 'vaadin-message-list::part(list)',
      displayName: 'List',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-message-list vaadin-message',
      displayName: 'Message item',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-message-list vaadin-message::part(name)',
      displayName: 'Name',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    },
    {
      selector: 'vaadin-message-list vaadin-message::part(time)',
      displayName: 'Time',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    },
    {
      selector: 'vaadin-message-list vaadin-message::part(message)',
      displayName: 'Text',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    }
  ]
} as ComponentMetadata;
