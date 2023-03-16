import {ComponentMetadata, EditorType} from '../model';
import {presets} from "./presets";

export default {
    tagName: 'vaadin-progress-bar',
    displayName: 'ProgressBar',
    properties: [],
    parts: [{
        partName: 'bar',
        displayName: 'Bar',
        properties: [
            {
                propertyName: 'background-color',
                displayName: 'Color',
                editorType: EditorType.color
            }
        ]
    },
    {
        partName: 'value',
        displayName: 'Value',
        properties: [
            {
                propertyName: 'background-color',
                displayName: 'Color',
                editorType: EditorType.color,
                presets: presets.lumoTextColor
            }
        ]
    }]
} as ComponentMetadata;