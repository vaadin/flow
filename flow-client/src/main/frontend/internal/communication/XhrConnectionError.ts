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

// TypeScript port of com.vaadin.client.communication.XhrConnectionError, built
// alongside the Java version. A data holder describing an error during an XHR
// request to the server.

/** Detail about an error during an XHR request to the server; mirrors XhrConnectionError.java. */
export class XhrConnectionError {
  private readonly xhr: XMLHttpRequest;

  private readonly payload: Record<string, unknown>;

  private readonly error: Error | null;

  constructor(xhr: XMLHttpRequest, payload: Record<string, unknown>, error: Error | null) {
    this.xhr = xhr;
    this.payload = payload;
    this.error = error;
  }

  /** The error that caused the problem, or null if not available. */
  getException(): Error | null {
    return this.error;
  }

  /** The request that failed to reach the server. */
  getXhr(): XMLHttpRequest {
    return this.xhr;
  }

  /** The payload that was sent to the server, never null. */
  getPayload(): Record<string, unknown> {
    return this.payload;
  }
}
