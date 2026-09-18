/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupeMixin } from '@open-wc/dedupe-mixin';
import { DisabledMixin } from '@vaadin/a11y-base/src/disabled-mixin.js';
import { DelegateStateMixin } from '@vaadin/component-base/src/delegate-state-mixin.js';
import { InputMixin } from './input-mixin.js';

/**
 * A mixin to manage the checked state.
 *
 * @polymerMixin
 * @mixes DelegateStateMixin
 * @mixes DisabledMixin
 * @mixes InputMixin
 */
export const CheckedMixin = dedupeMixin(
  (superclass) =>
    class CheckedMixinClass extends DelegateStateMixin(DisabledMixin(InputMixin(superclass))) {
      static get properties() {
        return {
          /**
           * True if the element is checked.
           * @type {boolean}
           */
          checked: {
            type: Boolean,
            value: false,
            notify: true,
            reflectToAttribute: true,
            sync: true,
          },
        };
      }

      static get delegateProps() {
        return [...super.delegateProps, 'checked'];
      }

      /**
       * @param {Event} event
       * @protected
       * @override
       */
      _onChange(event) {
        const input = event.target;

        this._toggleChecked(input.checked);
      }

      /** @protected */
      _toggleChecked(checked) {
        this.checked = checked;
      }
    },
);
