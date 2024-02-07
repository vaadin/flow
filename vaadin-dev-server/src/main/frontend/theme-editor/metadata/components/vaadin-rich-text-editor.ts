import { ComponentMetadata } from '../model';
import { shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-rich-text-editor',
  displayName: 'Rich Text Editor',
  elements: [
    {
      selector: 'vaadin-rich-text-editor',
      displayName: 'Editor',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderRadius,
        shapeProperties.borderWidth,
        shapeProperties.borderColor,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-rich-text-editor::part(toolbar)',
      displayName: 'Toolbar',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderRadius,
        shapeProperties.borderWidth,
        shapeProperties.borderColor,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-rich-text-editor::part(toolbar-button)',
      displayName: 'Toolbar Buttons',
      properties: [
        { ...textProperties.textColor, displayName: 'Color' },
        { ...textProperties.textColor, displayName: 'Highlight Color', propertyName: '--lumo-primary-color' }
      ]
    },
    {
      selector: 'vaadin-rich-text-editor::part(content)',
      displayName: 'Content',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderRadius,
        shapeProperties.borderWidth,
        shapeProperties.borderColor,
        shapeProperties.padding
      ]
    }
  ]
} as ComponentMetadata;
