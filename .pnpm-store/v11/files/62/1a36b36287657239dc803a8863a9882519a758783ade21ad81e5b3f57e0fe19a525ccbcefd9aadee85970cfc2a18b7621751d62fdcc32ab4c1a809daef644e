/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { registerCSSProperty } from '@vaadin/component-base/src/css-utils.js';
import { getLumoInjectorPropName, LumoInjector } from './src/lumo-injector.js';

/**
 * @type {Set<string>}
 */
const registeredProperties = new Set();

/**
 * Find enclosing root for given element to gather style rules from.
 *
 * @param {HTMLElement} element
 * @return {DocumentOrShadowRoot}
 */
export function findRoot(element) {
  const root = element.getRootNode();

  if (root.host && root.host.constructor.version) {
    return findRoot(root.host);
  }

  return root;
}

/**
 * Mixin for internal use only. Do not use it in custom components.
 */
export const LumoInjectionMixin = (superClass) =>
  class LumoInjectionMixinClass extends superClass {
    static finalize() {
      super.finalize();

      const propName = getLumoInjectorPropName(this.lumoInjector);

      // Prevent registering same property twice when a class extends
      // another class using this mixin, since `finalize()` is called
      // by LitElement for all superclasses in the prototype chain.
      if (this.is && !registeredProperties.has(propName)) {
        registeredProperties.add(propName);

        // Initialize custom property for this class with 0 as default
        // so that changing it to 1 would inject styles to instances
        // Use `inherits: true` so that property defined on `<html>`
        // would apply to components instances within shadow roots
        registerCSSProperty({
          name: propName,
          syntax: '<number>',
          inherits: true,
          initialValue: '0',
        });
      }
    }

    static get lumoInjector() {
      return {
        is: this.is,
        includeBaseStyles: false,
      };
    }

    /** @protected */
    connectedCallback() {
      super.connectedCallback();

      const root = findRoot(this);

      // Do not initialize LumoInjector if it's disabled at the root.
      // For example, Copilot does this because it uses its own styles
      // on top of the base styles, and Lumo injection would interfere.
      if (root.__lumoInjectorDisabled) {
        return;
      }

      if (this.isConnected) {
        root.__lumoInjector ||= new LumoInjector(root);
        this.__lumoInjector = root.__lumoInjector;
        this.__lumoInjector.componentConnected(this);
      }
    }

    /** @protected */
    disconnectedCallback() {
      super.disconnectedCallback();

      // Check if LumoInjector is defined. It might be unavailable if the component
      // is moved within the DOM during connectedCallback and becomes disconnected
      // before LumoInjector is assigned.
      if (this.__lumoInjector) {
        this.__lumoInjector.componentDisconnected(this);
        this.__lumoInjector = undefined;
      }
    }
  };
