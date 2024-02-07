import { ComponentMetadata } from '../model';
import { html } from 'lit';
import { iconProperties, shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-accordion-heading',
  displayName: 'Accordion Heading',
  description: html`You are trying to style selected heading only, if you wish to style all panel headings of given
    accordion please pick <code>vaadin-accordion</code> instead.`,
  notAccessibleDescription: html`If you wish to style all panel headings of current accordion please pick
    <code>vaadin-accordion</code> instead.`,
  elements: [
    {
      selector: 'vaadin-accordion-heading',
      displayName: 'Heading',
      properties: [textProperties.textColor, textProperties.fontSize, shapeProperties.padding]
    },
    {
      selector: 'vaadin-accordion-heading::part(toggle)',
      displayName: 'Toggle',
      properties: [iconProperties.iconColor, iconProperties.iconSize]
    }
  ]
} as ComponentMetadata;
