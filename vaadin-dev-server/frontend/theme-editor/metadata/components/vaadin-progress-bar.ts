import { ComponentMetadata, EditorType } from '../model';
import { presets } from './presets';

export default {
  tagName: 'vaadin-progress-bar',
  displayName: 'ProgressBar',
  elements: [
    {
      selector: 'vaadin-progress-bar::part(bar)',
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
      selector: 'vaadin-progress-bar::part(value)',
      displayName: 'Value',
      properties: [
        {
          propertyName: 'background-color',
          displayName: 'Color',
          editorType: EditorType.color,
          presets: presets.lumoTextColor
        }
      ]
    }
  ]
} as ComponentMetadata;
