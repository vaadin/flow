/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { SlotController } from '@vaadin/component-base/src/slot-controller.js';

/**
 * A controller to create and initialize slotted `<textarea>` element.
 */
export class TextAreaController extends SlotController {
  constructor(host, callback) {
    super(host, 'textarea', 'textarea', {
      initializer: (node, host) => {
        const value = host.getAttribute('value');
        if (value) {
          node.value = value;
        }

        const name = host.getAttribute('name');
        if (name) {
          node.setAttribute('name', name);
        }

        node.id = this.defaultId;

        if (typeof callback === 'function') {
          callback(node);
        }
      },
      useUniqueId: true,
    });
  }
}
