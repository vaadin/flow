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

// TypeScript port of com.vaadin.client.bootstrap.Bootstrapper: handles
// bootstrapping of the application. It reads the configuration provided by the
// server in the DOM and starts the client engine (ApplicationConnection), and
// acts as the entry point, which is the GWT module entry point in Java.
//
// Java is a class implementing EntryPoint, whose members are static apart from
// the onModuleLoad override the interface requires; the port is the module of
// those members, so onModuleLoad is an exported function.

import { ApplicationConfiguration, type ErrorMessage } from '../ApplicationConfiguration';
import {
  type ConfigObject,
  getAtmosphereJSVersion,
  getAtmosphereVersion,
  getConfigBoolean,
  getConfigError,
  getConfigInteger,
  getConfigString,
  getConfigStringArray,
  getUIDL,
  getVaadinVersion
} from './JsoConfiguration';
import { getScheduler } from '../TrackingScheduler';
import { getAbsoluteUrl } from '../WidgetUtil';
import { Console } from '../Console';
import { enter as profilerEnter, leave as profilerLeave } from '../Profiler';
import { assert } from '../../assert';
import type { ApplicationConnection } from '../ApplicationConnection';

// com.vaadin.flow.shared.ApplicationConstants
const SERVICE_URL = 'serviceUrl';
const APP_WC_MODE = 'webComponentMode';
const CONTEXT_ROOT_URL = 'contextRootUrl';
const UI_ID_PARAMETER = 'v-uiId';
const DEV_TOOLS_ENABLED = 'devToolsEnabled';

// Java calls the JsoConfiguration overlay's methods on the configuration object;
// the ported module takes that object as its first parameter instead, so every
// read below names the same Java method.
//
// Java unboxes the Integer these three return, so a configuration missing one is
// a bootstrap error rather than a defaulted value; assert instead of defaulting.
function getRequiredConfigInteger(config: ConfigObject, name: string): number {
  const value = getConfigInteger(config, name);
  assert(value !== null, `The bootstrap configuration has no ${name}`);
  return value as number;
}

/**
 * Reads the configuration values defined by the bootstrap JavaScript.
 *
 * @param conf - the configuration to fill in
 * @param jsoConfiguration - the bootstrap configuration object to read
 */
export function populateApplicationConfiguration(conf: ApplicationConfiguration, jsoConfiguration: ConfigObject): void {
  // The ported ApplicationConfiguration takes strings and a string array where
  // Java stores nullable ones, so the context root, the two version strings, the
  // exported web components and the three live-reload values below substitute an
  // empty value where Java stores the null. For the context root that null is
  // what Java concatenates into the URL as "null"; nothing depends on that, and
  // the server always writes these values.
  //
  // Resolve potentially relative URLs now so they survive later base-URL changes.
  const serviceUrl = getConfigString(jsoConfiguration, SERVICE_URL);

  conf.setWebComponentMode(getConfigBoolean(jsoConfiguration, APP_WC_MODE));

  if (serviceUrl === null) {
    conf.setServiceUrl(getAbsoluteUrl('.'));
    conf.setContextRootUrl(getAbsoluteUrl(getConfigString(jsoConfiguration, CONTEXT_ROOT_URL) ?? ''));
  } else {
    conf.setServiceUrl(serviceUrl);
    conf.setContextRootUrl(getAbsoluteUrl(serviceUrl + (getConfigString(jsoConfiguration, CONTEXT_ROOT_URL) ?? '')));
  }

  conf.setUIId(getRequiredConfigInteger(jsoConfiguration, UI_ID_PARAMETER));
  conf.setHeartbeatInterval(getRequiredConfigInteger(jsoConfiguration, 'heartbeatInterval'));
  conf.setMaxMessageSuspendTimeout(getRequiredConfigInteger(jsoConfiguration, 'maxMessageSuspendTimeout'));

  conf.setServletVersion(getVaadinVersion(jsoConfiguration) ?? '');
  conf.setAtmosphereVersion(getAtmosphereVersion(jsoConfiguration) ?? '');
  conf.setAtmosphereJSVersion(getAtmosphereJSVersion() ?? '');
  // JsoConfiguration returns the raw config value; the bootstrap contract is that
  // it carries the ErrorMessage fields (see getConfigError's doc).
  conf.setSessionExpiredError(getConfigError(jsoConfiguration, 'sessExpMsg') as ErrorMessage | null);

  // Debug or production mode?
  conf.setProductionMode(!getConfigBoolean(jsoConfiguration, 'debug'));
  conf.setRequestTiming(getConfigBoolean(jsoConfiguration, 'requestTiming'));
  conf.setExportedWebComponents((getConfigStringArray(jsoConfiguration, 'webcomponents') as string[] | null) ?? []);

  conf.setDevToolsEnabled(getConfigBoolean(jsoConfiguration, DEV_TOOLS_ENABLED));
  conf.setLiveReloadUrl(getConfigString(jsoConfiguration, 'liveReloadUrl') ?? '');
  conf.setLiveReloadBackend(getConfigString(jsoConfiguration, 'liveReloadBackend') ?? '');
  conf.setSpringBootLiveReloadPort(getConfigString(jsoConfiguration, 'springBootLiveReloadPort') ?? '');
}

/**
 * Constructs an ApplicationConfiguration object based on the information
 * available in the DOM.
 *
 * @param appId - the application id
 * @returns an application configuration object containing the read information
 */
function getConfigFromDOM(appId: string): ApplicationConfiguration {
  const conf = new ApplicationConfiguration();
  conf.setApplicationId(appId);
  populateApplicationConfiguration(conf, getJsoConfiguration(appId));
  return conf;
}

/**
 * Starts the application with the given id: reads its configuration from the DOM,
 * assembles the TypeScript engine through {@link ApplicationConnection.create},
 * and starts it from the initial UIDL. Mirrors Bootstrapper.doStartApplication.
 *
 * @param applicationId - id of the application to start
 */
export function doStartApplication(applicationId: string): void {
  profilerEnter('Bootstrapper.startApplication');
  const conf = getConfigFromDOM(applicationId);
  const initialUidl = getUIDL(getJsoConfiguration(applicationId));
  profilerLeave('Bootstrapper.startApplication');

  // Load the engine lazily: this keeps ApplicationConnection/DefaultRegistry and
  // the rest of the modern-JS engine out of the registerInternals bundle, which
  // the HtmlUnit-based GwtTests also load and cannot run (no Array.from, etc.).
  // The engine is only needed once a real application starts.
  //
  // Loading the engine is asynchronous, so an application started later can
  // reach start() before an earlier one does; the Java bootstrap started them in
  // call order. Nothing in the engine depends on that order today (each
  // application has its own registry), and the cutover, which no longer needs the
  // lazy split, restores it.
  void import('../ApplicationConnection')
    .then(({ ApplicationConnection }) => {
      ApplicationConnection.create(conf).start(initialUidl ?? null);
    })
    .catch((error: unknown) => {
      // The engine failed to load or to start, so there is no system error
      // handler to report through; log rather than leaving a silent rejection.
      Console.error(`Failed to start the Vaadin application ${applicationId}: ${String(error)}`);
    });
}

interface WebComponentsGlobal {
  WebComponents?: { ready?: boolean };
}

interface FlowWidgetsetRegistrar {
  Vaadin: { Flow: { registerWidgetset: (widgetsetName: string, callback: (applicationId: string) => void) => void } };
}

interface FlowAppLookup {
  Vaadin: { Flow: { getApp: (appId: string) => ConfigObject } };
}

/**
 * Whether the application can be started immediately, i.e. there is no
 * WebComponents polyfill still loading.
 *
 * @returns `true` if the application can be started now
 */
export function startApplicationImmediately(): boolean {
  const webComponents = (window as unknown as WebComponentsGlobal).WebComponents;
  return !webComponents || webComponents.ready === true;
}

/**
 * Defers starting the application until the WebComponents polyfill signals it is
 * ready, by starting it on the WebComponentsReady event. Java wraps the callback
 * in $entry; the port has no equivalent, as every listener is plain JavaScript.
 *
 * @param applicationId - id of the application to start
 */
export function deferStartApplication(applicationId: string): void {
  window.addEventListener('WebComponentsReady', () => doStartApplication(applicationId));
}

/**
 * Registers the callback that the bootstrap javascript uses to start
 * applications once the widgetset is loaded and all required information is
 * available.
 *
 * @param widgetsetName - the name of this widgetset
 */
export function registerCallback(widgetsetName: string): void {
  (window as unknown as FlowWidgetsetRegistrar).Vaadin.Flow.registerWidgetset(widgetsetName, startApplication);
}

// The client widgetset/module name (ClientEngine.gwt.xml rename-to="client").
const WIDGETSET_NAME = 'client';

/**
 * The bootstrap state, kept on window.Vaadin.Flow. The already-bootstrapped guard
 * lives here rather than in a module variable so it follows the bootstrap
 * context: the GWT client kept the equivalent Bootstrapper.moduleLoaded guard in
 * its engine, which was recreated whenever the engine re-ran, so the flag must
 * likewise reset when window.Vaadin.Flow is replaced (e.g. between tests).
 */
interface FlowBootstrapState {
  clientBootstrapped?: boolean;
}

function flowBootstrapState(): FlowBootstrapState | undefined {
  return (window as unknown as { Vaadin?: { Flow?: FlowBootstrapState } }).Vaadin?.Flow;
}

/**
 * Starts the application with a given id by reading the configuration options
 * stored by the bootstrap javascript. On the next deferred tick it starts
 * immediately, or defers until the WebComponents polyfill signals it is ready.
 *
 * @param applicationId - id of the application to load, this is also the id of
 *          the html element into which the application should be rendered
 */
export function startApplication(applicationId: string): void {
  getScheduler().scheduleDeferred(() => {
    if (startApplicationImmediately()) {
      doStartApplication(applicationId);
    } else {
      deferStartApplication(applicationId);
    }
  });
}

/**
 * The client bootstrap entry point: verifies the bootstrap JavaScript is present
 * and registers the widgetset start callback so the server bootstrap can start
 * applications once the widgetset is loaded. Runs at most once. Mirrors the GWT
 * Bootstrapper onModuleLoad / initModule entry.
 */
export function onModuleLoad(): void {
  const flow = flowBootstrapState();
  // Don't continue if vaadinBootstrap.js was not executed (window.Vaadin.Flow
  // absent).
  if (flow == null) {
    Console.warn('vaadinBootstrap.js was not loaded, skipping vaadin application configuration.');
    return;
  }
  // Don't run twice for the same bootstrap context. GWT initModule logs a
  // warning in the already-loaded case; we return silently because this guard is
  // keyed per bootstrap context (window.Vaadin.Flow.clientBootstrapped), where
  // benign re-entry is expected rather than a misconfiguration to warn about.
  if (flow.clientBootstrapped === true) {
    return;
  }
  flow.clientBootstrapped = true;

  // GWT initModule also calls Profiler.initialize() here. That is intentionally
  // omitted: the __gwtStatsEvent profiling logger is installed by the server
  // bootstrap JavaScript (BootstrapHandler.js), and the TypeScript profiler reads
  // performance timing directly, so it needs no relative-time supplier setup.
  registerCallback(WIDGETSET_NAME);
}

/**
 * Gets the configuration object for a specific application from the bootstrap
 * javascript.
 *
 * @param appId - the id of the application to get configuration data for
 * @returns a native javascript object containing the configuration data
 */
export function getJsoConfiguration(appId: string): ConfigObject {
  return (window as unknown as FlowAppLookup).Vaadin.Flow.getApp(appId);
}
