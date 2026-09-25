/**
 * @license
 * Copyright (c) 2000 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { registerCSSProperty } from '@vaadin/component-base/src/css-utils.js';
import { CSSPropertyObserver } from './css-property-observer.js';

// Register CSS custom properties for observing theme changes
registerCSSProperty({
  name: '--vaadin-aura-theme',
  syntax: '<number>',
  inherits: true,
  initialValue: '0',
});

registerCSSProperty({
  name: '--vaadin-lumo-theme',
  syntax: '<number>',
  inherits: true,
  initialValue: '0',
});

/**
 * Observes a root (Document or ShadowRoot) for which Vaadin theme is currently applied.
 * Notifies about theme changes by firing a `theme-changed` event.
 *
 * WARNING: For internal use only. Do not use this class in custom components.
 *
 * @private
 */
export class ThemeDetector extends EventTarget {
  /** @type {DocumentOrShadowRoot} */
  #root;
  /** @type {CSSPropertyObserver} */
  #observer;
  /** @type {{ aura: boolean; lumo: boolean }} */
  #themes = { aura: false, lumo: false };
  /** @type {(event: CustomEvent) => void} */
  #boundHandleThemeChange = this.#handleThemeChange.bind(this);

  constructor(root) {
    super();
    this.#root = root;
    this.#detectTheme();

    this.#observer = CSSPropertyObserver.for(this.#root);
    this.#observer.observe('--vaadin-aura-theme');
    this.#observer.observe('--vaadin-lumo-theme');
    this.#observer.addEventListener('property-changed', this.#boundHandleThemeChange);
  }

  get themes() {
    return { ...this.#themes };
  }

  #handleThemeChange(event) {
    const { propertyName } = event.detail;
    if (!['--vaadin-aura-theme', '--vaadin-lumo-theme'].includes(propertyName)) {
      return;
    }

    this.#detectTheme();
    this.dispatchEvent(new CustomEvent('theme-changed'));
  }

  #detectTheme() {
    const rootElement = this.#root.documentElement ?? this.#root.host;
    const style = getComputedStyle(rootElement);
    this.#themes = {
      aura: style.getPropertyValue('--vaadin-aura-theme').trim() === '1',
      lumo: style.getPropertyValue('--vaadin-lumo-theme').trim() === '1',
    };
  }

  disconnect() {
    this.#observer.removeEventListener('property-changed', this.#boundHandleThemeChange);
  }
}
