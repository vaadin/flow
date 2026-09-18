/**
 * @license
 * Copyright (c) 2023 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * A helper for observing slot changes.
 */
export class SlotObserver {
  constructor(slot, callback) {
    /** @type HTMLSlotElement */
    this.slot = slot;

    /** @type Function */
    this.callback = callback;

    /** @type {Node[]} */
    this._storedNodes = [];

    this._connected = false;
    this._scheduled = false;

    this._boundSchedule = () => {
      this._schedule();
    };

    this.connect();
    this._schedule();
  }

  /**
   * Activates an observer. This method is automatically called when
   * a `SlotObserver` is created. It should only be called to  re-activate
   * an observer that has been deactivated via the `disconnect` method.
   */
  connect() {
    this.slot.addEventListener('slotchange', this._boundSchedule);
    this._connected = true;
  }

  /**
   * Deactivates the observer. After calling this method the observer callback
   * will not be called when changes to slotted nodes occur. The `connect` method
   * may be subsequently called to reactivate the observer.
   */
  disconnect() {
    this.slot.removeEventListener('slotchange', this._boundSchedule);
    this._connected = false;
  }

  /** @private */
  _schedule() {
    if (!this._scheduled) {
      this._scheduled = true;

      queueMicrotask(() => {
        this.flush();
      });
    }
  }

  /**
   * Run the observer callback synchronously.
   */
  flush() {
    if (!this._connected) {
      return;
    }

    this._scheduled = false;

    this._processNodes();
  }

  /** @private */
  _processNodes() {
    const currentNodes = this.slot.assignedNodes({ flatten: true });

    let addedNodes = [];
    const removedNodes = [];
    const movedNodes = [];

    if (currentNodes.length) {
      addedNodes = currentNodes.filter((node) => !this._storedNodes.includes(node));
    }

    if (this._storedNodes.length) {
      this._storedNodes.forEach((node, index) => {
        const idx = currentNodes.indexOf(node);
        if (idx === -1) {
          removedNodes.push(node);
        } else if (idx !== index) {
          movedNodes.push(node);
        }
      });
    }

    if (addedNodes.length || removedNodes.length || movedNodes.length) {
      this.callback({ addedNodes, currentNodes, movedNodes, removedNodes });
    }

    this._storedNodes = currentNodes;
  }
}
