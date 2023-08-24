import { ComponentMetadata } from '../model';
import { shapeProperties } from './defaults';
import { html } from 'lit';

export default {
  tagName: 'vaadin-board-row',
  description: html`You are styling selected row only, if you wish to style all rows of given board please pick
    <code>vaadin-board</code> instead.`,
  notAccessibleDescription: html`If you wish to style all rows of current board please pick
    <code>vaadin-board</code> instead.`,
  displayName: 'Board Row',
  elements: [
    {
      selector: 'vaadin-board-row',
      displayName: 'Layout',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    }
  ]
} as ComponentMetadata;
