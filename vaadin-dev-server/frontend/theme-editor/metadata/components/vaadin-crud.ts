import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-crud',
  displayName: 'Crud',
  elements: [
    {
      selector: 'vaadin-crud',
      displayName: 'Crud',
      properties: [shapeProperties.borderColor, shapeProperties.borderWidth]
    },
    {
      selector: 'vaadin-crud::part(toolbar)',
      displayName: 'Toolbar',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-crud::part(editor)',
      displayName: 'Editor',
      properties: [shapeProperties.backgroundColor]
    },
    {
      selector: 'vaadin-crud vaadin-crud-edit',
      displayName: 'Edit button',
      properties: [shapeProperties.backgroundColor, shapeProperties.borderColor, shapeProperties.borderWidth]
    },
    {
      selector: 'vaadin-crud vaadin-crud-edit::part(icon)::before',
      displayName: 'Edit button icon',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ]
} as ComponentMetadata;
