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

// Mirrors com.vaadin.flow.internal.nodefeature.NodeFeatures
// .RECONNECT_DIALOG_CONFIGURATION.
const RECONNECT_DIALOG_CONFIGURATION = 9;

// Mirrors ReconnectDialogConfigurationMap keys/defaults.
const DIALOG_TEXT_KEY = 'dialogText';
const DIALOG_TEXT_GAVE_UP_KEY = 'dialogTextGaveUp';
const RECONNECT_ATTEMPTS_KEY = 'reconnectAttempts';
const RECONNECT_ATTEMPTS_DEFAULT = 10000;
const RECONNECT_INTERVAL_KEY = 'reconnectInterval';
const RECONNECT_INTERVAL_DEFAULT = 5000;

type MapPropertyLike = {
  getValueOrDefaultString(defaultValue: string | null): string | null;
  getValueOrDefaultNumber(defaultValue: number): number;
};

type StateTreeLike = {
  getRootNode(): { getMap(featureId: number): { getProperty(name: string): MapPropertyLike } };
};

/**
 * Reactive view of the reconnect-dialog configuration on the root state
 * node. Migrated from `com.vaadin.client.communication.ReconnectConfiguration`.
 *
 * The static `bind(ConnectionStateHandler)` helper stays on the Java side as
 * an `@JsOverlay` because it wires `Reactive.runWhenDependenciesChange` — that
 * helper constructs a Java `Computation` subclass that can't be expressed
 * from TypeScript.
 */
export class ReconnectConfiguration {
  private readonly tree: StateTreeLike;

  constructor(tree: StateTreeLike) {
    this.tree = tree;
  }

  getDialogText(): string | null {
    return this.getProperty(DIALOG_TEXT_KEY).getValueOrDefaultString(null);
  }

  getDialogTextGaveUp(): string | null {
    return this.getProperty(DIALOG_TEXT_GAVE_UP_KEY).getValueOrDefaultString(null);
  }

  getReconnectAttempts(): number {
    return this.getProperty(RECONNECT_ATTEMPTS_KEY).getValueOrDefaultNumber(RECONNECT_ATTEMPTS_DEFAULT);
  }

  getReconnectInterval(): number {
    return this.getProperty(RECONNECT_INTERVAL_KEY).getValueOrDefaultNumber(RECONNECT_INTERVAL_DEFAULT);
  }

  private getProperty(key: string): MapPropertyLike {
    return this.tree.getRootNode().getMap(RECONNECT_DIALOG_CONFIGURATION).getProperty(key);
  }
}
