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

import { NodeFeatures } from '../nodefeature/NodeFeatures';
import { Reactive } from '../reactive/reactive';

// com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap
const DIALOG_TEXT_KEY = 'dialogText';
const DIALOG_TEXT_GAVE_UP_KEY = 'dialogTextGaveUp';
const RECONNECT_ATTEMPTS_KEY = 'reconnectAttempts';
const RECONNECT_ATTEMPTS_DEFAULT = 10000;
const RECONNECT_INTERVAL_KEY = 'reconnectInterval';
const RECONNECT_INTERVAL_DEFAULT = 5000;

/** The slice of MapProperty ReconnectConfiguration reads. */
interface ReconnectProperty {
  getValue(): unknown;
  getValueOrDefault(defaultValue: number): number;
}

/** The slice of Registry ReconnectConfiguration reads. */
interface ReconnectRegistry {
  getStateTree(): { getRootNode(): { getMap(featureId: number): { getProperty(key: string): ReconnectProperty } } };
}

/** Notified when the reconnect configuration changes. */
interface ConfigurationListener {
  configurationUpdated(): void;
}

/** Exposes the reconnect configuration from the root node; mirrors ReconnectConfiguration.java. */
export class ReconnectConfiguration {
  private readonly registry: ReconnectRegistry;

  constructor(registry: ReconnectRegistry) {
    this.registry = registry;
  }

  /**
   * Re-runs the handler's configurationUpdated() whenever the reconnect
   * configuration changes. Mirrors ReconnectConfiguration.bind.
   */
  static bind(connectionStateHandler: ConfigurationListener): void {
    Reactive.runWhenDependenciesChange(() => connectionStateHandler.configurationUpdated());
  }

  private getProperty(key: string): ReconnectProperty {
    return this.registry
      .getStateTree()
      .getRootNode()
      .getMap(NodeFeatures.RECONNECT_DIALOG_CONFIGURATION)
      .getProperty(key);
  }

  /** The text to show in the reconnect dialog (deprecated configuration). */
  getDialogText(): string | null {
    return (this.getProperty(DIALOG_TEXT_KEY).getValue() as string | null) ?? null;
  }

  /** The text to show when no longer trying to reconnect (deprecated configuration). */
  getDialogTextGaveUp(): string | null {
    return (this.getProperty(DIALOG_TEXT_GAVE_UP_KEY).getValue() as string | null) ?? null;
  }

  /** The number of reconnect attempts before giving up. */
  getReconnectAttempts(): number {
    return this.getProperty(RECONNECT_ATTEMPTS_KEY).getValueOrDefault(RECONNECT_ATTEMPTS_DEFAULT);
  }

  /** The interval (ms) between reconnect attempts. */
  getReconnectInterval(): number {
    return this.getProperty(RECONNECT_INTERVAL_KEY).getValueOrDefault(RECONNECT_INTERVAL_DEFAULT);
  }
}
