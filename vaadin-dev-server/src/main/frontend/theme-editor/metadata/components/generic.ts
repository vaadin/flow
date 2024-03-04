import { ComponentMetadata } from '../model';
import { shapeProperties, textProperties } from './defaults';

export function createGenericMetadata(tagName: string): ComponentMetadata {
  const displayName = tagName.charAt(0).toUpperCase() + tagName.slice(1);

  return {
    tagName,
    displayName,
    elements: [
      {
        selector: tagName,
        displayName: 'Element',
        properties: [
          shapeProperties.backgroundColor,
          shapeProperties.borderColor,
          shapeProperties.borderWidth,
          shapeProperties.borderRadius,
          shapeProperties.padding,
          textProperties.textColor,
          textProperties.fontSize,
          textProperties.fontWeight,
          textProperties.fontStyle
        ]
      }
    ]
  };
}
