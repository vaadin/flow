import { ComponentMetadata } from '../model';
import { standardButtonProperties } from './vaadin-button';
import { shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-upload',
  displayName: 'Upload',
  elements: [
    {
      selector: 'vaadin-upload > vaadin-button',
      displayName: 'Upload button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-upload > vaadin-button::part(label)',
      displayName: 'Upload button text',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-upload::part(drop-label)',
      displayName: 'Drop label',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-upload vaadin-upload-file-list::part(list)',
      displayName: 'File list',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-upload vaadin-upload-file',
      displayName: 'File element',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-upload vaadin-upload-file > vaadin-progress-bar::part(value)',
      displayName: 'Progress bar',
      properties: [shapeProperties.backgroundColor]
    }
  ]
} as ComponentMetadata;
