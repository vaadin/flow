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

const NODE_ID = 'nodeId';
const EVENT_PREFIX = 'event';
// Mirrors JsonConstants.RPC_PROMISE_CALLBACK_NAME.
const PROMISE_CALLBACK_NAME = '}p';
// Mirrors NodeFeatures.POLYMER_EVENT_LISTENERS.
const POLYMER_EVENT_LISTENERS = 16;

type StateNodeLike = {
  getDomNode(): unknown;
  getMap(featureId: number): {
    hasPropertyValue(name: string): boolean;
    getProperty(name: string): { getValue(): unknown };
  };
  getTree(): {
    getRegistry(): { getConstantPool(): { get(key: string): string[] } };
    sendTemplateEventToServer(node: StateNodeLike, methodName: string, args: unknown[], promiseId: number): void;
  };
};

// Cache of compiled expression functions; keyed by the raw expression text.
const expressionCache = new Map<string, (event: Event, server: ServerEventObject) => unknown>();

type PendingPromise = [resolve: (value: unknown) => void, reject: (reason: Error) => void];

// The PROMISE_CALLBACK_NAME-keyed slot is added per-instance by
// initPromiseHandler so the function and its .promises array can be reached
// from the dispatched server-event closures. We use a Record-typed alias to
// stay within eslint's consistent-indexed-object-style rule.
type ServerEventObjectShape = ServerEventObject & Record<string, unknown>;

/**
 * Per-element `$server` object that carries server-handler callbacks.
 * Migrated from `com.vaadin.client.flow.binding.ServerEventObject`.
 *
 * Instances are stored on the host DOM element under `$server`. Each
 * `defineMethod` installs a JS function on the instance under the handler
 * name; user code calls them as `element.$server.<name>(event)`. The
 * `@v-return` promise infrastructure stores resolve/reject pairs keyed by a
 * promise id under the PROMISE_CALLBACK_NAME slot, where the server-side
 * resolution callback can find them.
 */
export class ServerEventObject {
  static get(element: Element): ServerEventObject {
    const existing = ServerEventObject.getIfPresent(element);
    if (existing !== null) {
      return existing;
    }
    const server = Object.create(ServerEventObject.prototype) as ServerEventObjectShape;
    server.initPromiseHandler();
    (element as unknown as Record<string, unknown>).$server = server;
    return server;
  }

  static getIfPresent(node: Node): ServerEventObject | null {
    const server = (node as unknown as Record<string, unknown>).$server;
    return (server as ServerEventObject | undefined) ?? null;
  }

  // Used during construction in `get()` via Object.create + manual call.
  // eslint-disable-next-line @typescript-eslint/no-unused-private-class-members
  private initPromiseHandler(): void {
    const self = this as unknown as ServerEventObjectShape;
    // Stored under a JsonConstants-defined key so the server can locate it
    // when delivering @ClientCallable result/rejection messages.
    Object.defineProperty(self, PROMISE_CALLBACK_NAME, {
      value: function (promiseId: number, success: boolean, value: unknown): void {
        const callback = (self[PROMISE_CALLBACK_NAME] as unknown as { promises: PendingPromise[] }) ?? null;
        if (callback === null) {
          return;
        }
        const pending = callback.promises[promiseId];
        if (pending === undefined) {
          // Client-side node was recreated after the call was scheduled.
          return;
        }
        // eslint-disable-next-line @typescript-eslint/no-dynamic-delete
        delete callback.promises[promiseId];
        if (success) {
          pending[0](value);
        } else {
          pending[1](new Error('Something went wrong. Check server-side logs for more information.'));
        }
      }
    });
    (self[PROMISE_CALLBACK_NAME] as unknown as { promises: PendingPromise[] }).promises = [];
  }

  defineMethod(methodName: string, node: StateNodeLike, returnPromise: boolean): void {
    const self = this as unknown as ServerEventObjectShape;
    self[methodName] = function (this: ServerEventObjectShape, eventParameter: Event | undefined): unknown {
      const proto = Object.getPrototypeOf(this) as Record<string, unknown>;
      const protoMethod = proto[methodName];
      // eslint-disable-next-line prefer-rest-params
      const callArgs = arguments as unknown as unknown[];
      if (typeof protoMethod === 'function') {
        protoMethod.apply(this, callArgs);
      }
      const event = eventParameter ?? (globalThis as unknown as { event?: Event }).event;
      let args = getEventData(self, event as Event, methodName, node);
      if (args === null) {
        args = Array.prototype.slice.call(callArgs);
      }

      let returnValue: Promise<unknown> | undefined;
      let promiseId = -1;
      if (returnPromise) {
        const callback = self[PROMISE_CALLBACK_NAME] as unknown as { promises: PendingPromise[] };
        promiseId = callback.promises.length;
        returnValue = new Promise<unknown>((resolve, reject) => {
          callback.promises[promiseId] = [resolve, reject];
        });
      }
      node.getTree().sendTemplateEventToServer(node, methodName, args, promiseId);
      return returnValue;
    };
  }

  removeMethod(methodName: string): void {
    const self = this as unknown as ServerEventObjectShape;
    // eslint-disable-next-line @typescript-eslint/no-dynamic-delete
    delete self[methodName];
  }

  getMethods(): string[] {
    return Object.keys(this);
  }

  rejectPromises(): void {
    const callback = (this as unknown as ServerEventObjectShape)[PROMISE_CALLBACK_NAME] as
      | { promises?: PendingPromise[] }
      | undefined;
    if (callback?.promises !== undefined) {
      callback.promises.forEach((item) => item[1](new Error('Client is resynchronizing')));
    }
  }
}

function getEventData(
  server: ServerEventObject,
  event: Event,
  methodName: string,
  node: StateNodeLike
): unknown[] | null {
  if (!node.getMap(POLYMER_EVENT_LISTENERS).hasPropertyValue(methodName)) {
    return null;
  }
  const expressionConstantKey = node.getMap(POLYMER_EVENT_LISTENERS).getProperty(methodName).getValue() as string;
  const dataExpressions = node.getTree().getRegistry().getConstantPool().get(expressionConstantKey);
  const dataArray: unknown[] = [];
  for (let i = 0; i < dataExpressions.length; i++) {
    dataArray[i] = getExpressionValue(server, event, node, dataExpressions[i]);
  }
  return dataArray;
}

function getExpressionValue(server: ServerEventObject, event: Event, node: StateNodeLike, expression: string): unknown {
  if (serverExpectsNodeId(expression)) {
    return getPolymerPropertyObject(server, event, node, expression);
  }
  return getOrCreateExpression(expression)(event, server);
}

function serverExpectsNodeId(expression: string): boolean {
  return !expression.startsWith(EVENT_PREFIX) || expression === 'event.model.item';
}

function getPolymerPropertyObject(
  server: ServerEventObject,
  event: Event,
  node: StateNodeLike,
  expression: string
): unknown {
  if (expression.startsWith(EVENT_PREFIX)) {
    return createPolymerPropertyObject(server, event, expression);
  }
  return getPolymerPropertyFromDomNode(node.getDomNode() as { get?: (path: string) => unknown }, expression);
}

function createPolymerPropertyObject(server: ServerEventObject, event: Event, expression: string): unknown {
  const expressionValue = getOrCreateExpression(expression)(event, server) as { [NODE_ID]?: number };
  return { [NODE_ID]: expressionValue[NODE_ID] };
}

function getPolymerPropertyFromDomNode(node: { get?: (path: string) => unknown }, propertyName: string): unknown {
  if (typeof node.get === 'function') {
    const polymerProperty = node.get(propertyName) as Record<string, unknown> | null;
    if (polymerProperty !== null && typeof polymerProperty === 'object' && polymerProperty['nodeId'] !== undefined) {
      return { nodeId: polymerProperty['nodeId'] };
    }
  }
  return null;
}

function getOrCreateExpression(expression: string): (event: Event, server: ServerEventObject) => unknown {
  let fn = expressionCache.get(expression);
  if (fn === undefined) {
    // The expression text may reference `event` and `element`, matching the
    // (event, element) -> ... shape the server side declared.
    fn = new Function(EVENT_PREFIX, 'element', 'return (' + expression + ')') as (
      event: Event,
      server: ServerEventObject
    ) => unknown;
    expressionCache.set(expression, fn);
  }
  return fn;
}
