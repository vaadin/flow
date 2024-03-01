import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';
import { html } from 'lit';

export default {
  tagName: 'vaadin-accordion-panel',
  displayName: 'Accordion Panel',
  description: html`You are styling selected panel only, if you wish to style all panel of given accordion please pick
    <code>vaadin-accordion</code> instead.`,
  notAccessibleDescription: html`If you wish to style all panels of current accordion please pick
    <code>vaadin-accordion</code> instead.`,
  elements: [
    {
      selector: 'vaadin-accordion-panel',
      displayName: 'Panel',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius
      ]
    }
  ]
} as ComponentMetadata;
