/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { adoptStyles } from 'lit';

/**
 * Returns the effective styles that should apply to the component
 * in the correct order, to place injected stylesheet after styles
 * defined with `static styles` and before any custom theme styles
 * that the user provided using `registerStyles()` function.
 *
 * @param {HTMLElement} component
 * @return {CSSStyleSheet[]}
 */
function getEffectiveStyles(component) {
  const { baseStyles, themeStyles, elementStyles, lumoInjector } = component.constructor;
  const lumoStyleSheet = component.__lumoStyleSheet;

  if (lumoStyleSheet) {
    const base = lumoInjector.includeBaseStyles ? (baseStyles ?? elementStyles) : [];
    return [...base, lumoStyleSheet, ...(themeStyles ?? [])];
  }

  return elementStyles;
}

/**
 * Apply styles on the instance of the component in the correct order.
 *
 * @param {HTMLElement} component
 */
export function applyInstanceStyles(component) {
  adoptStyles(component.shadowRoot, getEffectiveStyles(component));
}

/**
 * Injects the given stylesheet into the shadow root of the component
 * through the adoptedStyleSheets API. This will override any styles
 * that were injected previously since we only expect one stylesheet
 * to be injected into each component.
 *
 * @param {HTMLElement} component
 * @param {CSSStyleSheet} styleSheet
 */
export function injectLumoStyleSheet(component, styleSheet) {
  // Store the new stylesheet so that it can be removed later.
  component.__lumoStyleSheet = styleSheet;
  applyInstanceStyles(component);
}

/**
 * Removes the stylesheet from the component's shadow root that was added
 * by the `injectLumoStyleSheet` function.
 *
 * @param {HTMLElement} component
 */
export function removeLumoStyleSheet(component) {
  component.__lumoStyleSheet = undefined;
  applyInstanceStyles(component);
}
