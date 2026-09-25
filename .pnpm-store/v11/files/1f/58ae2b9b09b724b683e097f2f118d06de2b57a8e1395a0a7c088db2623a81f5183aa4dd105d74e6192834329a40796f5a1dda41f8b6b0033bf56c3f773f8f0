/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { CSSPropertyObserver } from './css-property-observer.js';
import { injectLumoStyleSheet, removeLumoStyleSheet } from './css-utils.js';
import { parseStyleSheets } from './lumo-modules.js';

export function getLumoInjectorPropName(lumoInjector) {
  return `--_lumo-${lumoInjector.is}-inject`;
}

/**
 * Implements auto-injection of CSS styles from document style sheets
 * into the Shadow DOM of corresponding Vaadin components.
 *
 * Styles to be injected are defined as reusable modules using the
 * following syntax, based on media queries and custom properties:
 *
 * ```css
 * \@media lumo_base-field {
 *  #label {
 *    color: gray;
 *  }
 * }
 *
 * \@media lumo_text-field {
 *   #input {
 *     color: yellow;
 *   }
 * }
 *
 * \@media lumo_email-field {
 *   #input {
 *     color: green;
 *   }
 * }
 *
 * html {
 *   --_lumo-vaadin-text-field-inject: 1;
 *   --_lumo-vaadin-text-field-inject-modules:
 *      lumo_base-field,
 *      lumo_text-field;
 *
 *   --_lumo-vaadin-email-field-inject: 1;
 *   --_lumo-vaadin-email-field-inject-modules:
 *      lumo_base-field,
 *      lumo_email-field;
 * }
 * ```
 *
 * The class observes the custom property `--_lumo-{tagName}-inject`,
 * which indicates whether styles are present for the given component
 * in the document style sheets. When the property is set to `1`, the
 * class recursively searches all document style sheets for CSS modules
 * listed in the `--_lumo-{tagName}-inject-modules` property that apply to
 * the given component tag name. The found rules are then injected
 * into the component's Shadow DOM using the `adoptedStyleSheets` API,
 * in the order specified in the `--_lumo-{tagName}-inject-modules` property.
 * The same module can be used in multiple components.
 *
 * The class also removes the injected styles when the property is set to `0`.
 *
 * If a root element is provided, the class will additionally search for
 * CSS modules in the root element's style sheets. This is useful for
 * embedded Flow applications that are fully isolated from the main document
 * and load styles into a component’s Shadow DOM instead of the main document.
 *
 * WARNING: For internal use only. Do not use this class in custom components.
 *
 * @private
 */
export class LumoInjector {
  /** @type {Document | ShadowRoot} */
  #root;

  /** @type {CSSPropertyObserver} */
  #cssPropertyObserver;

  /** @type {Map<string, CSSStyleSheet>} */
  #styleSheetsByTag = new Map();

  /** @type {Map<string, Set<HTMLElement>>} */
  #componentsByTag = new Map();

  constructor(root = document) {
    this.#root = root;
    this.handlePropertyChange = this.handlePropertyChange.bind(this);
    this.#cssPropertyObserver = CSSPropertyObserver.for(root);
    this.#cssPropertyObserver.addEventListener('property-changed', this.handlePropertyChange);
  }

  disconnect() {
    this.#cssPropertyObserver.removeEventListener('property-changed', this.handlePropertyChange);
    this.#styleSheetsByTag.clear();
    this.#componentsByTag.values().forEach((components) => components.forEach(removeLumoStyleSheet));
  }

  /**
   * Forces all monitored components to re-evaluate and update their
   * injected styles.
   *
   * This method can be used to force LumoInjector to clean up component
   * styles synchonously after the Lumo stylesheet has been removed from
   * the root element. Without this, there may be a short FOUC, when the
   * Lumo styles are already removed from the root but still present in
   * the component Shadow DOMs, since those are removed asynchronously on
   * `transitionstart` (CSSPropertyObserver).
   */
  forceUpdate() {
    for (const tagName of this.#styleSheetsByTag.keys()) {
      this.#updateStyleSheet(tagName);
    }
  }

  /**
   * Adds a component to the list of elements monitored for style injection.
   * If the styles have already been detected, they are injected into the
   * component's shadow DOM immediately. Otherwise, the class watches the
   * custom property `--_lumo-{tagName}-inject` to trigger injection when
   * the styles are added to the document or root element.
   *
   * @param {HTMLElement} component
   */
  componentConnected(component) {
    const { lumoInjector } = component.constructor;
    const { is: tagName } = lumoInjector;

    this.#componentsByTag.set(tagName, this.#componentsByTag.get(tagName) ?? new Set());
    this.#componentsByTag.get(tagName).add(component);

    const stylesheet = this.#styleSheetsByTag.get(tagName);
    if (stylesheet) {
      if (stylesheet.cssRules.length > 0) {
        injectLumoStyleSheet(component, stylesheet);
      }
      return;
    }

    this.#initStyleSheet(tagName);

    const propName = getLumoInjectorPropName(lumoInjector);
    this.#cssPropertyObserver.observe(propName);
  }

  /**
   * Removes the component from the list of elements monitored for
   * style injection and cleans up any previously injected styles.
   *
   * @param {HTMLElement} component
   */
  componentDisconnected(component) {
    const { is: tagName } = component.constructor.lumoInjector;
    this.#componentsByTag.get(tagName)?.delete(component);

    removeLumoStyleSheet(component);
  }

  handlePropertyChange(event) {
    const { propertyName } = event.detail;
    const tagName = propertyName.match(/^--_lumo-(.*)-inject$/u)?.[1];
    if (tagName) {
      this.#updateStyleSheet(tagName);
    }
  }

  #initStyleSheet(tagName) {
    this.#styleSheetsByTag.set(tagName, new CSSStyleSheet());
    this.#updateStyleSheet(tagName);
  }

  #updateStyleSheet(tagName) {
    const { tags, modules } = parseStyleSheets(this.#rootStyleSheets);

    const cssText = (tags.get(tagName) ?? [])
      .flatMap((moduleName) => modules.get(moduleName) ?? [])
      .map((rule) => rule.cssText)
      .join('\n');

    const stylesheet = this.#styleSheetsByTag.get(tagName);
    stylesheet.replaceSync(cssText);

    this.#componentsByTag.get(tagName)?.forEach((component) => {
      if (cssText) {
        injectLumoStyleSheet(component, stylesheet);
      } else {
        removeLumoStyleSheet(component);
      }
    });
  }

  get #rootStyleSheets() {
    let styleSheets = new Set();

    for (const root of [this.#root, document]) {
      styleSheets = styleSheets.union(new Set(root.styleSheets));
      styleSheets = styleSheets.union(new Set(root.adoptedStyleSheets));
    }

    return [...styleSheets];
  }
}
