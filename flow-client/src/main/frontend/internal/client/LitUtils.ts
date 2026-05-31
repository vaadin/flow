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

interface MaybeLitElement {
  update?: unknown;
  updateComplete?: unknown;
  shouldUpdate?: unknown;
  firstUpdated?: unknown;
}

/**
 * Helpers for interacting with Lit-based custom elements from the binding code.
 *
 * Migrated from `com.vaadin.client.LitUtils`.
 */
export const LitUtils = {
  isLitElement(element: unknown): boolean {
    const candidate = element as MaybeLitElement | null;
    return (
      candidate != null &&
      typeof candidate.update === 'function' &&
      candidate.updateComplete instanceof Promise &&
      typeof candidate.shouldUpdate === 'function' &&
      typeof candidate.firstUpdated === 'function'
    );
  },

  whenRendered(element: Element, runnable: () => void): void {
    const ready = (element as unknown as { updateComplete?: Promise<unknown> }).updateComplete;
    if (ready) {
      void ready.then(() => runnable());
    }
  }
};
