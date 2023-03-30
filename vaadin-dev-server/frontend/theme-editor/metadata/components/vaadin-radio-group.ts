import { ComponentMetadata, EditorType } from '../model';
import { iconProperties, shapeProperties, textProperties } from './defaults';
import { errorMessageProperties, helperTextProperties, labelProperties } from './vaadin-text-field';
import { presets } from './presets';

export default {
  tagName: 'vaadin-radio-group',
  displayName: 'RadioButtonGroup',
  elements: [
    {
      selector: 'vaadin-radio-group',
      displayName: 'Group',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-radio-group::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-radio-group::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-radio-group::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      selector: 'vaadin-radio-group vaadin-radio-button::part(radio)',
      displayName: 'Radio buttons',
      properties: [
        {
          // Should probably use `--vaadin-radio-button-size`, however that can only
          // be defined on the host rather than the radio part, and currently
          // there is no default value for the property in the Lumo theme
          propertyName: '--_radio-button-size',
          displayName: 'Size',
          editorType: EditorType.range,
          presets: presets.lumoFontSize,
          icon: 'square'
        },
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
      ]
    },
    {
      selector: 'vaadin-radio-group vaadin-radio-button[checked]::part(radio)',
      stateAttribute: 'checked',
      // Checked state attribute needs to be applied on radio button rather than group
      stateElementSelector: `vaadin-radio-group vaadin-radio-button`,
      displayName: 'Radio buttons (when checked)',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
      ]
    },
    {
      selector: 'vaadin-radio-group vaadin-radio-button::part(radio)::after',
      displayName: 'Radio dots',
      properties: [{
        ...iconProperties.iconColor,
        // Radio button dot uses border-color instead of background-color
        propertyName: 'border-color'
      }]
    },
    {
      selector: 'vaadin-radio-group vaadin-radio-button label',
      displayName: 'Label',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    }
  ],
  setupElement(group: any) {
    // Add a radio button to the group
    const radioButton = document.createElement('vaadin-radio-button') as any;
    const label = document.createElement('label') as any;
    label.textContent = 'Some label';
    label.setAttribute('slot', 'label');
    radioButton.append(label);

    group.append(radioButton);
  }
} as ComponentMetadata;
