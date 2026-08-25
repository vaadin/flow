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

// TypeScript port of com.vaadin.client.flow.binding.ServerEventObject -- the
// element.$server RPC object that sends event notifications to the server.
//
// It covers the $server lifecycle (get/getIfPresent), method definition
// (defineMethod), method management (initPromiseHandler/removeMethod/getMethods/
// rejectPromises), the node-based getPolymerPropertyObject, and the server
// event-data collection (getEventData and its expression evaluation), operating
// on the TypeScript StateNode/StateTree/ConstantPool.
//
// The Java defineMethod's $entry wrapper -- which routed handler exceptions to
// GWT's uncaught-exception handler -- has no equivalent: the handler is installed
// as a plain function and exceptions surface through the browser. The handler
// reads `this`/`arguments`, so it must stay a regular `function`, not an arrow.

import { JsonConstants } from '../../../flow/shared/JsonConstants';
import { NodeFeatures } from '../../../flow/internal/nodefeature/NodeFeatures';

// The $server object is an arbitrary-keyed JS object (server methods plus the
// promise-callback slot).
export type ServerObject = Record<string, any>;

// The (non-enumerable) key under which the promise-callback function is stored.
const PROMISE_CALLBACK_NAME = JsonConstants.RPC_PROMISE_CALLBACK_NAME;

// Expressions starting with this prefix are evaluated against the DOM event;
// other expressions describe a Polymer model property and resolve to a node id.
const EVENT_PREFIX = 'event';

const NODE_ID = 'nodeId';

/** The slice of MapProperty that getEventData reads. */
interface ServerEventProperty {
  getValue(): unknown;
}

/** The slice of NodeMap that getEventData reads. */
interface ServerEventMap {
  hasPropertyValue(name: string): boolean;
  getProperty(name: string): ServerEventProperty;
}

/** The slice of ConstantPool that getEventData reads. */
interface ServerEventConstantPool {
  get<T>(key: string): T;
}

/** The slice of Registry that exposes the constant pool. */
interface ServerEventRegistry {
  getConstantPool(): ServerEventConstantPool;
}

/** The slice of StateTree that defineMethod uses. */
interface ServerEventTree {
  getRegistry(): ServerEventRegistry;
  sendTemplateEventToServer(node: ServerEventNode, methodName: string, args: unknown[], promiseId: number): void;
}

/** The slice of StateNode that the $server methods use. */
export interface ServerEventNode {
  getTree(): ServerEventTree;
  getMap(featureId: number): ServerEventMap;
  getDomNode(): Node | null;
}

// An event-data expression parsed via `new Function`. The second parameter is
// named `element` (mirroring the Java contract) but receives the $server object.
type ServerEventDataExpression = (event: Event, element: ServerObject) => unknown;

const expressionCache = new Map<string, ServerEventDataExpression>();

/**
 * Installs the non-enumerable promise-callback function on the $server object.
 * The server calls it (by the promiseCallbackName key) to settle a pending
 * promise created by a returnPromise method, identified by promiseId.
 */
function initPromiseHandler(serverObject: ServerObject, promiseCallbackName: string): void {
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

/**
 * Removes a method with the given name.
 *
 * @param serverObject - the $server object the method is defined on
 * @param methodName - the name of the method to remove
 */
export function removeMethod(serverObject: ServerObject, methodName: string): void {
  // eslint-disable-next-line @typescript-eslint/no-dynamic-delete -- removing a dynamically named server method
  delete serverObject[methodName];
}

/**
 * Gets the defined methods.
 *
 * @param serverObject - the $server object the method is defined on
 * @returns an array of defined method names
 */
export function getMethods(serverObject: ServerObject): string[] {
  return Object.keys(serverObject);
}

/**
 * Reject all promises pending on this server object. Called during client
 * resynchronization to free consumers of promises that are never delivered by
 * the server.
 *
 * @param serverObject - the $server object the method is defined on
 * @param promiseCallbackName - the key under which the promise-callback
 *            function is stored
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
function getPolymerPropertyObject(node: unknown, propertyName: string): { nodeId: unknown } | null {
  const polymerNode = node as { get?: (path: string) => unknown };
  if (typeof polymerNode.get === 'function') {
    const polymerProperty = polymerNode.get(propertyName) as Record<string, unknown> | null;
    // Deviation from ServerEventObject.java: the extra `!== null` guard. Java
    // only checks `typeof(polymerProperty) === 'object'` and then dereferences
    // it, which would TypeError when node.get returns null (typeof null is
    // 'object'); guarding returns null instead.
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

/**
 * Gets or creates `element.$server` for the given element.
 *
 * @param element - the element to use
 * @returns a reference to the `$server` object in the element
 */
export function get(element: Element): ServerObject {
  let serverObject = getIfPresent(element);
  if (serverObject === null) {
    serverObject = {};
    initPromiseHandler(serverObject, PROMISE_CALLBACK_NAME);
    (element as any).$server = serverObject;
  }
  return serverObject;
}

/**
 * Gets or creates `element.$server` for the given element, if present.
 *
 * @param node - the element to use
 * @returns a reference to the `$server` object in the element, or `null` if
 *         note present.
 */
export function getIfPresent(node: Node): ServerObject | null {
  const serverObject = (node as any).$server;
  return serverObject === undefined ? null : serverObject;
}

/**
 * Returns a handle that rejects the pending promises of the node's $server
 * object, or null when the node has none. Used by StateTree.prepareForResync to
 * reject promises waiting on nodes that a resynchronization discards.
 */
export function getServerEventObjectForResync(node: Node): { rejectPromises(): void } | null {
  const serverObject = getIfPresent(node);
  if (serverObject === null) {
    return null;
  }
  return { rejectPromises: () => rejectPromises(serverObject, PROMISE_CALLBACK_NAME) };
}

/**
 * Defines a method with the given name to be a callback to the server for the
 * given state node.
 *
 * Note! If the Polymer.Element contains an implementation for `methodName` it
 * will be run before the server-side method.
 *
 * @param serverObject - the $server object the method is defined on
 * @param methodName - the name of the method to add
 * @param node - the node to use as an identifier when sending an event to the
 *            server
 * @param returnPromise - `true` if the handler should return a promise that
 *            will reflect the server-side result; `false` to not return any
 *            value
 */
export function defineMethod(
  serverObject: ServerObject,
  methodName: string,
  node: ServerEventNode,
  returnPromise: boolean
): void {
  serverObject[methodName] = function (this: ServerObject, eventParameter?: Event): unknown {
    // Run an existing prototype implementation (e.g. a Polymer element method)
    // before the server-side method.
    const prototype = Object.getPrototypeOf(this);
    if (prototype[methodName] !== undefined) {
      prototype[methodName].apply(this, arguments);
    }
    const event = eventParameter || (window as { event?: Event }).event;
    const tree = node.getTree();
    let args = getEventData(this, event as Event, methodName, node);
    if (args === null) {
      // No server-defined data: send all call arguments.
      args = Array.prototype.slice.call(arguments);
    }

    let returnValue: Promise<unknown> | undefined;
    let promiseId = -1;

    if (returnPromise) {
      const promises = this[PROMISE_CALLBACK_NAME].promises;
      promiseId = promises.length;
      returnValue = new Promise(function (resolve, reject) {
        // Store each callback for later use
        promises[promiseId] = [resolve, reject];
      });
    }

    tree.sendTemplateEventToServer(node, methodName, args, promiseId);

    return returnValue;
  };
}

/**
 * Collect extra data for element event if any has been sent from the server.
 * Note! Data is sent in the array in the same order as defined on the server
 * side.
 *
 * @param serverObject - the $server object the method is defined on
 * @param event - The fired Event
 * @param methodName - Method name that is called
 * @param node - Target node
 * @returns Array of extra event data
 */
function getEventData(
  serverObject: ServerObject,
  event: Event,
  methodName: string,
  node: ServerEventNode
): unknown[] | null {
  const listeners = node.getMap(NodeFeatures.POLYMER_EVENT_LISTENERS);
  if (listeners.hasPropertyValue(methodName)) {
    const dataArray: unknown[] = [];
    const constantPool = node.getTree().getRegistry().getConstantPool();
    const expressionConstantKey = listeners.getProperty(methodName).getValue() as string;

    const dataExpressions = constantPool.get<string[]>(expressionConstantKey);

    for (let i = 0; i < dataExpressions.length; i++) {
      dataArray[i] = getExpressionValue(serverObject, event, node, dataExpressions[i]);
    }
    return dataArray;
  }

  return null;
}

function getExpressionValue(
  serverObject: ServerObject,
  event: Event,
  node: ServerEventNode,
  expression: string
): unknown {
  if (serverExpectsNodeId(expression)) {
    return resolvePolymerPropertyObject(serverObject, event, node, expression);
  }

  return getOrCreateExpression(expression)(event, serverObject);
}

function serverExpectsNodeId(expression: string): boolean {
  return !expression.startsWith(EVENT_PREFIX) || expression === 'event.model.item';
}

// Mirrors the (event, node, expression) overload of getPolymerPropertyObject:
// an event-based expression is evaluated and wrapped as a node id, otherwise the
// model property is read from the DOM node.
function resolvePolymerPropertyObject(
  serverObject: ServerObject,
  event: Event,
  node: ServerEventNode,
  expression: string
): { nodeId: unknown } | null {
  if (expression.startsWith(EVENT_PREFIX)) {
    return createPolymerPropertyObject(serverObject, event, expression);
  }
  return getPolymerPropertyObject(node.getDomNode(), expression);
}

function createPolymerPropertyObject(
  serverObject: ServerObject,
  event: Event,
  expression: string
): { nodeId: unknown } {
  const expressionValue = getOrCreateExpression(expression)(event, serverObject) as Record<string, unknown>;
  // Java reads the value through JsonObject.getNumber(NODE_ID), coercing it to a
  // number before it is sent to the server.
  return { nodeId: Number(expressionValue[NODE_ID]) };
}

/**
 * Parses an event-data expression into a function `(event, element) => value`,
 * caching the result per expression string; mirrors getOrCreateExpression. The
 * `element` parameter receives the $server object, matching the Java contract.
 */
function getOrCreateExpression(expressionString: string): ServerEventDataExpression {
  let expression = expressionCache.get(expressionString);

  if (expression === undefined) {
    // Mirrors NativeFunction.create; the server controls these expressions.
    expression = new Function(EVENT_PREFIX, 'element', `return (${expressionString})`) as ServerEventDataExpression;
    expressionCache.set(expressionString, expression);
  }

  return expression;
}
