import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';
import { errorMessageProperties, helperTextProperties, labelProperties } from './vaadin-text-field';
import { checkboxElement, checkedCheckboxElement, checkmarkElement, labelElement } from './vaadin-checkbox';

export default {
  tagName: 'vaadin-checkbox-group',
  displayName: 'CheckboxGroup',
  elements: [
    {
      selector: 'vaadin-checkbox-group',
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
      selector: 'vaadin-checkbox-group::part(label)',
      displayName: 'Label',
      properties: labelProperties
    },
    {
      selector: 'vaadin-checkbox-group::part(helper-text)',
      displayName: 'Helper text',
      properties: helperTextProperties
    },
    {
      selector: 'vaadin-checkbox-group::part(error-message)',
      displayName: 'Error message',
      properties: errorMessageProperties
    },
    {
      ...checkboxElement,
      selector: `vaadin-checkbox-group ${checkboxElement.selector}`,
      displayName: 'Checkboxes'
    },
    {
      ...checkedCheckboxElement,
      selector: `vaadin-checkbox-group ${checkedCheckboxElement.selector}`,
      displayName: 'Checkboxes (when checked)',
      // Checked state attribute needs to be applied on checkbox rather than group
      stateElementSelector: `vaadin-checkbox-group vaadin-checkbox`
    },
    {
      ...checkmarkElement,
      selector: `vaadin-checkbox-group ${checkmarkElement.selector}`,
      displayName: 'Checkmarks'
    },
    {
      ...labelElement,
      selector: `vaadin-checkbox-group ${labelElement.selector}`,
      displayName: 'Checkbox labels'
    }
  ],
  setupElement(group: any) {
    // Add a checkbox to the group
    const checkbox = document.createElement('vaadin-checkbox') as any;
    const label = document.createElement('label') as any;
    label.textContent = 'Some label';
    label.setAttribute('slot', 'label');
    checkbox.append(label);

    group.append(checkbox);
  }
} as ComponentMetadata;
