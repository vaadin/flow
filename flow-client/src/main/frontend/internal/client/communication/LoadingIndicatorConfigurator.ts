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
import { ConnectionIndicator } from '../ConnectionIndicator';

// Mirrors com.vaadin.flow.internal.nodefeature.NodeFeatures
// .LOADING_INDICATOR_CONFIGURATION.
const LOADING_INDICATOR_CONFIGURATION = 10;

// Mirrors LoadingIndicatorConfigurationMap keys/defaults.
const FIRST_DELAY_KEY = 'first';
const FIRST_DELAY_DEFAULT = 450;
const SECOND_DELAY_KEY = 'second';
const SECOND_DELAY_DEFAULT = 1500;
const THIRD_DELAY_KEY = 'third';
const THIRD_DELAY_DEFAULT = 5000;
const DEFAULT_THEME_APPLIED_KEY = 'theme';
const DEFAULT_THEME_APPLIED_DEFAULT = true;

type MapPropertyLike = {
  addChangeListener(listener: (event: { getSource(): MapPropertyLike }) => void): unknown;
  getValueOrDefaultNumber(defaultValue: number): number;
  getValueOrDefaultBoolean(defaultValue: boolean): boolean;
};

type NodeMapLike = {
  getProperty(name: string): MapPropertyLike;
};

type StateNodeLike = {
  getMap(featureId: number): NodeMapLike;
};

function bindInteger(map: NodeMapLike, key: string, setter: (value: number) => void, defaultValue: number): void {
  map.getProperty(key).addChangeListener((event) => setter(event.getSource().getValueOrDefaultNumber(defaultValue)));
}

function bindBoolean(map: NodeMapLike, key: string, setter: (value: boolean) => void, defaultValue: boolean): void {
  map.getProperty(key).addChangeListener((event) => setter(event.getSource().getValueOrDefaultBoolean(defaultValue)));
}

/**
 * Observes the loading-indicator configuration on the root state node and
 * pushes changes into the connection indicator custom element. Migrated from
 * `com.vaadin.client.communication.LoadingIndicatorConfigurator`.
 */
export const LoadingIndicatorConfigurator = {
  observe(node: StateNodeLike): void {
    const configMap = node.getMap(LOADING_INDICATOR_CONFIGURATION);
    bindInteger(
      configMap,
      FIRST_DELAY_KEY,
      (v) => ConnectionIndicator.setProperty('firstDelay', v),
      FIRST_DELAY_DEFAULT
    );
    bindInteger(
      configMap,
      SECOND_DELAY_KEY,
      (v) => ConnectionIndicator.setProperty('secondDelay', v),
      SECOND_DELAY_DEFAULT
    );
    bindInteger(
      configMap,
      THIRD_DELAY_KEY,
      (v) => ConnectionIndicator.setProperty('thirdDelay', v),
      THIRD_DELAY_DEFAULT
    );
    bindBoolean(
      configMap,
      DEFAULT_THEME_APPLIED_KEY,
      (v) => ConnectionIndicator.setProperty('applyDefaultTheme', v),
      DEFAULT_THEME_APPLIED_DEFAULT
    );
  }
};
