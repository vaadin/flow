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

// TypeScript port of com.vaadin.client.URIResolver (and the relevant part of its
// superclass com.vaadin.flow.shared.VaadinUriResolver). It resolves Vaadin URI
// protocols (context:// -> context root, base:// -> base URI) to
// browser-loadable URLs; other protocols pass through unchanged.

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
 * Resolves the context:// and base:// protocols in a Vaadin URI against the
 * given context root. Mirrors VaadinUriResolver.resolveVaadinUri.
 */
export function resolveVaadinUri(uri: string | null, servletToContextRoot: string): string | null {
  if (uri === null) {
    return null;
  }
  let processedUri = processProtocol(CONTEXT_PROTOCOL_PREFIX, servletToContextRoot, uri);
  processedUri = processProtocol(BASE_PROTOCOL_PREFIX, '', processedUri);
  return processedUri;
}

/**
 * Returns the given uri relative to the given base uri, or the uri unchanged if
 * it is for a different context. Mirrors URIResolver.getBaseRelativeUri.
 */
export function getBaseRelativeUri(baseURI: string, uri: string): string {
  if (uri.startsWith(baseURI)) {
    return uri.substring(baseURI.length);
  }
  return uri;
}

/** The current document location relative to the document base URI. */
export function getCurrentLocationRelativeToBaseUri(): string {
  return getBaseRelativeUri(document.baseURI, window.location.href);
}

/** The slice of Registry that URIResolver uses. */
interface URIResolverRegistry {
  getApplicationConfiguration(): { getContextRootUrl(): string };
}

/** Resolves Vaadin URIs against the application context root; mirrors URIResolver.java. */
export class URIResolver {
  private readonly registry: URIResolverRegistry;

  constructor(registry: URIResolverRegistry) {
    this.registry = registry;
  }

  /** Translates a Vaadin URI to a browser-loadable URL. */
  resolveVaadinUri(uri: string | null): string | null {
    return resolveVaadinUri(uri, this.getContextRootUrl());
  }

  protected getContextRootUrl(): string {
    return this.registry.getApplicationConfiguration().getContextRootUrl();
  }
}
