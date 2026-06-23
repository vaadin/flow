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

// Bootstrap-sequence helpers migrated from Bootstrapper.java, registered on
// window.Vaadin.Flow.internal.Bootstrapper by registerInternals; the Java
// methods delegate here. The callbacks into the GWT entry point
// (doStartApplication / startApplication) are supplied from the Java side
// already wrapped in $entry. vaadinBootstrapLoaded stays in Java: it checks
// $wnd.Vaadin.Flow, which registerInternals itself creates, so a delegating
// version would always report true. Also bundled to ES5 for the HtmlUnit used
// by GwtTests.

interface WebComponentsGlobal {
  WebComponents?: { ready?: boolean };
}

interface FlowWidgetsetRegistrar {
  Vaadin: { Flow: { registerWidgetset: (widgetsetName: string, callback: (applicationId: string) => void) => void } };
}

/**
 * Whether the application can be started immediately, i.e. there is no
 * WebComponents polyfill still loading.
 */
export function startApplicationImmediately(): boolean {
  const webComponents = (window as unknown as WebComponentsGlobal).WebComponents;
  return !webComponents || webComponents.ready === true;
}

/**
 * Defers starting the application until the WebComponents polyfill signals it
 * is ready, by running the (already $entry-wrapped) callback on the
 * WebComponentsReady event.
 */
export function deferStartApplication(callback: () => void): void {
  window.addEventListener('WebComponentsReady', callback);
}

/**
 * Registers the widgetset start callback with the bootstrap JavaScript so it can
 * start applications once the widgetset is loaded.
 */
export function registerCallback(widgetsetName: string, callback: (applicationId: string) => void): void {
  (window as unknown as FlowWidgetsetRegistrar).Vaadin.Flow.registerWidgetset(widgetsetName, callback);
}
