/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

export function defineCustomElement(CustomElement, version = '24.5.12') {
  Object.defineProperty(CustomElement, 'version', {
    get() {
      return version;
    },
  });

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
