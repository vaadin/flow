import { ComponentMetadata } from '../model';
import { standardShapeProperties, standardTextProperties } from './defaults';

export default {
  tagName: 'vaadin-notification',
  displayName: 'Notification',
  elements: [
    {
      selector: 'vaadin-notification-card::part(overlay)',
      displayName: 'Notification card',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-notification-card::part(content)',
      displayName: 'Content',
      properties: standardTextProperties
    }
  ]
} as ComponentMetadata;
