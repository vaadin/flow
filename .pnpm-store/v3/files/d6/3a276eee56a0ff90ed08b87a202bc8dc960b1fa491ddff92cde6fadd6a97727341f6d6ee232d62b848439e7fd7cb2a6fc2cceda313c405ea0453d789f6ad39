/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { dedupeMixin } from '@open-wc/dedupe-mixin';
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
    static createProperty(name, options) {
      if ([String, Boolean, Number, Array].includes(options)) {
        options = {
          type: options,
        };
      }

      if (options.reflectToAttribute) {
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
            this[key] = value;
            this.requestUpdate(name, oldValue, options);

            // Enforce synchronous update
            if (this.hasUpdated) {
              this.performUpdate();
            }
          },
          configurable: true,
          enumerable: true,
        };
      }

      if (options.readOnly) {
        const setter = defaultDescriptor.set;

        this.addCheckedInitializer((instance) => {
          // This is run during construction of the element
          instance[`_set${upper(name)}`] = function (value) {
            setter.call(instance, value);

            if (options.sync) {
              this.performUpdate();
            }
          };
        });

        result = {
          get: defaultDescriptor.get,
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

        this.getOrCreateMap('__complexObservers').set(assignComputedMethod, observer.observerProps);
      }

      if (!options.attribute) {
        options.attribute = camelToDash(name);
      }

      return result;
    }

    /** @protected */
    firstUpdated() {
      super.firstUpdated();

      if (!this.$) {
        this.$ = {};
      }

      this.renderRoot.querySelectorAll('[id]').forEach((node) => {
        this.$[node.id] = node;
      });
    }

    /** @protected */
    ready() {}

    /** @protected */
    updated(props) {
      if (this.constructor.__observers) {
        this.__runObservers(props, this.constructor.__observers);
      }

      if (this.constructor.__complexObservers) {
        this.__runComplexObservers(props, this.constructor.__complexObservers);
      }

      if (this.__dynamicObservers) {
        this.__runComplexObservers(props, this.__dynamicObservers);
      }

      if (this.constructor.__notifyProps) {
        this.__runNotifyProps(props, this.constructor.__notifyProps);
      }

      if (!this.__isReadyInvoked) {
        this.__isReadyInvoked = true;
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
      const dynamicObservers = getOrCreateMap(this, '__dynamicObservers');
      const { method, observerProps } = parseObserver(observer);
      dynamicObservers.set(method, observerProps);
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
