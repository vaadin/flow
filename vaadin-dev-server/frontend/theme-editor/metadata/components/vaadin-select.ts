import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';

export default {
  tagName: 'vaadin-select',
  displayName: 'Select',
  elements: [
    {
      selector: 'vaadin-select::part(input-field)',
      displayName: 'Field',
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
          propertyName: 'height',
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
      selector: 'vaadin-select vaadin-select-value-button>vaadin-select-item',
      displayName: 'Field text',
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
      selector: 'vaadin-select::part(toggle-button)',
      displayName: 'Field toggle button',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Color',
          editorType: EditorType.color,
          presets: presets.lumoTextColor
        },
        {
          propertyName: 'font-size',
          displayName: 'Size',
          editorType: EditorType.range,
          presets: presets.lumoFontSize,
          icon: 'font'
        }
      ]
    },
    {
      selector: 'vaadin-select::part(label)',
      displayName: 'Label',
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
      selector: 'vaadin-select::part(helper-text)',
      displayName: 'Helper text',
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
      selector: 'vaadin-select::part(error-message)',
      displayName: 'Error message',
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
      selector: 'vaadin-select-overlay::part(overlay)',
      displayName: 'Overlay',
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
      selector: 'vaadin-select-overlay vaadin-select-item',
      displayName: 'Overlay items',
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
      selector: 'vaadin-select-overlay vaadin-select-item::part(checkmark)::before',
      displayName: 'Overlay item checkmark',
      properties: [
        {
          propertyName: 'color',
          displayName: 'Color',
          editorType: EditorType.color,
          presets: presets.lumoTextColor
        },
        {
          propertyName: 'font-size',
          displayName: 'Size',
          editorType: EditorType.range,
          presets: presets.lumoFontSize,
          icon: 'font'
        }
      ]
    }
  ],
  async setupElement(select: any) {
    // Apply overlay class name
    select.overlayClass = select.getAttribute('class');
    // Setup items
    select.items = [{ label: 'Item', value: 'value' }];
    // Select value
    select.value = 'value';
    // Open overlay
    select.opened = true;
    // Wait for overlay to open
    await new Promise((resolve) => setTimeout(resolve, 10));
  },
  async cleanupElement(select: any) {
    select.opened = false;
  }
} as ComponentMetadata;
