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

// TypeScript port of com.vaadin.client.communication.PushConfiguration, built
// alongside the Java version. It exposes the push configuration stored in the
// root node's UI_PUSHCONFIGURATION feature and, when the push mode changes,
// informs the MessageSender to enable/disable push (deferred to a flush listener
// so all parts of the configuration are updated first).

import type { Registry } from '../Registry';
import type { StateNode } from '../flow/StateNode';
import type { NodeMap } from '../flow/nodefeature/NodeMap';
import { NodeFeatures } from '../../flow/internal/nodefeature/NodeFeatures';
import { Reactive } from '../flow/reactive/Reactive';

// com.vaadin.flow.internal.nodefeature.PushConfigurationMap
const PUSHMODE_KEY = 'pushMode';
const PUSH_SERVLET_MAPPING_KEY = 'pushServletMapping';
const ALWAYS_USE_XHR_TO_SERVER = 'alwaysXhrToServer';
const PARAMETERS_KEY = 'parameters';

// Whether a PUSHMODE value enables push (anything other than DISABLED).
function isPushModeEnabled(propertyValue: unknown): boolean {
  if (propertyValue === null || propertyValue === undefined) {
    return false;
  }
  // Intentionally avoiding bringing the PushMode enum to the client side.
  return propertyValue !== 'DISABLED';
}

/**
 * Provides the push configuration stored in the root node with an easier to use
 * API.
 *
 * Additionally tracks when push is enabled/disabled and informs {@link
 * MessageSender}.
 */
export class PushConfiguration {
  readonly #registry: Registry;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: Registry) {
    this.#registry = registry;
    this.#getConfigurationMap()
      .getProperty(PUSHMODE_KEY)
      .addChangeListener((event) => this.#onPushModeChange(event));
  }

  /**
   * Called whenever the push mode is changed.
   *
   * @param event - the value change event for push mode
   */
  #onPushModeChange(event: { getOldValue(): unknown; getNewValue(): unknown }): void {
    const oldModeEnabled = isPushModeEnabled(event.getOldValue());
    const newModeEnabled = isPushModeEnabled(event.getNewValue());

    if (!oldModeEnabled && newModeEnabled) {
      // Switch push on, once all parts of the configuration are updated.
      Reactive.addFlushListener(() => this.#registry.getMessageSender().setPushEnabled(true));
    } else if (oldModeEnabled && !newModeEnabled) {
      // Switch push off, once all parts of the configuration are updated.
      Reactive.addFlushListener(() => this.#registry.getMessageSender().setPushEnabled(false));
    }
  }

  #getConfigurationMap(): NodeMap {
    return this.#registry.getStateTree().getRootNode().getMap(NodeFeatures.UI_PUSHCONFIGURATION);
  }

  /**
   * Gets the push servlet mapping configured or determined on the server.
   *
   * @returns the push servlet mapping configured or determined on the server or
   *          null if none has been configured
   */
  getPushServletMapping(): string | null {
    const map = this.#getConfigurationMap();
    if (map.hasPropertyValue(PUSH_SERVLET_MAPPING_KEY)) {
      return map.getProperty(PUSH_SERVLET_MAPPING_KEY).getValue() as string;
    }
    return null;
  }

  /**
   * Checks if XHR should be used for client -\> server messages even though we are using
   * a bidirectional push transport such as websockets.
   *
   * @returns true if XHR should always be used, false otherwise
   */
  isAlwaysXhrToServer(): boolean {
    // The only possible value is "true".
    return this.#getConfigurationMap().hasPropertyValue(ALWAYS_USE_XHR_TO_SERVER);
  }

  /**
   * Gets all configured push parameters.
   *
   * The parameters configured on the server, including transports.
   *
   * @returns a map of all parameters configured on the server
   */
  getParameters(): Map<string, string> {
    const parametersNode = this.#getConfigurationMap().getProperty(PARAMETERS_KEY).getValue() as StateNode;
    const parametersMap = parametersNode.getMap(NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS);

    const parameters = new Map<string, string>();
    parametersMap.forEachProperty((property, key) => {
      parameters.set(key, property.getValue() as string);
    });
    return parameters;
  }

  /**
   * Checks if push is enabled.
   *
   * @returns true if push is enabled, false otherwise
   */
  isPushEnabled(): boolean {
    return isPushModeEnabled(this.#getConfigurationMap().getProperty(PUSHMODE_KEY).getValue());
  }
}
