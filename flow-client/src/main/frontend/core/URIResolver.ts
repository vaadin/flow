/*
 * Copyright 2000-2025 Vaadin Ltd.
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
 * Application constants matching com.vaadin.flow.shared.ApplicationConstants
 */
const CONTEXT_PROTOCOL_PREFIX = 'context://';
const BASE_PROTOCOL_PREFIX = 'base://';

/**
 * Client side URL resolver for vaadin protocols.
 *
 * Translates Vaadin URIs to URLs that can be loaded by the browser.
 * Supports:
 * - context:// - resolves to the application context root
 * - base:// - resolves to the base URI of the page
 *
 * Migrated from com.vaadin.client.URIResolver (GWT)
 */
export class URIResolver {
  private registry: any; // Registry type to be defined later

  constructor(registry: any) {
    this.registry = registry;
  }

  /**
   * Translates a Vaadin URI to a URL that can be loaded by the browser.
   *
   * @param uri the URI to resolve
   * @return the resolved URI
   */
  resolveVaadinUri(uri: string | null): string | null {
    if (uri === null) {
      return null;
    }

    const contextRootUrl = this.getContextRootUrl();
    return URIResolver.resolveVaadinUriInternal(uri, contextRootUrl);
  }

  /**
   * Gets the context root URL from the registry.
   *
   * @return the context root URL ending with /
   */
  protected getContextRootUrl(): string {
    const root = this.registry.getApplicationConfiguration().getContextRootUrl();
    // Assert root ends with /
    if (!root.endsWith('/')) {
      throw new Error('Context root URL must end with /');
    }
    return root;
  }

  /**
   * Internal implementation of URI resolution.
   *
   * @param uri the URI to resolve
   * @param servletToContextRoot the relative path from servlet to context root
   * @return the resolved URI
   */
  private static resolveVaadinUriInternal(uri: string, servletToContextRoot: string): string {
    let processedUri = uri;

    // Process context:// protocol
    processedUri = URIResolver.processProtocol(CONTEXT_PROTOCOL_PREFIX, servletToContextRoot, processedUri);

    // Process base:// protocol
    processedUri = URIResolver.processProtocol(BASE_PROTOCOL_PREFIX, '', processedUri);

    return processedUri;
  }

  /**
   * Replaces the protocol prefix with the replacement string.
   *
   * @param protocol the protocol prefix to replace
   * @param replacement the replacement string
   * @param vaadinUri the URI to process
   * @return the processed URI
   */
  private static processProtocol(protocol: string, replacement: string, vaadinUri: string): string {
    if (vaadinUri.startsWith(protocol)) {
      return replacement + vaadinUri.substring(protocol.length);
    }
    return vaadinUri;
  }

  /**
   * Returns the current document location as relative to the base uri of the document.
   *
   * @return the document current location as relative to the document base uri
   */
  static getCurrentLocationRelativeToBaseUri(): string {
    const { baseURI } = document;
    const { href } = document.location;
    return URIResolver.getBaseRelativeUri(baseURI, href);
  }

  /**
   * Returns the given uri as relative to the given base uri.
   *
   * @param baseURI the base uri of the document
   * @param uri an absolute uri to transform
   * @return the uri as relative to the document base uri, or the given uri
   *         unmodified if it is for different context.
   */
  static getBaseRelativeUri(baseURI: string, uri: string): string {
    if (uri.startsWith(baseURI)) {
      return uri.substring(baseURI.length);
    }
    return uri;
  }
}

// Expose URIResolver to window.Vaadin.TypeScript so GWT can call it
declare global {
  interface Window {
    Vaadin?: {
      Flow?: any;
      connectionState?: any;
      TypeScript?: {
        URIResolver?: typeof URIResolver;
      };
    };
  }
}

if (typeof window !== 'undefined') {
  window.Vaadin = window.Vaadin || {};
  window.Vaadin.TypeScript = window.Vaadin.TypeScript || {};
  window.Vaadin.TypeScript.URIResolver = URIResolver;
}
