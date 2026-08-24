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

// Implementations migrated from ClientJsonCodec.java.

import type { StateNode } from '../StateNode';
import type { StateTree } from '../StateTree';

type JsFunction = (...args: unknown[]) => unknown;

/**
 * Creates the function exposed for a server return channel. Calling it forwards
 * all of its arguments (as an array) to the supplied sender, which the Java
 * caller wires (exception-guarded) to ServerConnector.sendReturnChannelMessage.
 *
 * Private in the Java source (`private static native`); kept module-local and
 * exercised through the public `decodeWithTypeInfo` (`@v-return`) surface.
 */
function createReturnChannelCallback(sendMessage: (args: unknown[]) => void): JsFunction {
  return (...args: unknown[]) => {
    sendMessage(args);
  };
}

/**
 * Wraps `fn` in a function that prepends `captures` to the runtime arguments
 * before delegating, while leaving `this` controlled by the caller (unlike
 * Function.prototype.bind, which would pre-bind `this`).
 *
 * Private in the Java source (`private static native`); kept module-local and
 * exercised through the public `decodeWithTypeInfo` (`@v-fn`) surface.
 */
function applyCaptures(fn: JsFunction, captures: ArrayLike<unknown>): JsFunction {
  return function (this: unknown, ...callArgs: unknown[]): unknown {
    const args = Array.from(captures).concat(callArgs);
    return fn.apply(this, args);
  };
}

/**
 * Encodes a value for transport without type information. In JavaScript the JSON
 * representation is used as-is; only `undefined`/`null` are normalized to null
 * (the JVM-only conversions in the Java version are test scaffolding). Mirrors
 * ClientJsonCodec.encodeWithoutTypeInfo.
 *
 * @param value - the value to encode
 * @returns the value encoded as JSON
 */
export function encodeWithoutTypeInfo(value: unknown): unknown {
  // undefined shouldn't go as undefined; encode it as null.
  return value === null || value === undefined ? null : value;
}

/**
 * Decodes a value transported without type information. In JavaScript the JSON
 * representation is used as-is (the JVM-only conversions in the Java version are
 * test scaffolding). Mirrors ClientJsonCodec.decodeWithoutTypeInfo.
 *
 * @param json - the JSON value to convert
 * @returns the decoded value
 */
export function decodeWithoutTypeInfo(json: unknown): unknown {
  return json;
}

/**
 * Decodes the state node encoded in a type-info-tagged JSON value, if it is an
 * element reference (`@v-node`); returns null otherwise. Mirrors
 * ClientJsonCodec.decodeStateNode (the node-returning counterpart of
 * decodeWithTypeInfo). Used e.g. by ExecuteJavaScriptProcessor to find the state
 * node behind an element parameter.
 *
 * @param tree - the state tree to use for resolving nodes and elements
 * @param json - the JSON value to decode
 * @returns the decoded state node if any
 */
export function decodeStateNode(tree: StateTree, json: unknown): StateNode | null {
  if (typeof json === 'object' && json !== null && !Array.isArray(json)) {
    const nodeIdValue = (json as Record<string, unknown>)['@v-node'];
    if (nodeIdValue !== undefined && nodeIdValue !== null) {
      if (typeof nodeIdValue !== 'number') {
        throw new Error(`@v-node value must be a number, got ${typeof nodeIdValue} in ${JSON.stringify(json)}`);
      }
      return tree.getNode(nodeIdValue);
    }
  }
  return null;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Recursively decodes a JSON object, processing any nested @v references.
 * Returns a native JS object that can store decoded values including DOM
 * elements.
 *
 * Private in Java; covered through the public `decodeWithTypeInfo` surface.
 */
function decodeObjectWithTypeInfo(tree: StateTree, jsonObject: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const key of Object.keys(jsonObject)) {
    result[key] = decodeWithTypeInfo(tree, jsonObject[key]);
  }
  return result;
}

/**
 * Recursively decodes a JSON array, processing any nested @v references.
 *
 * Private in Java; covered through the public `decodeWithTypeInfo` surface.
 */
function decodeArrayWithTypeInfo(tree: StateTree, jsonArray: unknown[]): unknown[] {
  return jsonArray.map((value) => decodeWithTypeInfo(tree, value));
}

function decodeJsFunction(tree: StateTree, fnObject: Record<string, unknown>, originalJson: string): unknown {
  const body = fnObject.body;
  if (typeof body !== 'string') {
    throw new Error(`@v-fn 'body' must be a string in ${originalJson}`);
  }
  const capturesValue = fnObject.captures;
  if (!Array.isArray(capturesValue)) {
    throw new Error(`@v-fn 'captures' must be an array in ${originalJson}`);
  }
  const captures = capturesValue.map((capture) => decodeWithTypeInfo(tree, capture));

  // Optional 'args' field: names of runtime parameters the manifested function
  // should accept at call time, after the bound captures.
  const argsValue = fnObject.args;
  if (argsValue !== undefined && !Array.isArray(argsValue)) {
    throw new Error(`@v-fn 'args' must be an array in ${originalJson}`);
  }
  // Each arg name must be a string (Java reads them with getString(i), which
  // throws on a non-string); validate rather than silently coercing.
  const argNames = ((argsValue as unknown[] | undefined) ?? []).map((argName) => {
    if (typeof argName !== 'string') {
      throw new Error(`@v-fn 'args' must contain only strings in ${originalJson}`);
    }
    return argName;
  });

  const paramsAndCode: string[] = [];
  for (let i = 0; i < captures.length; i++) {
    paramsAndCode.push(`$${i}`);
  }
  paramsAndCode.push(...argNames);
  paramsAndCode.push(body);

  // Manifests a server-sent JS function, mirroring the Java NativeFunction.
  const fn = new Function(...paramsAndCode) as JsFunction;
  return applyCaptures(fn, captures);
}

/**
 * Decodes a value transported with type information: element references
 * (`@v-node` → the DOM node), return channels (`@v-return` → a callback that
 * messages the server), manifested functions (`@v-fn`), and nested
 * objects/arrays (decoded recursively); other values pass through unchanged.
 * Mirrors ClientJsonCodec.decodeWithTypeInfo.
 *
 * @param tree - the state tree to use for resolving nodes and elements
 * @param json - the JSON value to decode
 * @returns the decoded value
 */
export function decodeWithTypeInfo(tree: StateTree, json: unknown): unknown {
  if (isPlainObject(json)) {
    const nodeIdValue = json['@v-node'];
    if (nodeIdValue !== undefined && nodeIdValue !== null) {
      if (typeof nodeIdValue !== 'number') {
        throw new Error(`@v-node value must be a number, got ${typeof nodeIdValue} in ${JSON.stringify(json)}`);
      }
      return tree.getNode(nodeIdValue)?.getDomNode();
    }

    const returnArray = json['@v-return'];
    if (returnArray !== undefined && returnArray !== null) {
      if (!Array.isArray(returnArray)) {
        throw new Error(`@v-return value must be an array in ${JSON.stringify(json)}`);
      }
      if (returnArray.length < 2) {
        throw new Error(`@v-return array must have at least 2 elements in ${JSON.stringify(json)}`);
      }
      const returnNodeId = returnArray[0] as number;
      const channelId = returnArray[1] as number;
      const serverConnector = tree.getRegistry().getServerConnector();
      return createReturnChannelCallback((args) =>
        serverConnector.sendReturnChannelMessage(returnNodeId, channelId, args)
      );
    }

    const fnValue = json['@v-fn'];
    if (fnValue !== undefined && fnValue !== null) {
      if (!isPlainObject(fnValue)) {
        throw new Error(`@v-fn value must be an object in ${JSON.stringify(json)}`);
      }
      return decodeJsFunction(tree, fnValue, JSON.stringify(json));
    }

    for (const key of Object.keys(json)) {
      if (key.startsWith('@v-')) {
        throw new Error(`Unsupported @v type '${key}' in ${JSON.stringify(json)}`);
      }
    }

    return decodeObjectWithTypeInfo(tree, json);
  } else if (Array.isArray(json)) {
    return decodeArrayWithTypeInfo(tree, json);
  }
  return decodeWithoutTypeInfo(json);
}
