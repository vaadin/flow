/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// TypeScript port of com.vaadin.client.communication.ReconnectConfiguration.
// It exposes the reconnect configuration stored in the root node's
// RECONNECT_DIALOG_CONFIGURATION feature and, via bind(), re-runs the
// connection-state handler's configurationUpdated() whenever the configuration
// changes (reactively).

import type { StateTree } from '../flow/StateTree';
import type { MapProperty } from '../flow/nodefeature/MapProperty';
import type { ConnectionStateHandler } from './ConnectionStateHandler';
import { NodeFeatures } from '../../flow/internal/nodefeature/NodeFeatures';
import { Reactive } from '../flow/reactive/Reactive';

// com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap
const DIALOG_TEXT_KEY = 'dialogText';
const DIALOG_TEXT_GAVE_UP_KEY = 'dialogTextGaveUp';
const RECONNECT_ATTEMPTS_KEY = 'reconnectAttempts';
const RECONNECT_ATTEMPTS_DEFAULT = 10000;
const RECONNECT_INTERVAL_KEY = 'reconnectInterval';
const RECONNECT_INTERVAL_DEFAULT = 5000;

/** The slice of Registry ReconnectConfiguration reads. */
interface ReconnectRegistry {
  getStateTree(): StateTree;
}

/**
 * Tracks the reconnect configuration stored in the root node and provides it
 * with an easier to use API. Also triggers
 * {@link ConnectionStateHandler.configurationUpdated} whenever part of the
 * configuration changes.
 */
export class ReconnectConfiguration {
  readonly #registry: ReconnectRegistry;

  /**
   * Creates a new instance using the given registry.
   *
   * @param registry - the registry
   */
  constructor(registry: ReconnectRegistry) {
    this.#registry = registry;
  }

  /**
   * Binds this ReconnectDialogConfiguration to the given
   * {@link ConnectionStateHandler} so that
   * {@link ConnectionStateHandler.configurationUpdated} is run whenever a
   * relevant part of {@link ReconnectConfiguration} changes.
   *
   * @param connectionStateHandler - the connection state handler to bind to
   */
  static bind(connectionStateHandler: ConnectionStateHandler): void {
    Reactive.runWhenDependenciesChange(() => connectionStateHandler.configurationUpdated());
  }

  #getProperty(key: string): MapProperty {
    return this.#registry
      .getStateTree()
      .getRootNode()
      .getMap(NodeFeatures.RECONNECT_DIALOG_CONFIGURATION)
      .getProperty(key);
  }

  /**
   * Gets the text to show in the reconnect dialog.
   *
   * @returns the text to show in the reconnect dialog.
   *
   * @deprecated The API for configuring the connection indicator has changed.
   */
  getDialogText(): string | null {
    return (this.#getProperty(DIALOG_TEXT_KEY).getValue() as string | null) ?? null;
  }

  /**
   * Gets the text to show in the reconnect dialog when no longer trying to
   * reconnect.
   *
   * @returns the text to show in the reconnect dialog when no longer trying to
   *          reconnect
   *
   * @deprecated The API for configuring the connection indicator has changed.
   */
  getDialogTextGaveUp(): string | null {
    return (this.#getProperty(DIALOG_TEXT_GAVE_UP_KEY).getValue() as string | null) ?? null;
  }

  /**
   * Gets the text to show in the reconnect dialog.
   *
   * @returns the text to show in the reconnect dialog.
   */
  getReconnectAttempts(): number {
    return this.#getProperty(RECONNECT_ATTEMPTS_KEY).getValueOrDefault(RECONNECT_ATTEMPTS_DEFAULT);
  }

  /**
   * Gets the interval in milliseconds to wait between reconnect attempts.
   *
   * @returns the interval in milliseconds to wait between reconnect attempts
   */
  getReconnectInterval(): number {
    return this.#getProperty(RECONNECT_INTERVAL_KEY).getValueOrDefault(RECONNECT_INTERVAL_DEFAULT);
  }
}
