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

// Implementation migrated from XhrConnection.java, registered on
// window.Vaadin.Flow.internal.XhrConnection by registerInternals; the Java
// method delegates here. Also bundled to ES5 for the HtmlUnit used by GwtTests.

/**
 * Attempts to resend a request that is still in its initial (OPENED, readyState
 * 1) state. Returns true if the request was still blocked and got re-sent, or
 * false if it had already progressed or send() threw (it is running for real).
 */
export function resendRequest(xhr: XMLHttpRequest): boolean {
  if (xhr.readyState !== 1) {
    // Progressed to some other readyState -> no longer blocked
    return false;
  }
  try {
    xhr.send();
    return true;
  } catch {
    // send throws if it is running for real
    return false;
  }
}
