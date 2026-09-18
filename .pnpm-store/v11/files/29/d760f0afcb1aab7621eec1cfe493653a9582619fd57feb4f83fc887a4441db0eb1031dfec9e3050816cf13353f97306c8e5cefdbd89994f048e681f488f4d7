/**
 * @license
 * Copyright (c) 2025 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

function deepMerge(target, ...sources) {
  const isArray = (item) => Array.isArray(item);
  const isObject = (item) => item && typeof item === 'object' && !isArray(item);
  const merge = (target, source) => {
    if (isObject(source) && isObject(target)) {
      Object.keys(source).forEach((key) => {
        const sourceValue = source[key];
        if (isObject(sourceValue)) {
          if (!target[key]) {
            target[key] = {};
          }
          merge(target[key], sourceValue);
        } else if (isArray(sourceValue)) {
          target[key] = [...sourceValue];
        } else if (sourceValue !== undefined && sourceValue !== null) {
          target[key] = sourceValue;
        }
      });
    }
  };

  sources.forEach((source) => {
    merge(target, source);
  });

  return target;
}

/**
 * A mixin that allows to set partial I18N properties.
 *
 * @polymerMixin
 */
export const I18nMixin = (defaultI18n, superClass) =>
  class I18nMixinClass extends superClass {
    static get properties() {
      return {
        /** @private */
        // Technically declaring a Polymer property is not needed, as we have a
        // getter/setter for it below. However, the React components currently
        // rely on the Polymer property declaration to detect which properties
        // are available on a custom element, so we add a dummy declaration for
        // it.
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

    constructor() {
      super();

      this.i18n = deepMerge({}, defaultI18n);
    }

    /**
     * The object used to localize this component. To change the default
     * localization, replace this with an object that provides all properties, or
     * just the individual properties you want to change.
     *
     * Should be overridden by subclasses to provide a custom JSDoc with the
     * default I18N properties.
     *
     * @returns {Object}
     */
    get i18n() {
      return this.__customI18n;
    }

    /**
     * The object used to localize this component. To change the default
     * localization, replace this with an object that provides all properties, or
     * just the individual properties you want to change.
     *
     * Should be overridden by subclasses to provide a custom JSDoc with the
     * default I18N properties.
     *
     * @param {Object} value
     */
    set i18n(value) {
      if (value === this.__customI18n) {
        return;
      }
      this.__customI18n = value;
      this.__effectiveI18n = deepMerge({}, defaultI18n, this.__customI18n);
    }
  };
