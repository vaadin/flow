import {ComponentMetadata, EditorType} from '../model';
import {presets} from "./presets";

export default {
    tagName: 'vaadin-text-area',
    displayName: 'TextArea',
    properties: [],
    parts: [
        {
            partName: 'label',
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
                    propertyName: 'background-color',
                    displayName: 'Background color',
                    editorType: EditorType.color
                }
            ]
        },
        {
            partName: 'input-field',
            displayName: 'Input field',
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
                    propertyName: 'background-color',
                    displayName: 'Background color',
                    editorType: EditorType.color
                }
            ]
        },
        {
            partName: 'helper-text',
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
                    propertyName: 'background-color',
                    displayName: 'Background color',
                    editorType: EditorType.color
                }
            ]
        },
        {
            partName: 'error-message',
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
                    propertyName: 'background-color',
                    displayName: 'Background color',
                    editorType: EditorType.color
                }
            ]
        }
    ]
} as ComponentMetadata;
