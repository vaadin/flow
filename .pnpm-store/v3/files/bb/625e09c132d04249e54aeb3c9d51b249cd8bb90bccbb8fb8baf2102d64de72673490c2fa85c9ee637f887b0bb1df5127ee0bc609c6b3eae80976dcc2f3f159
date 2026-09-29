/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupingMixin } from '@polymer/polymer/lib/utils/mixin.js';

const stylesMap = new WeakMap();

/**
 * Get all the styles inserted into root.
 * @param {DocumentOrShadowRoot} root
 * @return {Set<string>}
 */
function getRootStyles(root) {
  if (!stylesMap.has(root)) {
    stylesMap.set(root, new Set());
  }

  return stylesMap.get(root);
}

/**
 * Insert styles into the root.
 * @param {string} styles
 * @param {DocumentOrShadowRoot} root
 */
function insertStyles(styles, root) {
  const style = document.createElement('style');
  style.textContent = styles;

  if (root === document) {
    document.head.appendChild(style);
  } else {
    root.insertBefore(style, root.firstChild);
  }
}

/**
 * Mixin to insert styles into the outer scope to handle slotted components.
 * This is useful e.g. to hide native `<input type="number">` controls.
 *
 * @polymerMixin
 */
export const SlotStylesMixin = dedupingMixin(
  (superclass) =>
    class SlotStylesMixinClass extends superclass {
      /**
       * List of styles to insert into root.
       * @protected
       */
      get slotStyles() {
        return {};
      }

      /** @protected */
      connectedCallback() {
        super.connectedCallback();

        this.__applySlotStyles();
      }

      /** @private */
      __applySlotStyles() {
        const root = this.getRootNode();
        const rootStyles = getRootStyles(root);

        this.slotStyles.forEach((styles) => {
          if (!rootStyles.has(styles)) {
            insertStyles(styles, root);
            rootStyles.add(styles);
          }
        });
      }
    },
);
