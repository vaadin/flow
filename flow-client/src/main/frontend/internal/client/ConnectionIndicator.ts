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

interface ConnectionState {
  state: string;
  loadingStarted(): void;
  loadingFinished(): void;
  loadingFailed(): void;
}

interface VaadinGlobals {
  connectionState?: ConnectionState;
  connectionIndicator?: Record<string, unknown>;
}

function vaadin(): VaadinGlobals {
  return (globalThis as unknown as { Vaadin?: VaadinGlobals }).Vaadin ?? {};
}

/**
 * Bridge to the connection state + indicator globals set up by
 * `@vaadin/common-frontend`. Migrated from
 * `com.vaadin.client.ConnectionIndicator`.
 */
export const ConnectionIndicator = {
  setState(state: string): void {
    const cs = vaadin().connectionState;
    if (cs) {
      cs.state = state;
    }
  },

  getState(): string | null {
    return vaadin().connectionState?.state ?? null;
  },

  setProperty(property: string, value: unknown): void {
    const indicator = vaadin().connectionIndicator;
    if (indicator) {
      indicator[property] = value;
    }
  },

  loadingStarted(): void {
    vaadin().connectionState?.loadingStarted();
  },

  loadingFinished(): void {
    vaadin().connectionState?.loadingFinished();
  },

  loadingFailed(): void {
    vaadin().connectionState?.loadingFailed();
  }
};
