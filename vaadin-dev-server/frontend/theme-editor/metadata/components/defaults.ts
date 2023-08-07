import { CssPropertyMetadata, EditorType } from '../model';
import { presets } from './presets';

export const textProperties = {
  textColor: {
    propertyName: 'color',
    displayName: 'Text color',
    editorType: EditorType.color,
    presets: presets.lumoTextColor
  },
  fontSize: {
    propertyName: 'font-size',
    displayName: 'Font size',
    editorType: EditorType.range,
    presets: presets.lumoFontSize,
    icon: 'font'
  },
  fontWeight: {
    propertyName: 'font-weight',
    displayName: 'Bold',
    editorType: EditorType.checkbox,
    checkedValue: 'bold'
  },
  fontStyle: {
    propertyName: 'font-style',
    displayName: 'Italic',
    editorType: EditorType.checkbox,
    checkedValue: 'italic'
  }
};

export const shapeProperties = {
  backgroundColor: {
    propertyName: 'background-color',
    displayName: 'Background color',
    editorType: EditorType.color
  },
  borderColor: {
    propertyName: 'border-color',
    displayName: 'Border color',
    editorType: EditorType.color
  },
  borderWidth: {
    propertyName: 'border-width',
    displayName: 'Border width',
    editorType: EditorType.range,
    presets: presets.basicBorderSize,
    icon: 'square'
  },
  borderRadius: {
    propertyName: 'border-radius',
    displayName: 'Border radius',
    editorType: EditorType.range,
    presets: presets.lumoBorderRadius,
    icon: 'square'
  },
  padding: {
    propertyName: 'padding',
    displayName: 'Padding',
    editorType: EditorType.range,
    presets: presets.lumoSpace,
    icon: 'square'
  },
  gap: {
    propertyName: 'gap',
    displayName: 'Spacing',
    editorType: EditorType.range,
    presets: presets.lumoSpace,
    icon: 'square'
  }
};

export const fieldProperties = {
  height: {
    propertyName: 'height',
    displayName: 'Size',
    editorType: EditorType.range,
    presets: presets.lumoSize,
    icon: 'square'
  },
  paddingInline: {
    propertyName: 'padding-inline',
    displayName: 'Padding',
    editorType: EditorType.range,
    presets: presets.lumoSpace,
    icon: 'square'
  }
};

export const iconProperties = {
  iconColor: {
    propertyName: 'color',
    displayName: 'Icon color',
    editorType: EditorType.color,
    presets: presets.lumoTextColor
  },
  iconSize: {
    propertyName: 'font-size',
    displayName: 'Icon size',
    editorType: EditorType.range,
    presets: presets.lumoFontSize,
    icon: 'font'
  }
};

export const standardShapeProperties = <CssPropertyMetadata[]>[
  shapeProperties.backgroundColor,
  shapeProperties.borderColor,
  shapeProperties.borderWidth,
  shapeProperties.borderRadius,
  shapeProperties.padding
];

export const standardTextProperties = <CssPropertyMetadata[]>[
  textProperties.textColor,
  textProperties.fontSize,
  textProperties.fontWeight,
  textProperties.fontStyle
];

export const standardIconProperties = <CssPropertyMetadata[]>[iconProperties.iconColor, iconProperties.iconSize];
