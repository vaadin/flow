import { ComponentMetadata } from '../model';

export default {
  tagName: 'vaadin-button',
  displayName: 'Button',
  parts: [
    {
      partName: 'label',
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
      partName: 'prefix',
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
      partName: 'suffix',
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
