import { ComponentTheme, createScopedSelector, SelectorScope, ThemeScope } from './model';
import { ComponentMetadata } from './metadata/model';
import { ComponentReference } from '../component-util';

const measureElementClassname = '__vaadin-theme-editor-measure-element';
const partNameRegex = /::part\(([\w\d_-]+)\)$/;

export function detectTheme(metadata: ComponentMetadata): ComponentTheme {
  const componentTheme = new ComponentTheme(metadata);
  const element = document.createElement(metadata.tagName);
  element.classList.add(measureElementClassname);
  element.style.visibility = 'hidden';
  document.body.append(element);

  const scope: SelectorScope = {
    themeScope: ThemeScope.local,
    localClassName: measureElementClassname
  };

  try {
    metadata.elements.forEach((elementMetadata) => {
      const scopedSelector = createScopedSelector(elementMetadata, scope);
      // We can not access shadow DOM parts using document.querySelector, so we
      // need to split accessing those into a second query
      const partNameMatch = scopedSelector.match(/::part\(([\w\d-]+)\)$/);
      const lightDomSelector = scopedSelector.replace(partNameRegex, '');

      let element = document.querySelector(lightDomSelector);
      // If we target a part in shadow DOM, query for that within shadow DOM
      if (element && partNameMatch) {
        const partName = partNameMatch[1];
        const shadowDomSelector = `[part~="${partName}"]`;
        element = element!.shadowRoot!.querySelector(shadowDomSelector);
      }

      if (!element) {
        return;
      }
      const elementStyles = getComputedStyle(element);

      elementMetadata.properties.forEach((property) => {
        const propertyValue = elementStyles.getPropertyValue(property.propertyName);
        componentTheme.updatePropertyValue(elementMetadata.selector, property.propertyName, propertyValue);
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
  const label = element.shadowRoot && element.shadowRoot.querySelector('label');
  if (label && label.textContent) {
    return sanitizeText(label.textContent);
  }

  // use text content
  const text = element.textContent;
  return text ? sanitizeText(text) : null;
}
