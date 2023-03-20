import {ComponentMetadata, EditorType} from '../model';
import {presets} from "./presets";

export default {
    tagName: 'vaadin-avatar',
    displayName: 'Avatar',
    properties: [{
        propertyName: '--vaadin-avatar-size',
        displayName: 'Size',
        editorType: EditorType.range,
        presets: presets.lumoSize,
        icon: 'square'
    }],
    parts: []
} as ComponentMetadata;