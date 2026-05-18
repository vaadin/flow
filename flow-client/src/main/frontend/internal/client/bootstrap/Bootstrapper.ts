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

interface FlowGlobals {
  getApp?: (id: string) => unknown;
  registerWidgetset?: (name: string, callback: (appId: string) => void) => void;
}

interface VaadinGlobals {
  Flow?: FlowGlobals;
}

interface WebComponentsGlobal {
  WebComponents?: { ready?: boolean };
}

function vaadinFlow(): FlowGlobals | undefined {
  return (globalThis as unknown as { Vaadin?: VaadinGlobals }).Vaadin?.Flow;
}

/**
 * Browser-touching helpers migrated from
 * `com.vaadin.client.bootstrap.Bootstrapper`. Reached from GWT code via the
 * `NativeBootstrapper` JsType shim. The application start orchestration
 * stays in `Bootstrapper.java`.
 */
export const Bootstrapper = {
  getJsoConfiguration(appId: string): unknown {
    return vaadinFlow()?.getApp?.(appId);
  },

  vaadinBootstrapLoaded(): boolean {
    return vaadinFlow() != null;
  },

  deferStartApplication(applicationId: string, doStart: (id: string) => void): void {
    const callback = () => doStart(applicationId);
    globalThis.addEventListener('WebComponentsReady', callback);
  },

  startApplicationImmediately(): boolean {
    const wc = (globalThis as unknown as WebComponentsGlobal).WebComponents;
    return !wc || wc.ready === true;
  },

  registerCallback(widgetsetName: string, startApplication: (appId: string) => void): void {
    vaadinFlow()?.registerWidgetset?.(widgetsetName, startApplication);
  }
};
