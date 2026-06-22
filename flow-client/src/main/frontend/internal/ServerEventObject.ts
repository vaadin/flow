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

// The element.$server RPC-object helpers migrated from ServerEventObject.java,
// registered on window.Vaadin.Flow.internal.ServerEventObject by
// registerInternals; the Java methods delegate here, passing the $server object
// as the first argument. defineMethod stays in Java for now: its handler is set
// on the $server object wrapped in $entry, and the wrapping cannot be
// reproduced from TypeScript (it routes the handler's exceptions to GWT's
// uncaught-exception handler). Also bundled to ES5 for the HtmlUnit used by
// GwtTests.

// The $server object is an arbitrary-keyed JS object (server methods plus the
// promise-callback slot).
type ServerObject = Record<string, any>;

/**
 * Installs the non-enumerable promise-callback function on the $server object.
 * The server calls it (by the promiseCallbackName key) to settle a pending
 * promise created by a returnPromise method, identified by promiseId.
 */
export function initPromiseHandler(serverObject: ServerObject, promiseCallbackName: string): void {
  Object.defineProperty(serverObject, promiseCallbackName, {
    value: function (promiseId: number, success: boolean, value: unknown): void {
      const promise = serverObject[promiseCallbackName].promises[promiseId];

      // undefined if the client-side node was recreated after execution was scheduled
      if (promise !== undefined) {
        // eslint-disable-next-line @typescript-eslint/no-dynamic-delete -- frees the settled slot while preserving promise-id indexing
        delete serverObject[promiseCallbackName].promises[promiseId];

        if (success) {
          promise[0](value);
        } else {
          promise[1](Error('Something went wrong. Check server-side logs for more information.'));
        }
      }
    }
  });
  serverObject[promiseCallbackName].promises = [];
}

/** Removes a previously defined server method from the $server object. */
export function removeMethod(serverObject: ServerObject, methodName: string): void {
  // eslint-disable-next-line @typescript-eslint/no-dynamic-delete -- removing a dynamically named server method
  delete serverObject[methodName];
}

/** The names of the methods (and other own keys) defined on the $server object. */
export function getMethods(serverObject: ServerObject): string[] {
  return Object.keys(serverObject);
}

/**
 * Rejects all promises still pending on the $server object. Called during
 * client resynchronization to free consumers of promises the server will never
 * deliver.
 */
export function rejectPromises(serverObject: ServerObject, promiseCallbackName: string): void {
  const promises = serverObject[promiseCallbackName]?.promises;
  if (promises !== undefined) {
    promises.forEach(function (item: [unknown, (reason: unknown) => void]) {
      item[1](Error('Client is resynchronizing'));
    });
  }
}

/**
 * Reads the Polymer model value at the given path from the node and, when it is
 * a model object carrying a nodeId, returns a `{ nodeId }` wrapper; otherwise
 * null.
 */
export function getPolymerPropertyObject(node: unknown, propertyName: string): { nodeId: unknown } | null {
  const polymerNode = node as { get?: (path: string) => unknown };
  if (typeof polymerNode.get === 'function') {
    const polymerProperty = polymerNode.get(propertyName) as Record<string, unknown> | null;
    if (
      typeof polymerProperty === 'object' &&
      polymerProperty !== null &&
      typeof polymerProperty.nodeId !== 'undefined'
    ) {
      return { nodeId: polymerProperty.nodeId };
    }
  }
  return null;
}
