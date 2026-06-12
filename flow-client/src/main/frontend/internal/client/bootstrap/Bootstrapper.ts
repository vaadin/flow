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

import { ApplicationConfiguration } from '../ApplicationConfiguration';
import { ApplicationConnection } from '../ApplicationConnection';
import { Console } from '../Console';
import { WidgetUtil } from '../WidgetUtil';
import { JsoConfiguration } from './JsoConfiguration';

// Mirrors ApplicationConstants. Strings inlined so the bootstrap module does
// not need a flow-shared bridge.
const SERVICE_URL = 'serviceUrl';
const CONTEXT_ROOT_URL = 'contextRootUrl';
const APP_WC_MODE = 'webComponentMode';
const UI_ID_PARAMETER = 'v-uiId';
const DEV_TOOLS_ENABLED = 'devToolsEnabled';

// Identifier used when registering the widgetset callback. The bootstrap
// JavaScript looks up widgetsets by this name.
const WIDGETSET_NAME = 'client';

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

const runningApplications: ApplicationConnection[] = [];

function getJsoConfiguration(appId: string): JsoConfiguration {
  const raw = vaadinFlow()?.getApp?.(appId);
  return new JsoConfiguration(raw);
}

function vaadinBootstrapLoaded(): boolean {
  return vaadinFlow() != null;
}

function startApplicationImmediately(): boolean {
  const wc = (globalThis as unknown as WebComponentsGlobal).WebComponents;
  return !wc || wc.ready === true;
}

function populateApplicationConfiguration(conf: ApplicationConfiguration, jso: JsoConfiguration): void {
  // Resolve potentially relative URLs to ensure they point to the desired
  // locations even if the base URL of the page changes later (e.g. with
  // pushState).
  const serviceUrl = jso.getConfigString(SERVICE_URL);

  conf.webComponentMode = jso.getConfigBoolean(APP_WC_MODE);

  if (serviceUrl == null) {
    conf.serviceUrl = WidgetUtil.getAbsoluteUrl('.');
    conf.contextRootUrl = WidgetUtil.getAbsoluteUrl(jso.getConfigString(CONTEXT_ROOT_URL) ?? '');
  } else {
    conf.serviceUrl = serviceUrl;
    conf.contextRootUrl = WidgetUtil.getAbsoluteUrl(`${serviceUrl}${jso.getConfigString(CONTEXT_ROOT_URL) ?? ''}`);
  }

  conf.uiId = jso.getConfigInteger(UI_ID_PARAMETER) ?? 0;
  conf.heartbeatInterval = jso.getConfigInteger('heartbeatInterval') ?? 0;
  conf.maxMessageSuspendTimeout = jso.getConfigInteger('maxMessageSuspendTimeout') ?? 0;

  conf.servletVersion = jso.getVaadinVersion();
  conf.atmosphereVersion = jso.getAtmosphereVersion();
  conf.atmosphereJSVersion = jso.getAtmosphereJSVersion();
  conf.sessionExpiredError = jso.getConfigError('sessExpMsg');

  // Debug or production mode?
  conf.productionMode = !jso.getConfigBoolean('debug');
  conf.requestTiming = jso.getConfigBoolean('requestTiming');
  conf.exportedWebComponents = jso.getConfigStringArray('webcomponents');

  conf.devToolsEnabled = jso.getConfigBoolean(DEV_TOOLS_ENABLED);
  conf.liveReloadUrl = jso.getConfigString('liveReloadUrl');
  conf.liveReloadBackend = jso.getConfigString('liveReloadBackend');
  conf.springBootLiveReloadPort = jso.getConfigString('springBootLiveReloadPort');
}

function getConfigFromDOM(appId: string): ApplicationConfiguration {
  const conf = new ApplicationConfiguration();
  conf.applicationId = appId;
  populateApplicationConfiguration(conf, getJsoConfiguration(appId));
  return conf;
}

function doStartApplication(applicationId: string): void {
  const appConf = getConfigFromDOM(applicationId);
  const applicationConnection = new ApplicationConnection(appConf);
  runningApplications.push(applicationConnection);

  const initialUidl = getJsoConfiguration(applicationId).getUIDL();
  applicationConnection.start(initialUidl);
}

function deferStartApplication(applicationId: string): void {
  const callback = (): void => doStartApplication(applicationId);
  globalThis.addEventListener('WebComponentsReady', callback);
}

/**
 * Starts the application with a given id by reading the configuration
 * options stored by the bootstrap javascript. Deferred to give other start
 * callbacks a chance to register first.
 */
function startApplication(applicationId: string): void {
  // Match the original `Scheduler.scheduleDeferred(...)` semantics; lets
  // synchronous callers finish setting up before construction kicks in.
  setTimeout(() => {
    if (startApplicationImmediately()) {
      doStartApplication(applicationId);
    } else {
      deferStartApplication(applicationId);
    }
  }, 0);
}

function registerCallback(widgetsetName: string): void {
  vaadinFlow()?.registerWidgetset?.(widgetsetName, startApplication);
}

/**
 * Bootstraps the Flow client engine. Migrated from
 * `com.vaadin.client.bootstrap.Bootstrapper`. `Flow.ts` imports this
 * lazily after the per-app `FlowBootstrap.init(response)` registers the
 * application under `window.Vaadin.Flow.getApp(appId)`.
 */
export const Bootstrapper = {
  initModule(): void {
    // Refuse to start without the bootstrap-side helpers (`getApp`,
    // `registerWidgetset`, ...) the application config depends on.
    if (!vaadinBootstrapLoaded()) {
      Console.warn('vaadinBootstrap.js was not loaded, skipping vaadin application configuration.');
      return;
    }

    registerCallback(WIDGETSET_NAME);
  }
};
