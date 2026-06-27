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
// so all parts of the configuration are updated first). The Registry/StateTree/
// MessageSender are contracts satisfied at cutover.

import { NodeFeatures } from '../nodefeature/NodeFeatures';
import { Reactive } from '../reactive/reactive';

// com.vaadin.flow.internal.nodefeature.PushConfigurationMap
const PUSHMODE_KEY = 'pushMode';
const PUSH_SERVLET_MAPPING_KEY = 'pushServletMapping';
const ALWAYS_USE_XHR_TO_SERVER = 'alwaysXhrToServer';
const PARAMETERS_KEY = 'parameters';

/** The slice of MapProperty PushConfiguration reads. */
interface PushMapProperty {
  getValue(): unknown;
  addChangeListener(listener: (event: { getOldValue(): unknown; getNewValue(): unknown }) => void): unknown;
}

/** The slice of NodeMap PushConfiguration reads. */
interface PushNodeMap {
  getProperty(key: string): PushMapProperty;
  hasPropertyValue(key: string): boolean;
  forEachProperty(callback: (property: PushMapProperty, key: string) => void): void;
}

/** The slice of StateNode PushConfiguration reads. */
interface PushStateNode {
  getMap(featureId: number): PushNodeMap;
}

/** The slice of Registry PushConfiguration uses. */
interface PushConfigRegistry {
  getStateTree(): { getRootNode(): PushStateNode };
  getMessageSender(): { setPushEnabled(enabled: boolean): void };
}

// Whether a PUSHMODE value enables push (anything other than DISABLED).
function isPushModeEnabled(propertyValue: unknown): boolean {
  if (propertyValue === null || propertyValue === undefined) {
    return false;
  }
  // Intentionally avoiding bringing the PushMode enum to the client side.
  return propertyValue !== 'DISABLED';
}

/** Exposes the push configuration and drives MessageSender; mirrors PushConfiguration.java. */
export class PushConfiguration {
  private readonly registry: PushConfigRegistry;

  constructor(registry: PushConfigRegistry) {
    this.registry = registry;
    this.getConfigurationMap()
      .getProperty(PUSHMODE_KEY)
      .addChangeListener((event) => this.onPushModeChange(event));
  }

  private onPushModeChange(event: { getOldValue(): unknown; getNewValue(): unknown }): void {
    const oldModeEnabled = isPushModeEnabled(event.getOldValue());
    const newModeEnabled = isPushModeEnabled(event.getNewValue());

    if (!oldModeEnabled && newModeEnabled) {
      // Switch push on, once all parts of the configuration are updated.
      Reactive.addFlushListener(() => this.registry.getMessageSender().setPushEnabled(true));
    } else if (oldModeEnabled && !newModeEnabled) {
      // Switch push off, once all parts of the configuration are updated.
      Reactive.addFlushListener(() => this.registry.getMessageSender().setPushEnabled(false));
    }
  }

  private getConfigurationMap(): PushNodeMap {
    return this.registry.getStateTree().getRootNode().getMap(NodeFeatures.UI_PUSHCONFIGURATION);
  }

  /** The push servlet mapping configured on the server, or null if none. */
  getPushServletMapping(): string | null {
    const map = this.getConfigurationMap();
    if (map.hasPropertyValue(PUSH_SERVLET_MAPPING_KEY)) {
      return map.getProperty(PUSH_SERVLET_MAPPING_KEY).getValue() as string;
    }
    return null;
  }

  /** Whether XHR should always be used for client→server messages even with bidirectional push. */
  isAlwaysXhrToServer(): boolean {
    // The only possible value is "true".
    return this.getConfigurationMap().hasPropertyValue(ALWAYS_USE_XHR_TO_SERVER);
  }

  /** All push parameters configured on the server (including transports). */
  getParameters(): Map<string, string> {
    const parametersNode = this.getConfigurationMap().getProperty(PARAMETERS_KEY).getValue() as PushStateNode;
    const parametersMap = parametersNode.getMap(NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS);

    const parameters = new Map<string, string>();
    parametersMap.forEachProperty((property, key) => {
      parameters.set(key, property.getValue() as string);
    });
    return parameters;
  }

  /** Whether push is enabled. */
  isPushEnabled(): boolean {
    return isPushModeEnabled(this.getConfigurationMap().getProperty(PUSHMODE_KEY).getValue());
  }
}
