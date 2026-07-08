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

/** Holds the bootstrap configuration of a UI; mirrors ApplicationConfiguration.java. */
export class ApplicationConfiguration {
  private applicationId = '';

  private contextRootUrl = '';

  private serviceUrl = '';

  private uiId = 0;

  private sessionExpiredError: unknown = null;

  private heartbeatInterval = 0;

  private maxMessageSuspendTimeout = 0;

  private productionMode = false;

  private requestTiming = false;

  private webComponentMode = false;

  private servletVersion = '';

  private atmosphereVersion = '';

  private atmosphereJSVersion = '';

  private exportedWebComponents: string[] = [];

  private devToolsEnabled = false;

  private liveReloadUrl = '';

  private liveReloadBackend = '';

  private springBootLiveReloadPort = '';

  getApplicationId(): string {
    return this.applicationId;
  }

  setApplicationId(applicationId: string): void {
    this.applicationId = applicationId;
  }

  getServiceUrl(): string {
    return this.serviceUrl;
  }

  setServiceUrl(serviceUrl: string): void {
    this.serviceUrl = serviceUrl;
  }

  getContextRootUrl(): string {
    return this.contextRootUrl;
  }

  setContextRootUrl(contextRootUrl: string): void {
    this.contextRootUrl = contextRootUrl;
  }

  isWebComponentMode(): boolean {
    return this.webComponentMode;
  }

  setWebComponentMode(mode: boolean): void {
    this.webComponentMode = mode;
  }

  getUIId(): number {
    return this.uiId;
  }

  setUIId(uiId: number): void {
    this.uiId = uiId;
  }

  getHeartbeatInterval(): number {
    return this.heartbeatInterval;
  }

  setHeartbeatInterval(heartbeatInterval: number): void {
    this.heartbeatInterval = heartbeatInterval;
  }

  getMaxMessageSuspendTimeout(): number {
    return this.maxMessageSuspendTimeout;
  }

  setMaxMessageSuspendTimeout(maxMessageSuspendTimeout: number): void {
    this.maxMessageSuspendTimeout = maxMessageSuspendTimeout;
  }

  getSessionExpiredError(): unknown {
    return this.sessionExpiredError;
  }

  setSessionExpiredError(sessionExpiredError: unknown): void {
    this.sessionExpiredError = sessionExpiredError;
  }

  getServletVersion(): string {
    return this.servletVersion;
  }

  setServletVersion(servletVersion: string): void {
    this.servletVersion = servletVersion;
  }

  getAtmosphereVersion(): string {
    return this.atmosphereVersion;
  }

  setAtmosphereVersion(atmosphereVersion: string): void {
    this.atmosphereVersion = atmosphereVersion;
  }

  getAtmosphereJSVersion(): string {
    return this.atmosphereJSVersion;
  }

  setAtmosphereJSVersion(atmosphereJSVersion: string): void {
    this.atmosphereJSVersion = atmosphereJSVersion;
  }

  isProductionMode(): boolean {
    return this.productionMode;
  }

  setProductionMode(productionMode: boolean): void {
    this.productionMode = productionMode;
  }

  isRequestTiming(): boolean {
    return this.requestTiming;
  }

  setRequestTiming(requestTiming: boolean): void {
    this.requestTiming = requestTiming;
  }

  getExportedWebComponents(): string[] {
    return this.exportedWebComponents;
  }

  setExportedWebComponents(exportedWebComponents: string[]): void {
    this.exportedWebComponents = exportedWebComponents;
  }

  isDevToolsEnabled(): boolean {
    return this.devToolsEnabled;
  }

  setDevToolsEnabled(devToolsEnabled: boolean): void {
    this.devToolsEnabled = devToolsEnabled;
  }

  getLiveReloadUrl(): string {
    return this.liveReloadUrl;
  }

  setLiveReloadUrl(liveReloadUrl: string): void {
    this.liveReloadUrl = liveReloadUrl;
  }

  getLiveReloadBackend(): string {
    return this.liveReloadBackend;
  }

  setLiveReloadBackend(liveReloadBackend: string): void {
    this.liveReloadBackend = liveReloadBackend;
  }

  getSpringBootLiveReloadPort(): string {
    return this.springBootLiveReloadPort;
  }

  setSpringBootLiveReloadPort(springBootLiveReloadPort: string): void {
    this.springBootLiveReloadPort = springBootLiveReloadPort;
  }
}
