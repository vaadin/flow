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
 * Migrated `resendRequest` from
 * `com.vaadin.client.communication.XhrConnection`. The rest of XhrConnection
 * (server connection state, error handling, retry orchestration) stays Java.
 */
export const XhrConnection = {
  resendRequest(xhr: XMLHttpRequest): boolean {
    if (xhr.readyState !== 1) {
      // Progressed to some other readyState -> no longer blocked
      return false;
    }
    try {
      xhr.send();
      return true;
    } catch {
      // send throws if it is actually running for real
      return false;
    }
  }
};
