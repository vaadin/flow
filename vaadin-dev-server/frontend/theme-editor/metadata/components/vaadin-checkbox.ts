import {ComponentMetadata, EditorType} from '../model';
import {presets} from "./presets";

export default {
    tagName: 'vaadin-checkbox',
    displayName: 'Checkbox',
    properties: [
        {
            propertyName: '--lumo-primary-color',
            displayName: 'Color',
            editorType: EditorType.color
        },
        {
            propertyName: '--vaadin-checkbox-size',
            displayName: 'Size',
            editorType: EditorType.range,
            presets: presets.lumoSize,
            icon: 'square'
        },
        {
            propertyName: '--lumo-font-size-m',
            displayName: 'Font Size',
            editorType: EditorType.range,
            presets: presets.lumoSize,
            icon: 'square'
        },
        {
            propertyName: '--lumo-body-text-color',
            displayName: 'Text color',
            editorType: EditorType.color,
            presets: presets.lumoTextColor
        }],
    parts: []
} as ComponentMetadata;