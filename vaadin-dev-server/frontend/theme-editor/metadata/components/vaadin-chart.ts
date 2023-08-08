import { ComponentMetadata, EditorType } from '../model';
import { standardShapeProperties } from './defaults';

export default {
  tagName: 'vaadin-chart',
  displayName: 'Chart',
  elements: [
    {
      selector: 'vaadin-chart',
      displayName: 'Root element',
      properties: standardShapeProperties
    },
    {
      selector: 'vaadin-chart',
      displayName: 'Data series',
      properties: [
        {
          propertyName: '--vaadin-charts-color-0',
          displayName: 'Color 0',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-1',
          displayName: 'Color 1',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-2',
          displayName: 'Color 2',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-3',
          displayName: 'Color 3',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-4',
          displayName: 'Color 4',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-5',
          displayName: 'Color 5',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-6',
          displayName: 'Color 6',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-7',
          displayName: 'Color 7',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-8',
          displayName: 'Color 8',
          editorType: EditorType.color
        },
        {
          propertyName: '--vaadin-charts-color-9',
          displayName: 'Color 9',
          editorType: EditorType.color
        }
      ]
    }
  ]
} as ComponentMetadata;
