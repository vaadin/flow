import { ComponentMetadata } from '../model';
import { shapeProperties, textProperties } from './defaults';
import { html } from 'lit';

export default {
  tagName: 'vaadin-tab',
  displayName: 'Tab',
  description: html`You are styling selected tab only, if you wish to style all tabs please pick
    <code>vaadin-tabs</code> instead.`,
  notAccessibleDescription: html`If you wish to style all tabs please pick <code>vaadin-tabs</code> instead.`,
  elements: [
    {
      selector: 'vaadin-tab',
      displayName: 'Tab item',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        shapeProperties.backgroundColor,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-tab[selected]',
      displayName: 'Tab item (selected)',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        shapeProperties.backgroundColor,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-tab::before',
      displayName: 'Selection indicator',
      properties: [shapeProperties.backgroundColor]
    }
  ]
} as ComponentMetadata;
