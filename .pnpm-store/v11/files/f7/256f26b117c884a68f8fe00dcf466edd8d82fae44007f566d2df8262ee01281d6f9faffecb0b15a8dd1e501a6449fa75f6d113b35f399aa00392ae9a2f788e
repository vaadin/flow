/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { usageStatistics } from '@vaadin/vaadin-usage-statistics/vaadin-usage-statistics.js';
import { idlePeriod } from './async.js';
import { Debouncer, enqueueDebouncer } from './debounce.js';
import { DirMixin } from './dir-mixin.js';

if (!window.Vaadin) {
  window.Vaadin = {};
}

/**
 * Array of Vaadin custom element classes that have been finalized.
 */
if (!window.Vaadin.registrations) {
  window.Vaadin.registrations = [];
}

if (!window.Vaadin.developmentModeCallback) {
  window.Vaadin.developmentModeCallback = {};
}

window.Vaadin.developmentModeCallback['vaadin-usage-statistics'] = function () {
  usageStatistics();
};

let statsJob;

const registered = new Set();

export const ElementMixin = (superClass) =>
  class VaadinElementMixin extends DirMixin(superClass) {
    /** @protected */
    static _ensureRegistrations() {
      const { is } = this;

      // Registers a class prototype for telemetry purposes.
      if (is && !registered.has(is)) {
        window.Vaadin.registrations.push(this);
        registered.add(is);

        const callback = window.Vaadin.developmentModeCallback;
        if (callback) {
          statsJob = Debouncer.debounce(statsJob, idlePeriod, () => {
            callback['vaadin-usage-statistics']();
          });
          enqueueDebouncer(statsJob);
        }
      }
    }

    constructor() {
      super();

      if (document.doctype === null) {
        console.warn(
          'Vaadin components require the "standards mode" declaration. Please add <!DOCTYPE html> to the HTML document.',
        );
      }

      this.constructor._ensureRegistrations();
    }
  };
