/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { PolymerElement } from '@polymer/polymer';
import { templatize } from '@polymer/polymer/lib/utils/templatize.js';
import { defineCustomElement } from '@vaadin/component-base/src/define.js';

export class Templatizer extends PolymerElement {
  static get is() {
    return 'vaadin-template-renderer-templatizer';
  }

  static create(component, template) {
    const templatizer = new this();
    templatizer.__template = template;
    templatizer.__component = component;
    return templatizer;
  }

  constructor() {
    super();

    this.__template = null;
    this.__component = null;
    this.__TemplateClass = null;
    this.__templateInstances = new Set();
  }

  /**
   * If the template instance was created by this templatizer's instance and is still attached to DOM,
   * it only re-renders the instance with the new properties.
   * Otherwise, it disposes of the old template instance (if it exists),
   * creates a new template instance with the given properties and renders the instance's root to the element.
   */
  render(element, properties = {}) {
    let instance = element.__templateInstance;

    if (this.__hasTemplateInstance(instance) && this.__isTemplateInstanceAttachedToDOM(instance)) {
      this.__updateProperties(instance, properties);
      return;
    }

    if (this.__hasTemplateInstance(instance)) {
      this.__disposeOfTemplateInstance(instance);
    }

    instance = this.__createTemplateInstance(properties);
    element.__templateInstance = instance;
    element.innerHTML = '';
    element.appendChild(instance.root);
  }

  /** @private */
  __updateProperties(instance, properties) {
    // The Polymer uses `===` to check whether a property is changed and should be re-rendered.
    // This means, object properties won't be re-rendered when mutated inside.
    // This workaround forces the `item` property to re-render even
    // the new item is strictly equal to the old item.
    if (instance.item === properties.item) {
      instance._setPendingProperty('item');
    }

    instance.__properties = properties;
    instance.setProperties(properties);
  }

  /** @private */
  __createTemplateInstance(properties) {
    this.__createTemplateClass(properties);

    const instance = new this.__TemplateClass(properties);
    instance.__properties = properties;

    this.__templateInstances.add(instance);

    return instance;
  }

  /** @private */
  __disposeOfTemplateInstance(instance) {
    this.__templateInstances.delete(instance);
  }

  /** @private */
  __hasTemplateInstance(instance) {
    return this.__templateInstances.has(instance);
  }

  /** @private */
  __isTemplateInstanceAttachedToDOM(instance) {
    // The edge-case case when the template is empty
    if (instance.children.length === 0) {
      return false;
    }

    return !!instance.children[0].parentElement;
  }

  /** @private */
  __createTemplateClass(properties) {
    if (this.__TemplateClass) {
      return;
    }

    const instanceProps = Object.keys(properties).reduce((accum, key) => {
      return { ...accum, [key]: true };
    }, {});

    this.__TemplateClass = templatize(this.__template, this, {
      // Events handled by declarative event listeners
      // (`on-event="handler"`) will be decorated with a `model` property pointing
      // to the template instance that stamped it.
      parentModel: true,
      // This property prevents the template instance properties
      // from passing into the `forwardHostProp` callback
      instanceProps,

      // When changing a property of the data host component, this callback forwards
      // the changed property to the template instances so that cause their re-rendering.
      forwardHostProp(prop, value) {
        this.__templateInstances.forEach((instance) => {
          instance.forwardHostProp(prop, value);
        });
      },

      notifyInstanceProp(instance, path, value) {
        let rootProperty;

        // Extracts the root property name from the path
        rootProperty = path.split('.')[0];
        // Capitalizes the property name
        rootProperty = rootProperty[0].toUpperCase() + rootProperty.slice(1);

        const callback = `_on${rootProperty}PropertyChanged`;

        if (this[callback]) {
          this[callback](instance, path, value);
        }
      },
    });
  }
}

defineCustomElement(Templatizer);
