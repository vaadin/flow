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

// TypeScript port of com.vaadin.client.communication.PollConfigurator, built
// alongside the Java version. It observes the poll-interval property in the
// node's POLL_CONFIGURATION feature and reconfigures the poller on change.

import type { StateNode } from '../flow/StateNode';
import type { Poller } from './Poller';
import { NodeFeatures } from '../../flow/internal/nodefeature/NodeFeatures';

// com.vaadin.flow.internal.nodefeature.PollConfigurationMap.POLL_INTERVAL_KEY
const POLL_INTERVAL_KEY = 'pollInterval';

// The poller is named by the method it is driven through rather than as a whole
// Poller, so a suite can observe the configured interval directly; the type is
// still derived from the ported class, so it cannot drift from it.
type ConfigurablePoller = Pick<Poller, 'setInterval'>;

/**
 * Observes the node's poll configuration and configures the poller on change.
 * Mirrors PollConfigurator.observe.
 */
export function observe(node: StateNode, poller: ConfigurablePoller): void {
  const pollIntervalProperty = node.getMap(NodeFeatures.POLL_CONFIGURATION).getProperty(POLL_INTERVAL_KEY);
  pollIntervalProperty.addChangeListener((event) => {
    poller.setInterval(Math.trunc(Number(event.getNewValue())));
  });
}
