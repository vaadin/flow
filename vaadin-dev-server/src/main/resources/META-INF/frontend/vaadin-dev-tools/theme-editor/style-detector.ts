import { ComponentTheme } from './model';
import { ComponentMetadata } from './metadata/model';

export function detectStyles(metadata: ComponentMetadata): ComponentTheme {
  const componentTheme = new ComponentTheme(metadata.tagName);
  const element = document.createElement(metadata.tagName);
  element.style.display = 'hidden';
  document.body.append(element);

  try {
    metadata.parts.forEach((part) => {
      const partElement = element.shadowRoot?.querySelector(`[part~="${part.partName}"]`);
      if (!partElement) {
        return;
      }
      const partStyles = getComputedStyle(partElement);

      part.properties.forEach((property) => {
        componentTheme.updatePropertyValue(
          part.partName,
          property.propertyName,
          partStyles[property.propertyName as any]
        );
      });
    });
  } finally {
    element.remove();
  }

  return componentTheme;
}
