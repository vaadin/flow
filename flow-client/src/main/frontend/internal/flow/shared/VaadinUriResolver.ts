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

// TypeScript port of com.vaadin.flow.shared.VaadinUriResolver. URIResolver
// extends it, as in Java.

// com.vaadin.flow.shared.ApplicationConstants
const CONTEXT_PROTOCOL_PREFIX = 'context://';
const BASE_PROTOCOL_PREFIX = 'base://';

function processProtocol(protocol: string, replacement: string, vaadinUri: string): string {
  if (vaadinUri.startsWith(protocol)) {
    return replacement + vaadinUri.substring(protocol.length);
  }
  return vaadinUri;
}

/**
 * Utility for translating special Vaadin URIs into URLs usable by the browser.
 * This is an abstract class performing the main logic in
 * {@link VaadinUriResolver.resolveVaadinUri}.
 */
export abstract class VaadinUriResolver {
  /**
   * Translates a Vaadin URI to a URL that can be loaded by the browser. The
   * following URI schemes are supported:
   *
   * - `context://` resolves to the application context root
   * - `base://` - resolves to the base URI of the page
   *
   * Any other URI protocols, such as `http://` or `https://` are passed through
   * this method unmodified.
   *
   * @param uri - the URI to resolve
   * @param servletToContextRoot - the relative path from the servlet path (used
   *          as base path in the client) to the context root
   * @returns the resolved URI
   */
  protected resolveVaadinUri(uri: string | null, servletToContextRoot: string): string | null {
    if (uri === null) {
      return null;
    }

    let processedUri = processProtocol(CONTEXT_PROTOCOL_PREFIX, servletToContextRoot, uri);
    processedUri = processProtocol(BASE_PROTOCOL_PREFIX, '', processedUri);

    return processedUri;
  }
}
