/**
 * @license
 * Copyright (c) 2025 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * Registers a CSS custom property, tolerating the case where the property has
 * already been registered. This can happen when a module is evaluated more than
 * once (e.g. duplicate bundle chunks or two copies of the library on the page),
 * in which case `CSS.registerProperty` throws `InvalidModificationError`. That
 * error is caught and logged as a warning rather than allowed to break loading.
 *
 * @param {PropertyDefinition} definition
 */
export function registerCSSProperty(definition) {
  try {
    CSS.registerProperty(definition);
  } catch (e) {
    if (e instanceof DOMException && e.name === 'InvalidModificationError') {
      console.warn(`The CSS property ${definition.name} has already been registered.`);
    } else {
      throw e;
    }
  }
}

/**
 * Add a `<style>` block with given styles to the document.
 *
 * @param {string} id the id to set on the created element, only for informational purposes
 * @param  {CSSResultGroup[]} styles the styles to add
 */
export const addGlobalStyles = (id, ...styles) => {
  const styleTag = document.createElement('style');
  styleTag.id = id;
  styleTag.textContent = styles.map((style) => style.toString()).join('\n');

  document.head.insertAdjacentElement('afterbegin', styleTag);
};
