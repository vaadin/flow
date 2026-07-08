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

// TypeScript port of com.vaadin.client.communication.LoadingIndicatorConfigurator.
// It observes the loading-indicator delay / theme properties in the node's
// LOADING_INDICATOR_CONFIGURATION feature and applies them to the connection
// indicator singleton via ConnectionIndicator.

import { setProperty } from '../ConnectionIndicator';
import { NodeFeatures } from '../nodefeature/NodeFeatures';

// com.vaadin.flow.internal.nodefeature.LoadingIndicatorConfigurationMap
const FIRST_DELAY_KEY = 'first';
const SECOND_DELAY_KEY = 'second';
const THIRD_DELAY_KEY = 'third';
const DEFAULT_THEME_APPLIED_KEY = 'theme';
const FIRST_DELAY_DEFAULT = 450;
const SECOND_DELAY_DEFAULT = 1500;
const THIRD_DELAY_DEFAULT = 5000;
const DEFAULT_THEME_APPLIED_DEFAULT = true;

/** The source MapProperty of a change event, exposing getValueOrDefault. */
interface ConfigSource {
  getValueOrDefault(defaultValue: number): number;
  getValueOrDefault(defaultValue: boolean): boolean;
}

/** The slice of MapProperty LoadingIndicatorConfigurator observes. */
interface ConfigProperty {
  addChangeListener(listener: (event: { getSource(): ConfigSource }) => void): unknown;
}

/** The slice of NodeMap LoadingIndicatorConfigurator reads. */
interface ConfigMap {
  getProperty(name: string): ConfigProperty;
}

/** The slice of StateNode LoadingIndicatorConfigurator reads. */
interface LoadingIndicatorNode {
  getMap(featureId: number): ConfigMap;
}

function bindInteger(map: ConfigMap, key: string, setter: (delay: number) => void, defaultValue: number): void {
  map.getProperty(key).addChangeListener((event) => setter(event.getSource().getValueOrDefault(defaultValue)));
}

/**
 * Observes the node's loading-indicator configuration and applies it to the
 * connection indicator. Mirrors LoadingIndicatorConfigurator.observe.
 */
export function observe(node: LoadingIndicatorNode): void {
  const configMap = node.getMap(NodeFeatures.LOADING_INDICATOR_CONFIGURATION);

  bindInteger(configMap, FIRST_DELAY_KEY, (delay) => setProperty('firstDelay', delay), FIRST_DELAY_DEFAULT);
  bindInteger(configMap, SECOND_DELAY_KEY, (delay) => setProperty('secondDelay', delay), SECOND_DELAY_DEFAULT);
  bindInteger(configMap, THIRD_DELAY_KEY, (delay) => setProperty('thirdDelay', delay), THIRD_DELAY_DEFAULT);

  configMap
    .getProperty(DEFAULT_THEME_APPLIED_KEY)
    .addChangeListener((event) =>
      setProperty('applyDefaultTheme', event.getSource().getValueOrDefault(DEFAULT_THEME_APPLIED_DEFAULT))
    );
}
