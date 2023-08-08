import { ComponentMetadata, CssPropertyMetadata } from '../model';
import { iconProperties, shapeProperties, textProperties } from './defaults';
import { checkboxElement, checkedCheckboxElement } from './vaadin-checkbox';

export const cellProperties: CssPropertyMetadata[] = [
  textProperties.textColor,
  textProperties.fontSize,
  textProperties.fontWeight,
  textProperties.fontStyle,
  shapeProperties.backgroundColor
];

export default {
  tagName: 'vaadin-grid',
  displayName: 'Grid',
  elements: [
    {
      selector: 'vaadin-grid',
      displayName: 'Root element',
      properties: [shapeProperties.borderColor, shapeProperties.borderWidth]
    },
    {
      selector: 'vaadin-grid::part(header-cell)',
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
      selector: 'vaadin-grid::part(body-cell)',
      displayName: 'Body cell',
      properties: cellProperties
    },
    {
      selector: 'vaadin-grid::part(even-row-cell)',
      displayName: 'Cell in even row',
      properties: cellProperties
    },
    {
      selector: 'vaadin-grid::part(odd-row-cell)',
      displayName: 'Cell in odd row',
      properties: cellProperties
    },
    {
      selector: 'vaadin-grid::part(selected-row-cell)',
      displayName: 'Cell in selected row',
      properties: cellProperties
    },
    {
      selector: 'vaadin-grid vaadin-grid-cell-content > vaadin-checkbox::part(checkbox)',
      displayName: 'Row selection checkbox',
      properties: checkboxElement.properties
    },
    {
      selector: 'vaadin-grid vaadin-grid-cell-content > vaadin-checkbox[checked]::part(checkbox)',
      displayName: 'Row selection checkbox (when checked)',
      properties: checkedCheckboxElement.properties
    },
    {
      selector: 'vaadin-grid vaadin-grid-cell-content > vaadin-checkbox::part(checkbox)::after',
      displayName: 'Row selection checkbox checkmark',
      properties: [iconProperties.iconColor]
    },
    {
      selector: 'vaadin-grid vaadin-grid-tree-toggle::part(toggle)',
      displayName: 'Expand icon (for tree grid)',
      properties: [iconProperties.iconColor]
    }
  ]
} as ComponentMetadata;
