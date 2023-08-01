import { ComponentMetadata } from '../model';
import {iconProperties, shapeProperties, textProperties} from './defaults';

export default {
  tagName: 'vaadin-side-nav',
  displayName: 'SideNav',
  elements: [
    {
      selector: 'vaadin-side-nav',
      displayName: 'Side nav',
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
      selector: 'vaadin-side-nav::part(label)::after',
      displayName: 'Expand/collapse icon',
      properties: [
        iconProperties.iconSize,
        iconProperties.iconColor
      ]
    },
    {
      selector: 'vaadin-side-nav > vaadin-side-nav-item',
      displayName: 'Nav item',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-side-nav > vaadin-side-nav-item > [slot="prefix"]',
      displayName: 'Nav item icon',
      properties: [
        iconProperties.iconSize,
        iconProperties.iconColor
      ]
    },
    {
      selector: 'vaadin-side-nav > vaadin-side-nav-item::part(item)',
      displayName: 'Nav item label',
      properties: [
        textProperties.textColor,
        textProperties.fontSize,
        textProperties.fontWeight,
        textProperties.fontStyle
      ]
    }
  ]
} as ComponentMetadata;
