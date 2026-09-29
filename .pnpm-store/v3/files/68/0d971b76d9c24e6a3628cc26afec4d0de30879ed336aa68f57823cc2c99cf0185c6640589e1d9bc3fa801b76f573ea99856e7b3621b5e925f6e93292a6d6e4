/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { ReactiveController } from 'lit';

export class SlotController extends EventTarget implements ReactiveController {
  /**
   * The controller host element.
   */
  host: HTMLElement;

  /**
   * The slotted node managed by the controller.
   */
  node: HTMLElement;

  /**
   * The list of slotted nodes managed by the controller.
   * Only used when `multiple` property is set to `true`.
   */
  nodes: HTMLElement[];

  protected initialized: boolean;

  protected multiple: boolean;

  protected defaultNode: Node;

  protected defaultId: string;

  constructor(
    host: HTMLElement,
    slotName: string,
    tagName?: string,
    config?: {
      multiple?: boolean;
      observe?: boolean;
      useUniqueId?: boolean;
      uniqueIdPrefix?: string;
      initializer?(host: HTMLElement, node: HTMLElement): void;
    },
  );

  hostConnected(): void;

  /**
   * Return the list of nodes matching the slot managed by the controller.
   */
  getSlotChildren(): Node[];

  /**
   * Return a reference to the node managed by the controller.
   */
  getSlotChild(): Node;

  /**
   * Create and attach default node using the provided tag name, if any.
   */
  protected attachDefaultNode(): Node | undefined;

  /**
   * Run both `initCustomNode` and `initNode` for a custom slotted node.
   */
  protected initAddedNode(node: Node): void;

  /**
   * Run `slotInitializer` for the node managed by the controller.
   */
  protected initNode(node: Node): void;

  /**
   * Override to initialize the newly added custom node.
   */
  protected initCustomNode(node: Node): void;

  /**
   * Override to cleanup slotted node when it's removed.
   */
  protected teardownNode(node: Node): void;

  /**
   * Setup the observer to manage slot content changes.
   */
  protected observeSlot(): void;
}
