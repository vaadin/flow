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

import type { ApplicationConfiguration } from './ApplicationConfiguration';
import { VaadinUriResolver } from '../flow/shared/VaadinUriResolver';

/**
 * Returns the given uri as relative to the given base uri.
 *
 * @param baseURI - the base uri of the document
 * @param uri - an absolute uri to transform
 * @returns the uri as relative to the document base uri, or the given uri * unmodified
 *          if it is for different context.
 */
export function getBaseRelativeUri(baseURI: string, uri: string): string {
  if (uri.startsWith(baseURI)) {
    return uri.substring(baseURI.length);
  }
  return uri;
}

/**
 * Returns the current document location as relative to the base uri of the document.
 *
 * @returns the document current location as relative to the document base uri
 */
export function getCurrentLocationRelativeToBaseUri(): string {
  return getBaseRelativeUri(document.baseURI, window.location.href);
}

/** The slice of Registry that URIResolver uses. */
interface URIResolverRegistry {
  getApplicationConfiguration(): Pick<ApplicationConfiguration, 'getContextRootUrl'>;
}

/** Client side URL resolver for vaadin protocols. */
export class URIResolver extends VaadinUriResolver {
  readonly #registry: URIResolverRegistry;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: URIResolverRegistry) {
    super();
    this.#registry = registry;
  }

  /**
   * Translates a Vaadin URI to a URL that can be loaded by the browser. The
   * following URI schemes are supported:
   *
   * - `context://` - resolves to the application context root
   * - `base://` - resolves to the base URI of the page
   *
   * Any other URI protocols, such as `http://` or `https://` are passed through
   * this method unmodified.
   *
   * @param uri - the URI to resolve
   * @returns the resolved URI
   */
  override resolveVaadinUri(uri: string | null): string | null {
    return super.resolveVaadinUri(uri, this.getContextRootUrl());
  }

  protected getContextRootUrl(): string {
    return this.#registry.getApplicationConfiguration().getContextRootUrl();
  }
}
