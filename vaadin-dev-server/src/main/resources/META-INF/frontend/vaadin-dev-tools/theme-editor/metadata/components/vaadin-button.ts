import { ComponentMetadata } from '../model';

export default {
  tagName: 'vaadin-button',
  displayName: 'Button',
  parts: [
    {
      selector: '::part(label)',
      displayName: 'Label',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Text color'
        },
        {
          propertyName: 'font-size',
          displayName: 'Font size'
        },
        {
          propertyName: 'background',
          displayName: 'Background'
        }
      ]
    },
    {
      selector: '::part(prefix)',
      displayName: 'Prefix',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Text color'
        },
        {
          propertyName: 'font-size',
          displayName: 'Font size'
        },
        {
          propertyName: 'background',
          displayName: 'Background'
        }
      ]
    },
    {
      selector: '::part(suffix)',
      displayName: 'Suffix',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Text color'
        },
        {
          propertyName: 'font-size',
          displayName: 'Font size'
        },
        {
          propertyName: 'background',
          displayName: 'Background'
        }
      ]
    }
  ]
} as ComponentMetadata;
