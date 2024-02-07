import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-side-nav',
  displayName: 'Side Navigation',
  elements: [
    {
      selector: 'vaadin-side-nav',
      displayName: 'Root element',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-side-nav > [slot="label"]',
      displayName: 'Label',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    },
    {
      selector: 'vaadin-side-nav vaadin-side-nav-item > [slot="prefix"]',
      displayName: 'Nav item icon',
      properties: [iconProperties.iconSize, iconProperties.iconColor]
    },
    {
      selector: 'vaadin-side-nav vaadin-side-nav-item[active] > [slot="prefix"]',
      displayName: 'Nav item icon (active)',
      properties: [iconProperties.iconSize, iconProperties.iconColor]
    },
    {
      selector: 'vaadin-side-nav vaadin-side-nav-item::part(item)',
      displayName: 'Nav item',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding,
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    },
    {
      selector: 'vaadin-side-nav vaadin-side-nav-item[active]::part(item)',
      displayName: 'Nav item (active)',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle,
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-side-nav vaadin-side-nav-item::part(toggle-button)::before',
      displayName: 'Expand/collapse icon',
      properties: [iconProperties.iconSize, iconProperties.iconColor]
    },
    {
      selector: 'vaadin-side-nav vaadin-side-nav-item[active]::part(toggle-button)::before',
      displayName: 'Expand/collapse icon (active)',
      properties: [iconProperties.iconSize, iconProperties.iconColor]
    }
  ]
} as ComponentMetadata;
