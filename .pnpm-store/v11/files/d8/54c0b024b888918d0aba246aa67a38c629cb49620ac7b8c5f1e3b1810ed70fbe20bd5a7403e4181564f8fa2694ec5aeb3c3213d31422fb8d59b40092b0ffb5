/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupeMixin } from '@open-wc/dedupe-mixin';
import { notEqual } from 'lit';
import { get, set } from './path-utils.js';

const caseMap = {};

const CAMEL_TO_DASH = /([A-Z])/gu;

function camelToDash(camel) {
  if (!caseMap[camel]) {
    caseMap[camel] = camel.replace(CAMEL_TO_DASH, '-$1').toLowerCase();
  }
  return caseMap[camel];
}

function upper(name) {
  return name[0].toUpperCase() + name.substring(1);
}

function parseObserver(observerString) {
  const [method, rest] = observerString.split('(');
  const observerProps = rest
    .replace(')', '')
    .split(',')
    .map((prop) => prop.trim());

  return {
    method,
    observerProps,
  };
}

function getOrCreateMap(obj, name) {
  if (!Object.prototype.hasOwnProperty.call(obj, name)) {
    // Clone any existing entries (superclasses)
    obj[name] = new Map(obj[name]);
  }
  return obj[name];
}

const PolylitMixinImplementation = (superclass) => {
  class PolylitMixinClass extends superclass {
    // PolylitMixin, and components using it, force synchronous updates
    // in connectedCallback and for properties that are configured to
    // be sync. This causes Lit's `change-in-update` warning to be
    // logged for almost every component when Lit runs in development
    // mode. Since we intentionally force updates, disable the warning.
    static enabledWarnings = [];

    static createProperty(name, options) {
      if ([String, Boolean, Number, Array].includes(options)) {
        options = {
          type: options,
        };
      }

      if (options && options.reflectToAttribute) {
        options.reflect = true;
      }

      super.createProperty(name, options);
    }

    static getOrCreateMap(name) {
      return getOrCreateMap(this, name);
    }

    /**
     * @protected
     * @override
     */
    static finalize() {
      // Suppress warnings about deprecated overriding ReactiveElement methods
      // as the mixin requires those. See https://github.com/lit/lit/pull/4901
      if (window.litIssuedWarnings) {
        window.litIssuedWarnings.add('no-override-create-property');
        window.litIssuedWarnings.add('no-override-get-property-descriptor');
      }

      super.finalize();

      if (Array.isArray(this.observers)) {
        const complexObservers = this.getOrCreateMap('__complexObservers');

        this.observers.forEach((observer) => {
          const { method, observerProps } = parseObserver(observer);
          complexObservers.set(method, observerProps);
        });
      }
    }

    static addCheckedInitializer(initializer) {
      super.addInitializer((instance) => {
        // Prevent initializer from affecting superclass
        if (instance instanceof this) {
          initializer(instance);
        }
      });
    }

    static getPropertyDescriptor(name, key, options) {
      const defaultDescriptor = super.getPropertyDescriptor(name, key, options);

      let result = defaultDescriptor;

      // Set the key for this property
      this.getOrCreateMap('__propKeys').set(name, key);

      if (options.sync) {
        result = {
          get: defaultDescriptor.get,
          set(value) {
            const oldValue = this[name];

            if (notEqual(value, oldValue)) {
              this[key] = value;
              this.requestUpdate(name, oldValue, options);

              // Enforce synchronous update
              if (this.hasUpdated) {
                this.performUpdate();
              }
            }
          },
          configurable: true,
          enumerable: true,
        };
      }

      if (options.readOnly) {
        const setter = result.set;

        this.addCheckedInitializer((instance) => {
          // This is run during construction of the element
          instance[`_set${upper(name)}`] = function (value) {
            setter.call(instance, value);
          };
        });

        result = {
          get: result.get,
          set() {
            // Do nothing, property is read-only.
          },
          configurable: true,
          enumerable: true,
        };
      }

      if ('value' in options) {
        // Set the default value
        this.addCheckedInitializer((instance) => {
          const value = typeof options.value === 'function' ? options.value.call(instance) : options.value;

          if (options.readOnly) {
            instance[`_set${upper(name)}`](value);
          } else {
            instance[name] = value;
          }
        });
      }

      if (options.observer) {
        const method = options.observer;

        // Set this method
        this.getOrCreateMap('__observers').set(name, method);

        this.addCheckedInitializer((instance) => {
          if (!instance[method]) {
            console.warn(`observer method ${method} not defined`);
          }
        });
      }

      if (options.notify) {
        if (!this.__notifyProps) {
          this.__notifyProps = new Set();
          // eslint-disable-next-line no-prototype-builtins
        } else if (!this.hasOwnProperty('__notifyProps')) {
          // Clone any existing observers (superclasses)
          const notifyProps = this.__notifyProps;
          this.__notifyProps = new Set(notifyProps);
        }

        // Set this method
        this.__notifyProps.add(name);
      }

      if (options.computed) {
        const assignComputedMethod = `__assignComputed${name}`;
        const observer = parseObserver(options.computed);
        this.prototype[assignComputedMethod] = function (...props) {
          this[name] = this[observer.method](...props);
        };

        this.getOrCreateMap('__computedObservers').set(assignComputedMethod, observer.observerProps);
      }

      if (!options.attribute) {
        options.attribute = camelToDash(name);
      }

      return result;
    }

    static get polylitConfig() {
      return {
        asyncFirstRender: false,
      };
    }

    constructor() {
      super();
      this.__hasPolylitMixin = true;
    }

    /** @protected */
    connectedCallback() {
      super.connectedCallback();

      // Components like `vaadin-overlay` are teleported to the body element when opened.
      // If their opened state is set as an attribute, the teleportation happens immediately
      // after they are connected to the DOM. This means they will be outside the scope of
      // querySelectorAll in the parent component's `firstUpdated()`. To ensure their reference
      // is still registered in the $ map, we propagate the reference here.
      const parentHost = this.getRootNode().host;
      if (parentHost && parentHost.__hasPolylitMixin && this.id) {
        parentHost.$ ||= {};
        parentHost.$[this.id] = this;
      }

      const { polylitConfig } = this.constructor;
      if (!this.hasUpdated && !polylitConfig.asyncFirstRender) {
        this.performUpdate();
      }
    }

    /** @protected */
    firstUpdated() {
      super.firstUpdated();

      if (!this.$) {
        this.$ = {};
      }

      [...Object.values(this.$), this.renderRoot].forEach((node) => {
        node.querySelectorAll('[id]').forEach((node) => {
          this.$[node.id] = node;
        });
      });
    }

    /** @protected */
    ready() {}

    /** @protected */
    willUpdate(props) {
      if (this.constructor.__computedObservers) {
        this.__runComplexObservers(props, this.constructor.__computedObservers);
      }
    }

    /** @protected */
    updated(props) {
      const wasReadyInvoked = this.__isReadyInvoked;
      this.__isReadyInvoked = true;

      if (this.constructor.__observers) {
        this.__runObservers(props, this.constructor.__observers);
      }

      if (this.constructor.__complexObservers) {
        this.__runComplexObservers(props, this.constructor.__complexObservers);
      }

      if (this.__dynamicPropertyObservers) {
        this.__runDynamicObservers(props, this.__dynamicPropertyObservers);
      }

      if (this.__dynamicMethodObservers) {
        this.__runComplexObservers(props, this.__dynamicMethodObservers);
      }

      if (this.constructor.__notifyProps) {
        this.__runNotifyProps(props, this.constructor.__notifyProps);
      }

      if (!wasReadyInvoked) {
        this.ready();
      }
    }

    /**
     * Set several properties at once and perform synchronous update.
     * @protected
     */
    setProperties(props) {
      Object.entries(props).forEach(([name, value]) => {
        // Use private key and not setter to not trigger
        // update for properties marked as `sync: true`.
        const key = this.constructor.__propKeys.get(name);
        const oldValue = this[key];
        this[key] = value;
        this.requestUpdate(name, oldValue);
      });

      // Perform sync update
      if (this.hasUpdated) {
        this.performUpdate();
      }
    }

    /** @protected */
    _createMethodObserver(observer) {
      const dynamicObservers = getOrCreateMap(this, '__dynamicMethodObservers');
      const { method, observerProps } = parseObserver(observer);
      dynamicObservers.set(method, observerProps);
    }

    /** @protected */
    _createPropertyObserver(property, method) {
      const dynamicObservers = getOrCreateMap(this, '__dynamicPropertyObservers');
      dynamicObservers.set(method, property);
    }

    /** @private */
    __runComplexObservers(props, observers) {
      observers.forEach((observerProps, method) => {
        if (observerProps.some((prop) => props.has(prop))) {
          if (!this[method]) {
            console.warn(`observer method ${method} not defined`);
          } else {
            this[method](...observerProps.map((prop) => this[prop]));
          }
        }
      });
    }

    /** @private */
    __runDynamicObservers(props, observers) {
      observers.forEach((prop, method) => {
        if (props.has(prop) && this[method]) {
          this[method](this[prop], props.get(prop));
        }
      });
    }

    /** @private */
    __runObservers(props, observers) {
      props.forEach((v, k) => {
        const observer = observers.get(k);
        if (observer !== undefined && this[observer]) {
          this[observer](this[k], v);
        }
      });
    }

    /** @private */
    __runNotifyProps(props, notifyProps) {
      props.forEach((_, k) => {
        if (notifyProps.has(k)) {
          this.dispatchEvent(
            new CustomEvent(`${camelToDash(k)}-changed`, {
              detail: {
                value: this[k],
              },
            }),
          );
        }
      });
    }

    /** @protected */
    _get(path, object) {
      return get(path, object);
    }

    /** @protected */
    _set(path, value, object) {
      set(path, value, object);
    }
  }

  return PolylitMixinClass;
};

export const PolylitMixin = dedupeMixin(PolylitMixinImplementation);
