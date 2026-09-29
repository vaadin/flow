/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { ReactiveController } from 'lit';

/**
 * A controller for listening on media query changes.
 */
export class MediaQueryController implements ReactiveController {
  /**
   * The CSS media query to evaluate.
   */
  protected query: string;

  /**
   * Function to call when media query changes.
   */
  protected callback: (matches: boolean) => void;

  constructor(query: string, callback: (matches: boolean) => void);

  hostConnected(): void;

  hostDisconnected(): void;
}
