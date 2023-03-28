import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';

export default {
  tagName: 'vaadin-app-layout',
  displayName: 'AppLayout',
  elements: [
    {
      selector: 'vaadin-app-layout',
      displayName: 'Layout',
      properties: [shapeProperties.backgroundColor]
    },
    {
      selector: 'vaadin-app-layout::part(drawer)',
      displayName: 'Drawer',
      properties: [shapeProperties.backgroundColor, shapeProperties.padding]
    },
    {
      selector: 'vaadin-app-layout::part(navbar)',
      displayName: 'Nav bar',
      properties: [shapeProperties.backgroundColor, shapeProperties.padding]
    }
  ]
} as ComponentMetadata;
