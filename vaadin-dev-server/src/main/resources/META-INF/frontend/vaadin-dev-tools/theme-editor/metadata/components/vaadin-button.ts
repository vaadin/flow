import { ComponentMetadata, CssPropertyEditorType } from '../model';

export default {
  tagName: 'vaadin-button',
  displayName: 'Button',
  properties: [
    {
      propertyName: 'background-color',
      displayName: 'Background color',
      editor: CssPropertyEditorType.colorPicker
    },
    {
      propertyName: 'color',
      displayName: 'Text color',
      editor: CssPropertyEditorType.colorPicker
    },
    {
      propertyName: '--lumo-button-size',
      displayName: 'Size',
      editor: CssPropertyEditorType.slider,
      preferredValues: ['--lumo-size-xs', '--lumo-size-s', '--lumo-size-m', '--lumo-size-l', '--lumo-size-xl']
    },
    {
      propertyName: 'padding-inline',
      displayName: 'Padding',
      editor: CssPropertyEditorType.slider,
      preferredValues: ['--lumo-space-xs', '--lumo-space-s', '--lumo-space-m', '--lumo-space-l', '--lumo-space-xl']
    }
  ],
  parts: []
} as ComponentMetadata;
