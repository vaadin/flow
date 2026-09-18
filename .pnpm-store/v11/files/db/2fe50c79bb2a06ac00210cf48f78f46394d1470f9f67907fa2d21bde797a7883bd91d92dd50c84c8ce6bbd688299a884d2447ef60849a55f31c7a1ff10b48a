/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/* eslint-disable es/no-optional-chaining */
import StyleObserver from 'style-observer';
import { extractTagScopedCSSRules } from './css-rules.js';
import { cleanupStyleSheet, injectStyleSheet } from './css-utils.js';

/**
 * Implements auto-injection of component-scoped CSS styles from document
 * style sheets into the Shadow DOM of the corresponding Vaadin components.
 *
 * Styles are scoped to a component using the following syntax:
 *
 * 1. `@media vaadin-text-field { ... }` - a media query with a tag name
 * 2. `@import "styles.css" vaadin-text-field` - an import rule with a tag name
 *
 * The class observes the custom property `--{tagName}-css-inject`,
 * which indicates the presence of styles for the given component in
 * the document style sheets. When the property is set to `1`, the class
 * recursively searches all document style sheets for any CSS rules that
 * are scoped to the given component tag name using the syntax described
 * above. The found rules are then injected into the shadow DOM of all
 * subscribed components through the adoptedStyleSheets API.
 *
 * The class also observes the custom property to remove the styles when
 * the property is set to `0`.
 *
 * If a root element is provided, the class will additionally search for
 * component-scoped styles in the root element's style sheets. This is
 * useful for embedded Flow applications that are fully isolated from
 * the main document and load styles into a component's shadow DOM
 * rather than the main document.
 *
 * WARNING: For internal use only. Do not use this class in custom components.
 */
export class CSSInjector {
  /** @type {Document | ShadowRoot} */
  #root;

  /** @type {Map<string, HTMLElement[]>} */
  #componentsByTag = new Map();

  /** @type {Map<string, CSSStyleSheet>} */
  #styleSheetsByTag = new Map();

  #styleObserver = new StyleObserver((records) => {
    records.forEach((record) => {
      const { property, value, oldValue } = record;
      const tagName = property.slice(2).replace('-css-inject', '');
      if (value === '1') {
        this.#componentStylesAdded(tagName);
      } else if (oldValue === '1') {
        this.#componentStylesRemoved(tagName);
      }
    });
  });

  constructor(root = document) {
    this.#root = root;
  }

  /**
   * Adds a component to the list of elements monitored for component-scoped
   * styles in global style sheets. If the styles have already been detected,
   * they are injected into the component's shadow DOM immediately. Otherwise,
   * the class watches the custom property `--{tagName}-css-inject` to trigger
   * injection when the styles are added to the document or root element.
   *
   * @param {HTMLElement} component
   */
  componentConnected(component) {
    const { is: tagName, cssInjectPropName } = component.constructor;

    if (this.#componentsByTag.has(tagName)) {
      this.#componentsByTag.get(tagName).add(component);
    } else {
      this.#componentsByTag.set(tagName, new Set([component]));
    }

    const stylesheet = this.#styleSheetsByTag.get(tagName);
    if (stylesheet) {
      injectStyleSheet(component, stylesheet);
      return;
    }

    // If styles for custom property are already loaded for this root,
    // store corresponding tag name so that we can inject styles
    const value = getComputedStyle(this.#rootHost).getPropertyValue(cssInjectPropName);
    if (value === '1') {
      this.#componentStylesAdded(tagName);
    }

    // Observe custom property that would trigger injection for this class
    this.#styleObserver.observe(this.#rootHost, cssInjectPropName);
  }

  /**
   * Removes the component from the list of elements monitored for
   * component-scoped styles and cleans up any previously injected
   * styles from the component's shadow DOM.
   *
   * @param {HTMLElement} component
   */
  componentDisconnected(component) {
    const { is: tagName } = component.constructor;

    cleanupStyleSheet(component);

    this.#componentsByTag.get(tagName)?.delete(component);
  }

  #componentStylesAdded(tagName) {
    const stylesheet = this.#styleSheetsByTag.get(tagName) || new CSSStyleSheet();

    const cssText = this.#extractComponentScopedCSSRules(tagName)
      .map((rule) => rule.cssText)
      .join('\n');
    stylesheet.replaceSync(cssText);

    this.#componentsByTag.get(tagName)?.forEach((component) => {
      injectStyleSheet(component, stylesheet);
    });

    this.#styleSheetsByTag.set(tagName, stylesheet);
  }

  #componentStylesRemoved(tagName) {
    this.#componentsByTag.get(tagName)?.forEach((component) => {
      cleanupStyleSheet(component);
    });

    this.#styleSheetsByTag.delete(tagName);
  }

  #extractComponentScopedCSSRules(tagName) {
    // Global stylesheets
    const rules = extractTagScopedCSSRules(document, tagName);

    // Scoped stylesheets
    if (this.#root !== document) {
      rules.push(...extractTagScopedCSSRules(this.#root, tagName));
    }

    return rules;
  }

  get #rootHost() {
    return this.#root === document ? this.#root.documentElement : this.#root.host;
  }
}
