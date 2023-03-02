import { ComponentMetadata } from '../model';

export default {
  tagName: 'vaadin-button',
  displayName: 'Button',
  properties: [
    {
      propertyName: 'background-color',
      displayName: 'Background color',
    },
    {
      propertyName: 'color',
      displayName: 'Text color',
    },
    {
      propertyName: '--lumo-button-size',
      displayName: 'Size',
    },
    {
      propertyName: 'padding-inline',
      displayName: 'Padding',
    }
  ],
  parts: []
} as ComponentMetadata;
