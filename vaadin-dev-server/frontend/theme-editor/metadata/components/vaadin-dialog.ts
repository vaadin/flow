import { ComponentMetadata } from '../model';
import { standardShapeProperties, standardTextProperties } from './defaults';

export default {
  tagName: 'vaadin-dialog',
  displayName: 'Dialog',
  elements: [
    {
      selector: 'vaadin-dialog-overlay::part(backdrop)',
      displayName: 'Modality curtain (backdrop)',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-dialog-overlay::part(overlay)',
      displayName: 'Dialog surface',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-dialog-overlay::part(header)',
      displayName: 'Header',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-dialog-overlay::part(title)',
      displayName: 'Title',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-dialog-overlay::part(content)',
      displayName: 'Content',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-dialog-overlay::part(footer)',
      displayName: 'Footer',
      properties: standardShapeProperties
    }
  ]
} as ComponentMetadata;
