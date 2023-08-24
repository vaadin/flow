import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-crud',
  displayName: 'CRUD',
  elements: [
    {
      selector: 'vaadin-crud',
      displayName: 'Root element',
      properties: [shapeProperties.borderColor, shapeProperties.borderWidth]
    },
    {
      selector: 'vaadin-crud::part(toolbar)',
      displayName: 'Toolbar below grid',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-crud::part(editor)',
      displayName: 'Editor panel',
      properties: [shapeProperties.backgroundColor]
    },
    {
      selector: 'vaadin-crud vaadin-crud-edit',
      displayName: 'Edit button in grid',
      properties: [shapeProperties.backgroundColor, shapeProperties.borderColor, shapeProperties.borderWidth]
    },
    {
      selector: 'vaadin-crud vaadin-crud-edit::part(icon)::before',
      displayName: 'Edit button in grid icon',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ]
} as ComponentMetadata;
