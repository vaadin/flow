/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { setCancelSyntheticClickEvents } from '@polymer/polymer/lib/utils/settings.js';
import { usageStatistics } from '@vaadin/vaadin-usage-statistics/vaadin-usage-statistics.js';
import { idlePeriod } from './async.js';
import { Debouncer, enqueueDebouncer } from './debounce.js';
import { DirMixin } from './dir-mixin.js';

// This setting affects the legacy Polymer gestures which get activated
// once you import any iron component e.g iron-icon.
// It has to be explicitly disabled to prevent click issues in iOS + VoiceOver
// for buttons that are based on `[role=button]` e.g vaadin-button.
setCancelSyntheticClickEvents(false);

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

/**
 * @polymerMixin
 * @mixes DirMixin
 */
export const ElementMixin = (superClass) =>
  class VaadinElementMixin extends DirMixin(superClass) {
    /** @protected */
    static finalize() {
      super.finalize();

      const { is } = this;

      // Registers a class prototype for telemetry purposes.
      if (is && !registered.has(is)) {
        window.Vaadin.registrations.push(this);
        registered.add(is);

        if (window.Vaadin.developmentModeCallback) {
          statsJob = Debouncer.debounce(statsJob, idlePeriod, () => {
            window.Vaadin.developmentModeCallback['vaadin-usage-statistics']();
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
    }
  };
