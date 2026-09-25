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
 * every descendant `<slot>`. Cross-slot reassignment of the same node does
 * not change the union and therefore fires no callback.
 *
 * The initial pass runs in a microtask by default. Use the `syncInitial` option
 * when the callback sets state that affects the layout of the component, so that
 * it has its final size once connected. Otherwise consumers that measure it
 * synchronously, such as auto-width columns in `<vaadin-grid>`, would measure the
 * component before that state is applied.
 */
export class SlotObserver {
  constructor(
    target: HTMLSlotElement | DocumentFragment,
    callback: (info: { addedNodes: Node[]; currentNodes: Node[]; movedNodes: Node[]; removedNodes: Node[] }) => void,
    options?: { forceInitial?: boolean; syncInitial?: boolean },
  );

  readonly target: HTMLSlotElement | DocumentFragment;

  /**
   * Activates an observer. This method is automatically called when
   * a `SlotObserver` is created. It should only be called to  re-activate
   * an observer that has been deactivated via the `disconnect` method.
   */
  connect(): void;

  /**
   * Deactivates the observer. After calling this method the observer callback
   * will not be called when changes to slotted nodes occur. The `connect` method
   * may be subsequently called to reactivate the observer.
   */
  disconnect(): void;

  /**
   * Run the observer callback synchronously.
   */
  flush(): void;
}
