import { ComponentMetadata } from '../model';
import { standardShapeProperties, standardTextProperties } from './defaults';

export default {
  tagName: 'vaadin-cookie-consent',
  displayName: 'Cookie Consent',
  elements: [
    {
      selector: 'div.cc-banner',
      displayName: 'Banner',
      properties: standardShapeProperties
    },
    {
      selector: 'div.cc-banner span.cc-message',
      displayName: 'Message',
      properties: standardTextProperties
    },
    {
      selector: 'div.cc-banner a.cc-btn',
      displayName: 'Button',
      properties: [...standardShapeProperties, ...standardTextProperties]
    }
  ]
} as ComponentMetadata;
