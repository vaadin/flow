import { ComponentMetadata, EditorType } from '../model';
import { iconProperties, shapeProperties, textProperties } from './defaults';
import { checkboxElement, checkedCheckboxElement } from './vaadin-checkbox';
import { cellProperties } from './vaadin-grid';
import { inputFieldProperties } from './vaadin-text-field';
import { presets } from './presets';

export default {
  tagName: 'vaadin-grid-pro',
  displayName: 'Grid Pro',
  elements: [
    {
      selector: 'vaadin-grid-pro',
      displayName: 'Root element',
      properties: [shapeProperties.borderColor, shapeProperties.borderWidth]
    },
    {
      selector: 'vaadin-grid-pro::part(header-cell)',
      displayName: 'Header row cell',
      properties: [
        textProperties.textColor,
        // hack to overcome slotted vaadin-grid-cell-content
        { ...textProperties.fontSize, propertyName: '--lumo-font-size-s' },
        // textProperties.fontWeight, -- cannot be styled in single block
        textProperties.fontStyle,
        shapeProperties.backgroundColor
      ]
    },
    {
      selector: 'vaadin-grid-pro::part(body-cell)',
      displayName: 'Body cell',
      properties: cellProperties
    },
    {
      selector: 'vaadin-grid-pro::part(even-row-cell)',
      displayName: 'Cell in even row',
      properties: cellProperties
    },
    {
      selector: 'vaadin-grid-pro::part(odd-row-cell)',
      displayName: 'Cell in odd row',
      properties: cellProperties
    },
    {
      selector: 'vaadin-grid-pro::part(selected-row-cell)',
      displayName: 'Cell in selected row',
      properties: cellProperties
    },
    {
      selector: 'vaadin-grid-pro vaadin-grid-cell-content > vaadin-checkbox::part(checkbox)',
      displayName: 'Row selection checkbox',
      properties: checkboxElement.properties
    },
    {
      selector: 'vaadin-grid-pro vaadin-grid-cell-content > vaadin-checkbox[checked]::part(checkbox)',
      displayName: 'Row selection checkbox (when checked)',
      properties: checkedCheckboxElement.properties
    },
    {
      selector: 'vaadin-grid-pro vaadin-grid-cell-content > vaadin-checkbox::part(checkbox)::after',
      displayName: 'Row selection checkbox checkmark',
      properties: [iconProperties.iconColor]
    },
    {
      selector: 'vaadin-grid-pro vaadin-grid-pro-edit-text-field',
      displayName: 'Text field editor',
      properties: inputFieldProperties
    },
    {
      selector: 'vaadin-grid-pro vaadin-grid-pro-edit-checkbox',
      displayName: 'Checkbox editor',
      properties: [
        {
          propertyName: '--vaadin-checkbox-size',
          displayName: 'Checkbox size',
          defaultValue: 'var(--lumo-font-size-l)',
          editorType: EditorType.range,
          presets: presets.lumoFontSize,
          icon: 'square'
        }
      ]
    },
    {
      selector: 'vaadin-grid-pro vaadin-grid-pro-edit-select',
      displayName: 'Select editor',
      properties: inputFieldProperties
    }
  ]
} as ComponentMetadata;
