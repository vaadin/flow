import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';

export default {
  tagName: 'vaadin-menu-bar',
  displayName: 'MenuBar',
  elements: [
    {
      selector: 'vaadin-menu-bar vaadin-menu-bar-button',
      displayName: 'Buttons',
      properties: [
        {
          propertyName: 'background-color',
          displayName: 'Background color',
          editorType: EditorType.color
        },
        {
          propertyName: 'border-color',
          displayName: 'Border color',
          editorType: EditorType.color
        },
        {
          propertyName: 'border-width',
          displayName: 'Border width',
          editorType: EditorType.range,
          presets: presets.basicBorderSize,
          icon: 'square'
        },
        {
          propertyName: 'border-radius',
          displayName: 'Border radius',
          editorType: EditorType.range,
          presets: presets.lumoBorderRadius,
          icon: 'square'
        },
        {
          propertyName: '--lumo-button-size',
          displayName: 'Size',
          editorType: EditorType.range,
          presets: presets.lumoSize,
          icon: 'square'
        },
        {
          propertyName: 'padding-inline',
          displayName: 'Padding',
          editorType: EditorType.range,
          presets: presets.lumoSpace,
          icon: 'square'
        }
      ]
    },
    {
      selector: 'vaadin-menu-bar vaadin-menu-bar-button vaadin-menu-bar-item',
      displayName: 'Button labels',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Text color',
          editorType: EditorType.color,
          presets: presets.lumoTextColor
        },
        {
          propertyName: 'font-size',
          displayName: 'Font size',
          editorType: EditorType.range,
          presets: presets.lumoFontSize,
          icon: 'font'
        },
        {
          propertyName: 'font-weight',
          displayName: 'Bold',
          editorType: EditorType.checkbox,
          checkedValue: 'bold'
        }
      ]
    },
    {
      selector: 'vaadin-menu-bar-overlay::part(overlay)',
      displayName: 'Menus',
      properties: [
        {
          propertyName: 'background-color',
          displayName: 'Background color',
          editorType: EditorType.color
        },
        {
          propertyName: 'border-color',
          displayName: 'Border color',
          editorType: EditorType.color
        },
        {
          propertyName: 'border-width',
          displayName: 'Border width',
          editorType: EditorType.range,
          presets: presets.basicBorderSize,
          icon: 'square'
        },
        {
          propertyName: 'border-radius',
          displayName: 'Border radius',
          editorType: EditorType.range,
          presets: presets.lumoBorderRadius,
          icon: 'square'
        },
        {
          propertyName: 'padding',
          displayName: 'Padding',
          editorType: EditorType.range,
          presets: presets.lumoSpace,
          icon: 'square'
        }
      ]
    },
    {
      selector: 'vaadin-menu-bar-overlay vaadin-menu-bar-item',
      displayName: 'Menu items',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Text color',
          editorType: EditorType.color,
          presets: presets.lumoTextColor
        },
        {
          propertyName: 'font-size',
          displayName: 'Font size',
          editorType: EditorType.range,
          presets: presets.lumoFontSize,
          icon: 'font'
        },
        {
          propertyName: 'font-weight',
          displayName: 'Bold',
          editorType: EditorType.checkbox,
          checkedValue: 'bold'
        }
      ]
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
