import { ComponentMetadata } from '../model';

export default {
  tagName: 'vaadin-text-field',
  displayName: 'TextField',
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
      partName: 'input-field',
      displayName: 'Input field',
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
      partName: 'helper-text',
      displayName: 'Helper text',
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
      partName: 'error-message',
      displayName: 'Error message',
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
