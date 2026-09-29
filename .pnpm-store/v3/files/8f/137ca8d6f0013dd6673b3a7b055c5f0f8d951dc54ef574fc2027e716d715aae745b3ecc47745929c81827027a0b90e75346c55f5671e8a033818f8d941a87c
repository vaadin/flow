/**
 * @license
 * Copyright (c) 2022 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { SlotController } from './slot-controller.js';

/**
 * A controller that observes slotted element mutations, especially ID attribute
 * and the text content, and fires an event to notify host element about those.
 */
export class SlotChildObserveController extends SlotController {
  /**
   * Setup the mutation observer on the node to update ID and notify host.
   * Node doesn't get observed automatically until this method is called.
   */
  protected observeNode(node: Node): void;

  /**
   * Override to restore default node when a custom one is removed.
   */
  protected restoreDefaultNode(): void;

  /**
   * Override to update default node text on property change.
   */
  protected updateDefaultNode(node: Node): void;
}
