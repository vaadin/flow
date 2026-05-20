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

const CONTEXT_PROTOCOL_PREFIX = 'context://';
const BASE_PROTOCOL_PREFIX = 'base://';

type RegistryLike = {
  getApplicationConfiguration(): { getContextRootUrl(): string };
};

/**
 * Client-side resolver for Vaadin URI protocols (`context://`, `base://`).
 * Migrated from `com.vaadin.client.URIResolver`.
 */
export class URIResolver {
  private readonly registry: RegistryLike;

  constructor(registry: RegistryLike) {
    this.registry = registry;
  }

  resolveVaadinUri(uri: string | null): string | null {
    if (uri == null) {
      return null;
    }
    let processed = URIResolver.processProtocol(CONTEXT_PROTOCOL_PREFIX, this.getContextRootUrl(), uri);
    processed = URIResolver.processProtocol(BASE_PROTOCOL_PREFIX, '', processed);
    return processed;
  }

  protected getContextRootUrl(): string {
    const root = this.registry.getApplicationConfiguration().getContextRootUrl();
    if (!root.endsWith('/')) {
      throw new Error('context root URL must end with /');
    }
    return root;
  }

  static getCurrentLocationRelativeToBaseUri(): string {
    return URIResolver.getBaseRelativeUri(document.baseURI, document.location.href);
  }

  static getBaseRelativeUri(baseURI: string, uri: string): string {
    if (uri.startsWith(baseURI)) {
      return uri.substring(baseURI.length);
    }
    return uri;
  }

  private static processProtocol(protocol: string, replacement: string, vaadinUri: string): string {
    if (vaadinUri.startsWith(protocol)) {
      return replacement + vaadinUri.substring(protocol.length);
    }
    return vaadinUri;
  }
}
