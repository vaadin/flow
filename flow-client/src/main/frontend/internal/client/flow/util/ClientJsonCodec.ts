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

type ReturnChannelArgs = (...args: unknown[]) => void;

/**
 * The single migrated JSNI helper from `com.vaadin.client.flow.util.ClientJsonCodec`.
 *
 * `createReturnChannelCallback` builds a JS function that forwards its
 * call arguments to a Java consumer. The Java shim packages
 * `(nodeId, channelId, ServerConnector::sendReturnChannelMessage)` into the
 * `send` callback so the TS side doesn't need to know about
 * `ServerConnector`.
 */
export const ClientJsonCodec = {
  createReturnChannelCallback(send: (args: unknown[]) => void): ReturnChannelArgs {
    return function (...args: unknown[]): void {
      send(args);
    };
  }
};
