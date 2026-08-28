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

// TypeScript port of com.vaadin.client.ApplicationConfiguration, built alongside
// the Java version. A holder for the per-UI bootstrap configuration (service /
// context URLs, UI id, heartbeat / message-suspend timeouts, production mode,
// versions, dev-tools / live-reload settings). In Java it is a @JsType exported
// as window.Vaadin.Flow.internal.ApplicationConfiguration; the bootstrap
// populates it via the setters before the engine starts.

import { assert } from '../assert';
import { Console } from './Console';

/**
 * The slice of the bootstrap error message this configuration carries.
 *
 * TODO(flow-client-ts): replace with ErrorMessage once
 * com.vaadin.client.bootstrap.ErrorMessage is ported.
 */
export interface ErrorMessage {
  caption?: string;
  message?: string;
  url?: string;
}

/** Holds the bootstrap configuration of a UI; mirrors ApplicationConfiguration.java. */
export class ApplicationConfiguration {
  #applicationId = '';

  #contextRootUrl = '';

  #serviceUrl = '';

  #uiId = 0;

  #sessionExpiredError: ErrorMessage | null = null;

  #heartbeatInterval = 0;

  #maxMessageSuspendTimeout = 0;

  #productionMode = false;

  #requestTiming = false;

  #webComponentMode = false;

  #servletVersion = '';

  #atmosphereVersion = '';

  #atmosphereJSVersion = '';

  // Java leaves these null when absent from the bootstrap config; we default to
  // [] / '' to keep the published client API non-null (clientApi types
  // getExportedWebComponents() as string[]) and to avoid an NPE in consumers
  // that iterate exportedWebComponents (e.g. SystemErrorHandler).
  #exportedWebComponents: string[] = [];

  #devToolsEnabled = false;

  #liveReloadUrl = '';

  #liveReloadBackend = '';

  #springBootLiveReloadPort = '';

  /**
   * Gets the id generated for the application.
   *
   * @returns the id for the application
   */
  getApplicationId(): string {
    return this.#applicationId;
  }

  /**
   * Sets the id generated for the application.
   *
   * @param applicationId - the id for the application
   */
  setApplicationId(applicationId: string): void {
    this.#applicationId = applicationId;
  }

  /**
   * Gets the URL to the server-side VaadinService.
   *
   * @returns the URL to the server-side service as a string
   */
  getServiceUrl(): string {
    return this.#serviceUrl;
  }

  /**
   * Sets the URL to the server-side VaadinService.
   *
   * @param serviceUrl - the URL to the server-side service as a string
   */
  setServiceUrl(serviceUrl: string): void {
    this.#serviceUrl = serviceUrl;
  }

  /**
   * Gets the URL of the context root on the server.
   *
   * @returns the URL of the context root, ending with a "/"
   */
  getContextRootUrl(): string {
    return this.#contextRootUrl;
  }

  /**
   * Sets the URL of the context root on the server.
   *
   * @param contextRootUrl - the URL of the context root, ending with a "/"
   */
  setContextRootUrl(contextRootUrl: string): void {
    assert(contextRootUrl.endsWith('/'), 'The context root URL must end with a "/"');
    this.#contextRootUrl = contextRootUrl;
  }

  /**
   * Checks whether the application is running as a web-component in the page.
   *
   * @returns true in case the app is a WC
   */
  isWebComponentMode(): boolean {
    return this.#webComponentMode;
  }

  /**
   * Sets whether the application is running as a web-component in the page.
   *
   * @param mode - set to true if it's a WC
   */
  setWebComponentMode(mode: boolean): void {
    this.#webComponentMode = mode;
  }

  /**
   * Gets the UI id of the server-side UI associated with this client-side
   * instance. The UI id should be included in every request originating from
   * this instance in order to associate the request with the right UI
   * instance on the server.
   *
   * @returns the UI id
   */
  getUIId(): number {
    return this.#uiId;
  }

  /**
   * Sets the UI id of the server-side UI associated with this client-side
   * instance.
   *
   * @param uiId - the UI id
   */
  setUIId(uiId: number): void {
    this.#uiId = uiId;
  }

  /**
   * Gets the interval for heartbeat requests.
   *
   * @returns The interval in seconds between heartbeat requests, or -1 if heartbeat is disabled.
   */
  getHeartbeatInterval(): number {
    return this.#heartbeatInterval;
  }

  /**
   * Sets the interval for heartbeat requests.
   *
   * @param heartbeatInterval - The interval in seconds between heartbeat requests, or -1 if heartbeat is disabled.
   */
  setHeartbeatInterval(heartbeatInterval: number): void {
    this.#heartbeatInterval = heartbeatInterval;
  }

  /**
   * Gets the maximum message suspension delay.
   *
   * @returns The maximum time, in milliseconds, to suspend out-of-order messages waiting for their predecessor before resynchronizing.
   */
  getMaxMessageSuspendTimeout(): number {
    return this.#maxMessageSuspendTimeout;
  }

  /**
   * Sets the maximum message suspension delay.
   *
   * @param maxMessageSuspendTimeout - The maximum time, in milliseconds, to suspend out-of-order messages waiting for their predecessor before resynchronizing.
   */
  setMaxMessageSuspendTimeout(maxMessageSuspendTimeout: number): void {
    this.#maxMessageSuspendTimeout = maxMessageSuspendTimeout;
  }

  /**
   * Gets the message used when a session expiration error occurs.
   *
   * @returns the session expiration error message
   */
  getSessionExpiredError(): ErrorMessage | null {
    return this.#sessionExpiredError;
  }

  /**
   * Sets the message used when a session expiration error occurs.
   *
   * @param sessionExpiredError - the session expiration error message
   */
  setSessionExpiredError(sessionExpiredError: ErrorMessage | null): void {
    this.#sessionExpiredError = sessionExpiredError;
  }

  /**
   * Gets the Vaadin servlet version in use.
   *
   * @returns the Vaadin servlet version in use
   */
  getServletVersion(): string {
    return this.#servletVersion;
  }

  /**
   * Sets the Vaadin servlet version in use.
   *
   * @param servletVersion - the Vaadin servlet version in use
   */
  setServletVersion(servletVersion: string): void {
    this.#servletVersion = servletVersion;
  }

  /**
   * Gets the Atmosphere runtime version in use.
   *
   * @returns the Atmosphere runtime version in use
   */
  getAtmosphereVersion(): string {
    return this.#atmosphereVersion;
  }

  /**
   * Sets the Atmosphere runtime version in use.
   *
   * @param atmosphereVersion - the Atmosphere runtime version in use
   */
  setAtmosphereVersion(atmosphereVersion: string): void {
    this.#atmosphereVersion = atmosphereVersion;
  }

  /**
   * Gets the Atmosphere JavaScript version in use.
   *
   * @returns the Atmosphere JavaScript version in use
   */
  getAtmosphereJSVersion(): string {
    return this.#atmosphereJSVersion;
  }

  /**
   * Sets the Atmosphere JavaScript version in use.
   *
   * @param atmosphereJSVersion - the Atmosphere JavaScript version in use
   */
  setAtmosphereJSVersion(atmosphereJSVersion: string): void {
    this.#atmosphereJSVersion = atmosphereJSVersion;
  }

  /**
   * Checks if we are running in production mode.
   *
   * With production mode disabled, a lot more information is logged to the
   * browser console. In production you should always enable production mode,
   * because logging and other debug features can have a significant
   * performance impact.
   *
   * @returns `true` if production mode is enabled, `false` otherwise
   */
  isProductionMode(): boolean {
    return this.#productionMode;
  }

  /**
   * Checks if request timing info should be made available.
   *
   * @returns `true` if request timing info should be made availble, `false` otherwise
   */
  isRequestTiming(): boolean {
    return this.#requestTiming;
  }

  /**
   * Sets whether we are running in production mode.
   *
   * With production mode disabled, a lot more information is logged to the
   * browser console. In production you should always enable production mode,
   * because logging and other debug features can have a significant
   * performance impact.
   *
   * @param productionMode - `true` if production mode is enabled, `false` otherwise
   */
  setProductionMode(productionMode: boolean): void {
    this.#productionMode = productionMode;
    Console.setProductionMode(productionMode);
  }

  /**
   * Sets whether request timing info should be made available.
   *
   * @param requestTiming - `true` if request timing info should be made available, `false` otherwise
   */
  setRequestTiming(requestTiming: boolean): void {
    this.#requestTiming = requestTiming;
  }

  /**
   * Sets the exported web components.
   *
   * @param exportedWebComponents - the exported web components
   */
  setExportedWebComponents(exportedWebComponents: string[]): void {
    this.#exportedWebComponents = exportedWebComponents;
  }

  /**
   * Gets the exported web components.
   *
   * @returns the exported web components
   */
  getExportedWebComponents(): string[] {
    return this.#exportedWebComponents;
  }

  /**
   * Gets if development tools should be added to the page.
   *
   * @returns whether development tools should be added
   */
  isDevToolsEnabled(): boolean {
    return this.#devToolsEnabled;
  }

  /**
   *
   * Sets if development tools should be added to the page.
   *
   * @param devToolsEnabled - whether development tools should be added
   */
  setDevToolsEnabled(devToolsEnabled: boolean): void {
    this.#devToolsEnabled = devToolsEnabled;
  }

  /**
   * Gets the URL for the live reload websocket connection.
   *
   * @returns URL for the live reload websocket connection
   */
  getLiveReloadUrl(): string {
    return this.#liveReloadUrl;
  }

  /**
   * Sets the URL for the live reload websocket connection.
   *
   * @param liveReloadUrl - URL for the live reload websocket connection
   */
  setLiveReloadUrl(liveReloadUrl: string): void {
    this.#liveReloadUrl = liveReloadUrl;
  }

  /**
   * Gets the the live reload backend technology identifier.
   *
   * @returns the live reload backend technology identifier
   */
  getLiveReloadBackend(): string {
    return this.#liveReloadBackend;
  }

  /**
   * Sets the live reload backend technology identifier.
   *
   * @param liveReloadBackend - the live reload backend technology identifier
   */
  setLiveReloadBackend(liveReloadBackend: string): void {
    this.#liveReloadBackend = liveReloadBackend;
  }

  /**
   * Gets the Spring boot live reload port.
   *
   * @returns the Spring boot live reload port
   */
  getSpringBootLiveReloadPort(): string {
    return this.#springBootLiveReloadPort;
  }

  /**
   * Sets the Spring boot live reload port.
   *
   * @param springBootLiveReloadPort - the Spring boot live reload port
   */
  setSpringBootLiveReloadPort(springBootLiveReloadPort: string): void {
    this.#springBootLiveReloadPort = springBootLiveReloadPort;
  }
}
