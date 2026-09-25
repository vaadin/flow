/**
 * @license
 * Copyright (c) 2025 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { deepMergePartials } from './object-utils.js';

/**
 * A mixin that allows to set partial I18N properties.
 *
 * Subclasses provide their default values by overriding the
 * `defaultI18n` static getter:
 *
 * ```js
 * static get defaultI18n() {
 *   return { foo: 'Foo', bar: 'Bar' };
 * }
 * ```
 */
export const I18nMixin = (superClass) =>
  class I18nMixinClass extends superClass {
    static get properties() {
      return {
        // Even though the property is overridden by a custom getter/setter, it needs to be declared here to initialize
        // __effectiveI18n properly if the i18n property is set before upgrading the element.
        i18n: {
          type: Object,
        },

        /** @private */
        __effectiveI18n: {
          type: Object,
          sync: true,
        },
      };
    }

    /**
     * Default I18N values. Must be overridden by subclasses with actual defaults.
     *
     * @protected
     * @return {Object}
     */
    static get defaultI18n() {
      return {};
    }

    constructor() {
      super();

      this.i18n = deepMergePartials({}, this.constructor.defaultI18n);
    }

    /**
     * The object used to localize this component. To change the default
     * localization, replace this with an object that provides all properties, or
     * just the individual properties you want to change.
     *
     * Should be overridden by subclasses to provide a custom JSDoc with the
     * default I18N properties.
     *
     * @type {Object}
     */
    get i18n() {
      return this.__customI18n;
    }

    set i18n(value) {
      if (value === this.__customI18n) {
        return;
      }
      this.__customI18n = value;
      this.__effectiveI18n = deepMergePartials({}, this.constructor.defaultI18n, this.__customI18n);
    }
  };
