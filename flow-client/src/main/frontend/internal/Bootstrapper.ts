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
//
// populateApplicationConfiguration below is the build-alongside TS port of the
// Bootstrapper DOM-config reader: it fills an ApplicationConfiguration from the
// bootstrap JsoConfiguration. The EntryPoint sequence (onModuleLoad /
// startApplication / doStartApplication -> new ApplicationConnection -> start) is
// the cutover wiring.

import type { ApplicationConfiguration } from './ApplicationConfiguration';
import { getAbsoluteUrl } from './WidgetUtil';

// com.vaadin.flow.shared.ApplicationConstants
const SERVICE_URL = 'serviceUrl';
const APP_WC_MODE = 'webComponentMode';
const CONTEXT_ROOT_URL = 'contextRootUrl';
const UI_ID_PARAMETER = 'v-uiId';
const DEV_TOOLS_ENABLED = 'devToolsEnabled';

/** The bootstrap configuration object's typed accessors (a JS overlay in Java). */
export interface JsoConfiguration {
  getConfigString(name: string): string | null;
  getConfigBoolean(name: string): boolean;
  getConfigInteger(name: string): number;
  getConfigStringArray(name: string): string[];
  getConfigError(name: string): unknown;
  getVaadinVersion(): string;
  getAtmosphereVersion(): string;
  getAtmosphereJSVersion(): string;
}

/**
 * Fills the application configuration from the bootstrap JavaScript config.
 * Mirrors Bootstrapper.populateApplicationConfiguration.
 */
export function populateApplicationConfiguration(
  conf: ApplicationConfiguration,
  jsoConfiguration: JsoConfiguration
): void {
  // Resolve potentially relative URLs now so they survive later base-URL changes.
  const serviceUrl = jsoConfiguration.getConfigString(SERVICE_URL);

  conf.setWebComponentMode(jsoConfiguration.getConfigBoolean(APP_WC_MODE));

  if (serviceUrl === null) {
    conf.setServiceUrl(getAbsoluteUrl('.'));
    conf.setContextRootUrl(getAbsoluteUrl(jsoConfiguration.getConfigString(CONTEXT_ROOT_URL) ?? ''));
  } else {
    conf.setServiceUrl(serviceUrl);
    conf.setContextRootUrl(getAbsoluteUrl(serviceUrl + (jsoConfiguration.getConfigString(CONTEXT_ROOT_URL) ?? '')));
  }

  conf.setUIId(jsoConfiguration.getConfigInteger(UI_ID_PARAMETER));
  conf.setHeartbeatInterval(jsoConfiguration.getConfigInteger('heartbeatInterval'));
  conf.setMaxMessageSuspendTimeout(jsoConfiguration.getConfigInteger('maxMessageSuspendTimeout'));

  conf.setServletVersion(jsoConfiguration.getVaadinVersion());
  conf.setAtmosphereVersion(jsoConfiguration.getAtmosphereVersion());
  conf.setAtmosphereJSVersion(jsoConfiguration.getAtmosphereJSVersion());
  conf.setSessionExpiredError(jsoConfiguration.getConfigError('sessExpMsg'));

  // Debug or production mode?
  conf.setProductionMode(!jsoConfiguration.getConfigBoolean('debug'));
  conf.setRequestTiming(jsoConfiguration.getConfigBoolean('requestTiming'));
  conf.setExportedWebComponents(jsoConfiguration.getConfigStringArray('webcomponents'));

  conf.setDevToolsEnabled(jsoConfiguration.getConfigBoolean(DEV_TOOLS_ENABLED));
  conf.setLiveReloadUrl(jsoConfiguration.getConfigString('liveReloadUrl') ?? '');
  conf.setLiveReloadBackend(jsoConfiguration.getConfigString('liveReloadBackend') ?? '');
  conf.setSpringBootLiveReloadPort(jsoConfiguration.getConfigString('springBootLiveReloadPort') ?? '');
}

interface WebComponentsGlobal {
  WebComponents?: { ready?: boolean };
}

interface FlowWidgetsetRegistrar {
  Vaadin: { Flow: { registerWidgetset: (widgetsetName: string, callback: (applicationId: string) => void) => void } };
}

interface FlowAppLookup {
  Vaadin: { Flow: { getApp: (appId: string) => unknown } };
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

/**
 * The bootstrap configuration object for the application with the given id, as
 * stored by the bootstrap JavaScript.
 */
export function getJsoConfiguration(appId: string): unknown {
  return (window as unknown as FlowAppLookup).Vaadin.Flow.getApp(appId);
}
