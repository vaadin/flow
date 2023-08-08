import { ComponentMetadata } from '../model';
import { iconProperties, shapeProperties, textProperties } from './defaults';
import { html } from 'lit';

export default {
  tagName: 'vaadin-side-nav-item',
  displayName: 'Side Navigation Item',
  description: html`You are styling selected item only, if you wish to style all items of given SideNav component please
    pick <code>vaadin-side-nav</code> instead.`,
  notAccessibleDescription: html`If you wish to style all items of current SideNav component please pick
    <code>vaadin-side-nav</code> instead.`,
  elements: [
    {
      selector: 'vaadin-side-nav-item > [slot="prefix"]',
      displayName: 'Nav item icon',
      properties: [iconProperties.iconSize, iconProperties.iconColor]
    },
    {
      selector: 'vaadin-side-nav-item[active] > [slot="prefix"]',
      displayName: 'Nav item icon (active)',
      properties: [iconProperties.iconSize, iconProperties.iconColor]
    },
    {
      selector: 'vaadin-side-nav-item::part(item)',
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
      selector: 'vaadin-side-nav-item[active]::part(item)',
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
      selector: 'vaadin-side-nav-item::part(toggle-button)::before',
      displayName: 'Expand/collapse icon',
      properties: [iconProperties.iconSize, iconProperties.iconColor]
    },
    {
      selector: 'vaadin-side-nav-item[active]::part(toggle-button)::before',
      displayName: 'Expand/collapse icon (active)',
      properties: [iconProperties.iconSize, iconProperties.iconColor]
    }
  ]
} as ComponentMetadata;
