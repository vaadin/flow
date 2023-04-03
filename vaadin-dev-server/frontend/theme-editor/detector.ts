import { css } from 'lit';
import { ComponentTheme, createScopedSelector, SelectorScope, ThemeScope } from './model';
import { ComponentElementMetadata, ComponentMetadata } from './metadata/model';
import { ComponentReference } from '../component-util';
import { injectGlobalCss } from './styles';

const measureElementClassname = '__vaadin-theme-editor-measure-element';
const pseudoRegex = /((::before)|(::after))$/;
const partNameRegex = /::part\(([\w\d_-]+)\)$/;

injectGlobalCss(css`
  .__vaadin-theme-editor-measure-element {
    position: absolute;
    top: 0;
    left: 0;
    visibility: hidden;
  }
`);

export async function detectTheme(metadata: ComponentMetadata): Promise<ComponentTheme> {
  const componentTheme = new ComponentTheme(metadata);
  const element = document.createElement(metadata.tagName);
  element.classList.add(measureElementClassname);
  document.body.append(element);

  // If component has a custom setup function, run it
  if (metadata.setupElement) {
    await metadata.setupElement(element);
  }

  const scope: SelectorScope = {
    themeScope: ThemeScope.local,
    localClassName: measureElementClassname
  };

  try {
    metadata.elements.forEach((elementMetadata) => {
      // Apply state attribute if it is necessary for measuring sub-element
      applyStateAttribute(element, elementMetadata, scope, true);

      let scopedSelector = createScopedSelector(elementMetadata, scope);
      // Pseudo-element needs to be queried in getComputedStyle separately
      const pseudoMatch = scopedSelector.match(pseudoRegex);
      scopedSelector = scopedSelector.replace(pseudoRegex, '');

      // We can not access shadow DOM parts using document.querySelector, so we
      // need to split accessing those into a second query
      const partNameMatch = scopedSelector.match(partNameRegex);
      const lightDomSelector = scopedSelector.replace(partNameRegex, '');

      let subElement = document.querySelector(lightDomSelector) as HTMLElement;
      // If we target a part in shadow DOM, query for that within shadow DOM
      if (subElement && partNameMatch) {
        const partName = partNameMatch[1];
        const shadowDomSelector = `[part~="${partName}"]`;
        subElement = subElement.shadowRoot!.querySelector(shadowDomSelector) as HTMLElement;
      }

      if (!subElement) {
        return;
      }

      // Disable transitions on measured sub-element to avoid having to wait for
      // state attribute changes to apply
      subElement.style.transition = 'none';

      const pseudoName = pseudoMatch ? pseudoMatch[1] : null;
      const elementStyles = getComputedStyle(subElement, pseudoName);

      elementMetadata.properties.forEach((property) => {
        const propertyValue = elementStyles.getPropertyValue(property.propertyName) || property.defaultValue || '';
        componentTheme.updatePropertyValue(elementMetadata.selector, property.propertyName, propertyValue);
      });

      // Clear state attribute after measuring
      applyStateAttribute(element, elementMetadata, scope, false);
    });
  } finally {
    try {
      // If component has a custom cleanup function, run it
      if (metadata.cleanupElement) {
        await metadata.cleanupElement(element);
      }
    } finally {
      element.remove();
    }
  }

  return componentTheme;
}

function applyStateAttribute(
  element: HTMLElement,
  elementMetadata: ComponentElementMetadata,
  scope: SelectorScope,
  apply: boolean
) {
  // Skip if metadata does not define state attribute
  if (!elementMetadata.stateAttribute) {
    return;
  }

  // If metadata defines custom element on which to apply the state attribute,
  // look it up
  if (elementMetadata.stateElementSelector) {
    const scopedSelector = createScopedSelector(
      {
        ...elementMetadata,
        selector: elementMetadata.stateElementSelector
      },
      scope
    );
    element = document.querySelector(scopedSelector) as HTMLElement;
  }

  if (!element) {
    return;
  }

  if (apply) {
    element.setAttribute(elementMetadata.stateAttribute, '');
  } else {
    element.removeAttribute(elementMetadata.stateAttribute);
  }
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
  const label = element.querySelector('label');
  if (label && label.textContent) {
    return sanitizeText(label.textContent);
  }

  // use text content
  const text = element.textContent;
  return text ? sanitizeText(text) : null;
}
