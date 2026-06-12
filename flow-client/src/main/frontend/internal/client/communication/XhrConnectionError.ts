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
 * Detail about an error that occurred during an XHR request to the server.
 * Migrated from `com.vaadin.client.communication.XhrConnectionError`. Pure
 * data carrier; the constructor parameters are passed straight through to
 * fields and exposed via getter methods that mirror the Java API.
 */
export class XhrConnectionError {
  private readonly xhr: unknown;
  private readonly payload: unknown;
  private readonly exception: unknown;

  constructor(xhr: unknown, payload: unknown, exception: unknown) {
    this.xhr = xhr;
    this.payload = payload;
    this.exception = exception;
  }

  getException(): unknown {
    return this.exception;
  }

  getXhr(): unknown {
    return this.xhr;
  }

  getPayload(): unknown {
    return this.payload;
  }
}
