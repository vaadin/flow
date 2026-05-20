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

/**
 * Application configuration data. Migrated from
 * `com.vaadin.client.ApplicationConfiguration`. Effectively immutable; setters
 * exist only for construction-time assignment by `Bootstrapper`.
 */
export class ApplicationConfiguration {
  applicationId: string | null = null;
  contextRootUrl: string | null = null;
  serviceUrl: string | null = null;
  uiId = 0;
  sessionExpiredError: unknown = null;
  heartbeatInterval = 0;
  maxMessageSuspendTimeout = 0;

  productionMode = false;
  requestTiming = false;
  webComponentMode = false;

  servletVersion: string | null = null;
  atmosphereVersion: string | null = null;
  atmosphereJSVersion: string | null = null;
  exportedWebComponents: string[] | null = null;

  devToolsEnabled = false;
  liveReloadUrl: string | null = null;
  liveReloadBackend: string | null = null;
  springBootLiveReloadPort: string | null = null;
}
