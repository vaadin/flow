/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

window.Vaadin ||= {};
window.Vaadin.featureFlags ||= {};

function dashToCamelCase(dash) {
  return dash.replace(/-[a-z]/gu, (m) => m[1].toUpperCase());
}

const experimentalMap = {};

export function defineCustomElement(CustomElement, version = '24.8.14') {
  Object.defineProperty(CustomElement, 'version', {
    get() {
      return version;
    },
  });

  if (CustomElement.experimental) {
    const featureFlagKey =
      typeof CustomElement.experimental === 'string'
        ? CustomElement.experimental
        : `${dashToCamelCase(CustomElement.is.split('-').slice(1).join('-'))}Component`;

    if (!window.Vaadin.featureFlags[featureFlagKey] && !experimentalMap[featureFlagKey]) {
      // Add setter to define experimental component when it's set to true
      experimentalMap[featureFlagKey] = new Set();
      experimentalMap[featureFlagKey].add(CustomElement);

      Object.defineProperty(window.Vaadin.featureFlags, featureFlagKey, {
        get() {
          return experimentalMap[featureFlagKey].size === 0;
        },
        set(value) {
          if (!!value && experimentalMap[featureFlagKey].size > 0) {
            experimentalMap[featureFlagKey].forEach((elementClass) => {
              customElements.define(elementClass.is, elementClass);
            });
            experimentalMap[featureFlagKey].clear();
          }
        },
      });

      return;
    } else if (experimentalMap[featureFlagKey]) {
      // Allow to register multiple components with single flag
      experimentalMap[featureFlagKey].add(CustomElement);
      return;
    }
  }

  const defined = customElements.get(CustomElement.is);
  if (!defined) {
    customElements.define(CustomElement.is, CustomElement);
  } else {
    const definedVersion = defined.version;
    if (definedVersion && CustomElement.version && definedVersion === CustomElement.version) {
      // Just loading the same thing again
      console.warn(`The component ${CustomElement.is} has been loaded twice`);
    } else {
      console.error(
        `Tried to define ${CustomElement.is} version ${CustomElement.version} when version ${defined.version} is already in use. Something will probably break.`,
      );
    }
  }
}
