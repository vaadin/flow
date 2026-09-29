/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';

const observer = new ResizeObserver((entries) => {
  setTimeout(() => {
    entries.forEach((entry) => {
      // Notify child resizables, if any
      if (entry.target.resizables) {
        entry.target.resizables.forEach((resizable) => {
          resizable._onResize(entry.contentRect);
        });
      } else {
        entry.target._onResize(entry.contentRect);
      }
    });
  });
});

/**
 * A mixin that uses a ResizeObserver to listen to host size changes.
 *
 * @polymerMixin
 */
export const ResizeMixin = dedupingMixin(
  (superclass) =>
    class ResizeMixinClass extends superclass {
      /**
       * When true, the parent element resize will be also observed.
       * Override this getter and return `true` to enable this.
       *
       * @protected
       */
      get _observeParent() {
        return false;
      }

      /** @protected */
      connectedCallback() {
        super.connectedCallback();
        observer.observe(this);

        if (this._observeParent) {
          const parent = this.parentNode instanceof ShadowRoot ? this.parentNode.host : this.parentNode;

          if (!parent.resizables) {
            parent.resizables = new Set();
            observer.observe(parent);
          }

          parent.resizables.add(this);
          this.__parent = parent;
        }
      }

      /** @protected */
      disconnectedCallback() {
        super.disconnectedCallback();
        observer.unobserve(this);

        const parent = this.__parent;
        if (this._observeParent && parent) {
          const resizables = parent.resizables;

          if (resizables) {
            resizables.delete(this);

            if (resizables.size === 0) {
              observer.unobserve(parent);
            }
          }

          this.__parent = null;
        }
      }

      /**
       * A handler invoked on host resize. By default, it does nothing.
       * Override the method to implement your own behavior.
       *
       * @protected
       */
      _onResize(_contentRect) {
        // To be implemented.
      }
    },
);
