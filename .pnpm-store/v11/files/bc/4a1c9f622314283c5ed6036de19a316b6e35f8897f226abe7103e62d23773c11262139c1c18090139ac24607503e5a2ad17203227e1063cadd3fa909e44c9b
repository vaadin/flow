/**
 * @license
 * Copyright (c) 2023 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * A helper for observing slot or shadow-root changes.
 *
 * When `target` is an `HTMLSlotElement`, the observer listens for `slotchange`
 * on the slot itself and diffs `target.assignedNodes({ flatten: true })`.
 *
 * When `target` is a `ShadowRoot`, the observer listens for `slotchange` events
 * bubbling to it and diffs the **union** of `assignedNodes({ flatten: true })`
 * across every descendant `<slot>`. Cross-slot reassignment of the same node
 * does not change the union and therefore fires no callback.
 *
 * The initial pass runs in a microtask by default. Use the `syncInitial` option
 * when the callback sets state that affects the layout of the component, so that
 * it has its final size once connected. Otherwise consumers that measure it
 * synchronously, such as auto-width columns in `<vaadin-grid>`, would measure the
 * component before that state is applied.
 */
export class SlotObserver {
  /**
   * @param {HTMLSlotElement | DocumentFragment} target
   * @param {Function} callback
   * @param {{ forceInitial?: boolean, syncInitial?: boolean }} options
   */
  constructor(target, callback, options = {}) {
    /** @type {HTMLSlotElement | DocumentFragment} */
    this.target = target;

    /** @type {Function} */
    this.callback = callback;

    /** @type {boolean} */
    this.forceInitial = options.forceInitial;

    /** @type {Node[]} */
    this._storedNodes = [];

    /** @type {boolean} */
    this._isSlot = target instanceof HTMLSlotElement;

    this._connected = false;
    this._scheduled = false;

    this._boundSchedule = () => {
      this._schedule();
    };

    this.connect();

    if (options.syncInitial) {
      this.flush();
    } else {
      this._schedule();
    }
  }

  /**
   * Activates an observer. This method is automatically called when
   * a `SlotObserver` is created. It should only be called to  re-activate
   * an observer that has been deactivated via the `disconnect` method.
   */
  connect() {
    this.target.addEventListener('slotchange', this._boundSchedule);
    this._connected = true;
  }

  /**
   * Deactivates the observer. After calling this method the observer callback
   * will not be called when changes to slotted nodes occur. The `connect` method
   * may be subsequently called to reactivate the observer.
   */
  disconnect() {
    this.target.removeEventListener('slotchange', this._boundSchedule);
    this._connected = false;
  }

  /** @private */
  _schedule() {
    if (!this._scheduled) {
      this._scheduled = true;

      queueMicrotask(() => {
        // Skip if the nodes have already been processed by an explicit `flush()`
        // in the meantime, to avoid running the diff a second time for nothing.
        if (this._scheduled) {
          this.flush();
        }
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
  _collectNodes() {
    const slots = this._isSlot ? [this.target] : [...this.target.querySelectorAll('slot')];
    return [...new Set(slots.flatMap((slot) => slot.assignedNodes({ flatten: true })))];
  }

  /** @private */
  _groupNodesBySlot(nodes) {
    const map = new Map();
    nodes.forEach((node) => {
      const slot = node.assignedSlot;
      map.set(slot, map.get(slot) ?? []);
      map.get(slot).push(node);
    });
    return map;
  }

  /**
   * Collect moved nodes reordered within its current slot,
   * but not those that are assigned to different slot.
   *
   * @private
   */
  _collectMovedNodes(currentNodes) {
    const currentPerSlot = this._groupNodesBySlot(currentNodes);
    const storedPerSlot = this._groupNodesBySlot(this._storedNodes);
    const movedNodes = [];

    currentPerSlot.forEach((nodes, slot) => {
      const stored = storedPerSlot.get(slot) || [];
      // Skip slots whose membership changed: nodes entered or left the slot.
      if (new Set(stored).difference(new Set(nodes)).size > 0) {
        return;
      }
      stored.forEach((node, storedIndex) => {
        if (nodes.indexOf(node) !== storedIndex) {
          movedNodes.push(node);
        }
      });
    });

    return movedNodes;
  }

  /** @private */
  _processNodes() {
    const currentNodes = this._collectNodes();

    const addedNodes = currentNodes.filter((node) => !this._storedNodes.includes(node));
    const removedNodes = this._storedNodes.filter((node) => !currentNodes.includes(node));
    const movedNodes = this._collectMovedNodes(currentNodes);

    // By default, callback is not invoked if there is no child nodes in the slot.
    // Use `forceInitial` flag if needed to also invoke it for the initial state.
    if (addedNodes.length || removedNodes.length || movedNodes.length || this.forceInitial) {
      this.callback({ addedNodes, currentNodes, movedNodes, removedNodes });
    }

    if (this.forceInitial) {
      this.forceInitial = false;
    }

    this._storedNodes = currentNodes;
  }
}
