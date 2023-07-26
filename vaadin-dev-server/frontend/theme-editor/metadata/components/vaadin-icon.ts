import { ComponentMetadata } from '../model';
import { iconProperties } from './defaults';

export default {
  tagName: 'vaadin-icon',
  displayName: 'Icon',
  elements: [
    {
      selector: 'vaadin-icon',
      displayName: 'Host',
      properties: [
        iconProperties.iconColor,
        iconProperties.iconSize,
      ]
    }
  ]
} as ComponentMetadata;
