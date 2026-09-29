/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';

/**
 * @typedef ReactiveController
 * @type {import('lit').ReactiveController}
 */

/**
 * A mixin for connecting controllers to the element.
 *
 * @polymerMixin
 */
export const ControllerMixin = dedupingMixin((superClass) => {
  // If the superclass extends from LitElement,
  // use its own controllers implementation.
  if (typeof superClass.prototype.addController === 'function') {
    return superClass;
  }

  return class ControllerMixinClass extends superClass {
    constructor() {
      super();

      /**
       * @type {Set<ReactiveController>}
       */
      this.__controllers = new Set();
    }

    /** @protected */
    connectedCallback() {
      super.connectedCallback();

      this.__controllers.forEach((c) => {
        if (c.hostConnected) {
          c.hostConnected();
        }
      });
    }

    /** @protected */
    disconnectedCallback() {
      super.disconnectedCallback();

      this.__controllers.forEach((c) => {
        if (c.hostDisconnected) {
          c.hostDisconnected();
        }
      });
    }

    /**
     * Registers a controller to participate in the element update cycle.
     *
     * @param {ReactiveController} controller
     * @protected
     */
    addController(controller) {
      this.__controllers.add(controller);
      // Call hostConnected if a controller is added after the element is attached.
      if (this.$ !== undefined && this.isConnected && controller.hostConnected) {
        controller.hostConnected();
      }
    }

    /**
     * Removes a controller from the element.
     *
     * @param {ReactiveController} controller
     * @protected
     */
    removeController(controller) {
      this.__controllers.delete(controller);
    }
  };
});
