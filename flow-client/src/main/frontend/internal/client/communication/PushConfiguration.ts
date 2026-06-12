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
import { Reactive } from '../flow/reactive/Reactive';

// Mirrors NodeFeatures.UI_PUSHCONFIGURATION / UI_PUSHCONFIGURATION_PARAMETERS.
const UI_PUSHCONFIGURATION = 5;
const UI_PUSHCONFIGURATION_PARAMETERS = 6;

// Mirrors PushConfigurationMap keys.
const PUSH_SERVLET_MAPPING_KEY = 'pushServletMapping';
const PUSHMODE_KEY = 'pushMode';
const ALWAYS_USE_XHR_TO_SERVER = 'alwaysXhrToServer';
const PARAMETERS_KEY = 'parameters';

type MapPropertyChange = { getOldValue(): unknown; getNewValue(): unknown };

type MapPropertyLike = {
  getValue(): unknown;
  hasPropertyValue(): boolean;
  addChangeListener(listener: (event: MapPropertyChange) => void): unknown;
};

type NodeMapLike = {
  getProperty(name: string): MapPropertyLike;
  hasPropertyValue(name: string): boolean;
  forEachProperty(callback: (property: MapPropertyLike, key: string) => void): void;
};

type StateNodeLike = { getMap(featureId: number): NodeMapLike };

type StateTreeLike = { getRootNode(): StateNodeLike };

/**
 * Push-configuration view of the root state node, plus the listener that
 * informs `MessageSender` when push is enabled/disabled. Migrated from
 * `com.vaadin.client.communication.PushConfiguration`.
 *
 * The two enable/disable callbacks are supplied at construction (each
 * dispatches into `MessageSender.setPushEnabled`) so the TS class does not
 * need to reach back through the Java Registry facade.
 */
export class PushConfiguration {
  private readonly tree: StateTreeLike;

  constructor(tree: StateTreeLike, enablePush: () => void, disablePush: () => void) {
    this.tree = tree;
    this.getConfigurationMap()
      .getProperty(PUSHMODE_KEY)
      .addChangeListener((event) => {
        const oldEnabled = PushConfiguration.isPushModeEnabled(event.getOldValue());
        const newEnabled = PushConfiguration.isPushModeEnabled(event.getNewValue());
        if (!oldEnabled && newEnabled) {
          // Defer until all parts of push configuration have updated.
          Reactive.addFlushListener(() => enablePush());
        } else if (oldEnabled && !newEnabled) {
          Reactive.addFlushListener(() => disablePush());
        }
      });
  }

  getPushServletMapping(): string | null {
    const map = this.getConfigurationMap();
    if (map.hasPropertyValue(PUSH_SERVLET_MAPPING_KEY)) {
      return map.getProperty(PUSH_SERVLET_MAPPING_KEY).getValue() as string;
    }
    return null;
  }

  isAlwaysXhrToServer(): boolean {
    // The only meaningful value is `true`, so presence is the signal.
    return this.getConfigurationMap().hasPropertyValue(ALWAYS_USE_XHR_TO_SERVER);
  }

  getParameters(): Map<string, string> {
    const property = this.getConfigurationMap().getProperty(PARAMETERS_KEY);
    const parametersNode = property.getValue() as StateNodeLike;
    const parametersMap = parametersNode.getMap(UI_PUSHCONFIGURATION_PARAMETERS);
    const out = new Map<string, string>();
    parametersMap.forEachProperty((p, key) => {
      out.set(key, p.getValue() as string);
    });
    return out;
  }

  isPushEnabled(): boolean {
    return PushConfiguration.isPushModeEnabled(this.getConfigurationMap().getProperty(PUSHMODE_KEY).getValue());
  }

  private getConfigurationMap(): NodeMapLike {
    return this.tree.getRootNode().getMap(UI_PUSHCONFIGURATION);
  }

  // Mirrors the private static isPushEnabled(Object) check. Avoids bringing
  // the PushMode enum to the client side.
  private static isPushModeEnabled(propertyValue: unknown): boolean {
    if (propertyValue == null) {
      return false;
    }
    return propertyValue !== 'DISABLED';
  }
}
