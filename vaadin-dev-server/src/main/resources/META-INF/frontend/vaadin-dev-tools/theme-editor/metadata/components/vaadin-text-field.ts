import { ComponentMetadata } from '../model';

export default {
  tagName: 'vaadin-text-field',
  displayName: 'TextField',
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
      selector: '::part(input-field)',
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
      selector: '::part(helper-text)',
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
      selector: '::part(error-message)',
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
