/**
 * @license
 * Copyright (c) 2023 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * A helper for observing slot changes.
 */
export class SlotObserver {
  constructor(
    slot: HTMLSlotElement,
    callback: (info: { addedNodes: Node[]; currentNodes: Node[]; movedNodes: Node[]; removedNodes: Node[] }) => void,
  );

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
