import { ComponentTheme } from './model';
import { ComponentMetadata } from './metadata/model';
import { ComponentReference } from '../component-util';

export function detectTheme(metadata: ComponentMetadata): ComponentTheme {
  const componentTheme = new ComponentTheme(metadata);
  const element = document.createElement(metadata.tagName);
  element.style.visibility = 'hidden';
  document.body.append(element);

  try {
    // Host
    const hostStyles = getComputedStyle(element);

    metadata.properties.forEach((property) => {
      const propertyValue = hostStyles.getPropertyValue(property.propertyName);
      componentTheme.updatePropertyValue(null, property.propertyName, propertyValue);
    });

    // Parts
    metadata.parts.forEach((part) => {
      const partElement = element.shadowRoot?.querySelector(`[part~="${part.partName}"]`);
      if (!partElement) {
        return;
      }
      const partStyles = getComputedStyle(partElement);

      part.properties.forEach((property) => {
        const propertyValue = partStyles.getPropertyValue(property.propertyName);
        componentTheme.updatePropertyValue(part.partName, property.propertyName, propertyValue);
      });
    });
  } finally {
    element.remove();
  }

  return componentTheme;
}

function sanitizeText(text: string) {
  return text.trim();
}

export function detectElementDisplayName(component: ComponentReference) {
  const element = component.element;
  if (!element) {
    return null;
  }

  // check for label
  const label = element.shadowRoot?.querySelector('label');
  if (label && label.textContent) {
    return sanitizeText(label.textContent);
  }

  // use text content
  const text = element.textContent;
  return text ? sanitizeText(text) : null;
}
