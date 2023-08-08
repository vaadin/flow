import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';
import { fieldProperties, shapeProperties, textProperties } from './defaults';

export default {
  tagName: 'vaadin-menu-bar',
  displayName: 'Menu Bar',
  elements: [
    {
      selector: 'vaadin-menu-bar vaadin-menu-bar-button',
      displayName: 'Buttons',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        {
          propertyName: '--lumo-button-size',
          displayName: 'Size',
          editorType: EditorType.range,
          presets: presets.lumoSize,
          icon: 'square'
        },
        fieldProperties.paddingInline
      ]
    },
    {
      selector: 'vaadin-menu-bar vaadin-menu-bar-button vaadin-menu-bar-item',
      displayName: 'Button labels',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    },
    {
      selector: 'vaadin-menu-bar-overlay::part(overlay)',
      displayName: 'Menus',
      properties: [
        shapeProperties.backgroundColor,
        shapeProperties.borderColor,
        shapeProperties.borderWidth,
        shapeProperties.borderRadius,
        shapeProperties.padding
      ]
    },
    {
      selector: 'vaadin-menu-bar-overlay vaadin-menu-bar-item',
      displayName: 'Menu items',
      properties: [textProperties.textColor, textProperties.fontSize, textProperties.fontWeight]
    }
  ],
  async setupElement(menuBar: any) {
    // Apply overlay class name
    menuBar.overlayClass = menuBar.getAttribute('class');
    // Setup items
    // Flow component wraps all button contents into a vaadin-menu-bar-item, so
    // we need to reproduce that
    const rootItem = document.createElement('vaadin-menu-bar-item');
    menuBar.items = [
      {
        component: rootItem,
        children: [
          {
            text: 'Sub item'
          }
        ]
      }
    ];
    // Open overlay
    menuBar.querySelector('vaadin-menu-bar-button').click();
    // Wait for overlay to open
    await new Promise((resolve) => setTimeout(resolve, 10));
  },
  async cleanupElement(menuBar: any) {
    // Menu bar does not close / remove its overlay when it is removed from DOM,
    // so we need to do it manually
    menuBar._close();
  }
} as ComponentMetadata;
