/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { InputConstraintsMixin } from './input-constraints-mixin.js';

/**
 * A mixin to provide `pattern` property.
 *
 * @polymerMixin
 * @mixes InputConstraintsMixin
 */
export const PatternMixin = (superclass) =>
  class PatternMixinClass extends InputConstraintsMixin(superclass) {
    static get properties() {
      return {
        /**
         * A regular expression that the value is checked against.
         * The pattern must match the entire value, not just some subset.
         */
        pattern: {
          type: String,
        },
      };
    }

    static get delegateAttrs() {
      return [...super.delegateAttrs, 'pattern'];
    }

    static get constraints() {
      return [...super.constraints, 'pattern'];
    }
  };
