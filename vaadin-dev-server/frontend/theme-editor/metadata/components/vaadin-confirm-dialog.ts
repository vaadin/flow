import { ComponentMetadata } from '../model';
import { standardShapeProperties, standardTextProperties } from './defaults';
import { standardButtonProperties } from './vaadin-button';

export default {
  tagName: 'vaadin-confirm-dialog',
  displayName: 'Confirm Dialog',
  elements: [
    {
      selector: 'vaadin-confirm-dialog-overlay::part(backdrop)',
      displayName: 'Modality curtain (backdrop)',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay::part(overlay)',
      displayName: 'Dialog surface',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay::part(header)',
      displayName: 'Header',
      properties: [...standardShapeProperties, ...standardTextProperties]
    },
    {
      selector: 'vaadin-confirm-dialog-overlay::part(content)',
      displayName: 'Content',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay::part(message)',
      displayName: 'Message',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay::part(footer)',
      displayName: 'Footer',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay > [slot="confirm-button"]',
      displayName: 'Confirm button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay > [slot="confirm-button"]::part(label)',
      displayName: 'Confirm button label',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay > [slot="reject-button"]',
      displayName: 'Reject button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay > [slot="reject-button"]::part(label)',
      displayName: 'Reject button label',
      properties: standardTextProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay > [slot="cancel-button"]',
      displayName: 'Cancel button',
      properties: standardButtonProperties
    },
    {
      selector: 'vaadin-confirm-dialog-overlay > [slot="cancel-button"]::part(label)',
      displayName: 'Cancel button label',
      properties: standardTextProperties
    }
  ]
} as ComponentMetadata;
