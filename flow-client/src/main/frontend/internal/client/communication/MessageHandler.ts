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

interface AfterServerUpdateNode {
  afterServerUpdate?: () => void;
}

/**
 * Browser-touching helpers migrated from
 * `com.vaadin.client.communication.MessageHandler`. Reached from GWT code via
 * the `NativeMessageHandler` JsType shim. The protocol orchestration stays in
 * `MessageHandler.java`.
 */
export const MessageHandler = {
  removeStylesheetByIdFromDom(dependencyId: string): void {
    const selector = `link[data-id="${dependencyId}"], style[data-id="${dependencyId}"]`;
    for (const el of Array.from(document.querySelectorAll(selector))) {
      el.remove();
    }
  },

  callAfterServerUpdates(node: Node | null): void {
    const candidate = node as AfterServerUpdateNode | null;
    if (candidate && typeof candidate.afterServerUpdate === 'function') {
      candidate.afterServerUpdate();
    }
  },

  calculateBootstrapTime(): number {
    const timing = performance?.timing;
    if (timing) {
      return Date.now() - timing.responseStart;
    }
    return -1;
  },

  parseJSONResponse(jsonText: string): unknown {
    return JSON.parse(jsonText);
  },

  getFetchStartTime(): number {
    return performance?.timing?.fetchStart ?? 0;
  }
};
