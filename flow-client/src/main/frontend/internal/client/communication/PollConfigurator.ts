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

// Mirrors com.vaadin.flow.internal.nodefeature.NodeFeatures.POLL_CONFIGURATION.
const POLL_CONFIGURATION = 8;

// Mirrors PollConfigurationMap.POLL_INTERVAL_KEY.
const POLL_INTERVAL_KEY = 'pollInterval';

type MapPropertyLike = {
  addChangeListener(listener: (event: { getNewValue(): unknown }) => void): unknown;
};

type NodeMapLike = {
  getProperty(name: string): MapPropertyLike;
};

type StateNodeLike = {
  getMap(featureId: number): NodeMapLike;
};

type PollerLike = {
  setInterval(interval: number): void;
};

/**
 * Observes the poll configuration on the given state node and pushes interval
 * changes into the supplied poller. Migrated from
 * `com.vaadin.client.communication.PollConfigurator`.
 */
export const PollConfigurator = {
  observe(node: StateNodeLike, poller: PollerLike): void {
    node
      .getMap(POLL_CONFIGURATION)
      .getProperty(POLL_INTERVAL_KEY)
      .addChangeListener((event) => {
        // Server stores interval as double; truncate to int to match the
        // original `(int) (double) e.getNewValue()` cast.
        const raw = event.getNewValue();
        const interval = typeof raw === 'number' ? Math.trunc(raw) : Number(raw);
        poller.setInterval(interval);
      });
  }
};
