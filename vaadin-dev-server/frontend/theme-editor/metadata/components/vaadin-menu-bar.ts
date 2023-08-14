import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';
import { fieldProperties, shapeProperties, textProperties } from './defaults';
import { ComponentReference } from '../../../component-util';
import { createDocumentClickEvent } from '../../components/component-overlay-manager';

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
      displayName: 'Overlay',
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
      displayName: 'Menu Items',
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
    // Wait for overlay to open
    await new Promise((resolve) => setTimeout(resolve, 10));
  },
  async cleanupElement(menuBar: any) {
    // Menu bar does not close / remove its overlay when it is removed from DOM,
    // so we need to do it manually
    menuBar._close();
  },
  openOverlay(component: ComponentReference) {
    // Open overlay
    (component.element as any).querySelector('vaadin-menu-bar-button').click();
    const subMenu = (component.element as any).shadowRoot.querySelector('vaadin-menu-bar-submenu');
    if (!subMenu) {
      return;
    }
    const overlay = subMenu.$.overlay;
    if (!overlay) {
      return;
    }
    overlay._storedModeless = overlay.modeless;
    overlay.modeless = true;
    (document as any)._themeEditorDocClickListener = createDocumentClickEvent(subMenu, overlay);
    document.addEventListener('click', (document as any)._themeEditorDocClickListener);
    document.documentElement.removeEventListener('click', subMenu.__itemsOutsideClickListener);
  },
  hideOverlay(component: ComponentReference) {
    const subMenu = (component.element as any).shadowRoot.querySelector('vaadin-menu-bar-submenu');
    if (!subMenu) {
      return;
    }
    const overlay = subMenu.$.overlay;
    if (!overlay) {
      return;
    }
    overlay.close();
    overlay.modeless = overlay._storedModeless;
    delete overlay._storedModeless;
    document.removeEventListener('click', (document as any)._themeEditorDocClickListener);
    document.documentElement.addEventListener('click', subMenu.__itemsOutsideClickListener);
    delete (document as any)._themeEditorDocClickListener;
  }
} as ComponentMetadata;
