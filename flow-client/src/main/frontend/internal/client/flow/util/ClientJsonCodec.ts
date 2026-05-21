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

type ReturnChannelArgs = (...args: unknown[]) => void;

type StateNodeLike = { getDomNode(): Node | null };

type StateTreeLike = {
  getNode(id: number): StateNodeLike | null;
  getRegistry(): { getServerConnector(): ServerConnectorLike };
};

type ServerConnectorLike = {
  sendReturnChannelMessage(stateNodeId: number, channelId: number, args: unknown[]): void;
};

/**
 * Static helpers for encoding/decoding JSON between the client and server.
 * Migrated from `com.vaadin.client.flow.util.ClientJsonCodec`.
 *
 * In compiled JavaScript the elemental.json `asXxx()` accessors collapse to
 * the same JS primitive the value already is, so the encode/decode paths
 * here are essentially passthroughs except for the `@v-node` /
 * `@v-return` type tags the server uses to mark state-node references and
 * return channels.
 */
export const ClientJsonCodec = {
  createReturnChannelCallback(send: (args: unknown[]) => void): ReturnChannelArgs {
    return function (...args: unknown[]): void {
      send(args);
    };
  },

  decodeStateNode(tree: StateTreeLike, json: unknown): StateNodeLike | null {
    if (!isPlainObject(json)) {
      return null;
    }
    const nodeIdValue = (json as Record<string, unknown>)['@v-node'];
    if (nodeIdValue === undefined) {
      return null;
    }
    if (typeof nodeIdValue !== 'number') {
      throw new Error(`@v-node value must be a number, got ${typeof nodeIdValue} in ${JSON.stringify(json)}`);
    }
    return tree.getNode(nodeIdValue);
  },

  decodeWithTypeInfo(tree: StateTreeLike, json: unknown): unknown {
    if (isPlainObject(json)) {
      const obj = json as Record<string, unknown>;
      const nodeIdValue = obj['@v-node'];
      if (nodeIdValue !== undefined) {
        if (typeof nodeIdValue !== 'number') {
          throw new Error(`@v-node value must be a number, got ${typeof nodeIdValue} in ${JSON.stringify(json)}`);
        }
        return tree.getNode(nodeIdValue)?.getDomNode() ?? null;
      }
      const returnArray = obj['@v-return'];
      if (returnArray !== undefined) {
        if (!Array.isArray(returnArray)) {
          throw new Error(`@v-return value must be an array, got ${typeof returnArray} in ${JSON.stringify(json)}`);
        }
        if (returnArray.length < 2) {
          throw new Error(
            `@v-return array must have at least 2 elements, got ${returnArray.length} in ${JSON.stringify(json)}`
          );
        }
        const returnNodeId = returnArray[0] as number;
        const channelId = returnArray[1] as number;
        const serverConnector = tree.getRegistry().getServerConnector();
        return ClientJsonCodec.createReturnChannelCallback((args) =>
          serverConnector.sendReturnChannelMessage(returnNodeId, channelId, args)
        );
      }
      const fnValue = obj['@v-fn'];
      if (fnValue !== undefined) {
        if (!isPlainObject(fnValue)) {
          throw new Error(`@v-fn value must be an object, got ${typeof fnValue} in ${JSON.stringify(json)}`);
        }
        return decodeJsFunction(tree, fnValue as Record<string, unknown>, JSON.stringify(json));
      }
      // Unknown @v- types should fail loudly.
      for (const key of Object.keys(obj)) {
        if (key.startsWith('@v-')) {
          throw new Error(`Unsupported @v type '${key}' in ${JSON.stringify(json)}`);
        }
      }
      // Recursively decode nested @v references; mutate the object in place
      // so callers retain the same JS object reference.
      for (const key of Object.keys(obj)) {
        obj[key] = ClientJsonCodec.decodeWithTypeInfo(tree, obj[key]);
      }
      return obj;
    }
    if (Array.isArray(json)) {
      return (json as unknown[]).map((item) => ClientJsonCodec.decodeWithTypeInfo(tree, item));
    }
    return ClientJsonCodec.decodeWithoutTypeInfo(json);
  },

  decodeWithoutTypeInfo(json: unknown): unknown {
    // In JS the elemental.json `asBoolean/asNumber/asString` accessors return
    // the underlying primitive the JsonValue is wrapping, so the JS value is
    // already the right shape — passthrough.
    return json;
  },

  encodeWithoutTypeInfo(value: unknown): unknown {
    // Same reasoning as decodeWithoutTypeInfo. Undefined must become null
    // because the server can't distinguish them, and treating undefined as
    // "absent" would break callers that explicitly send a null value.
    if (value === undefined) {
      return null;
    }
    return value;
  }
};

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Decodes a `@v-fn` payload — a serialized JS function with `body`, bound
 * `captures`, and optionally named runtime `args`. The result is a function
 * that, when called, prepends the captures to the runtime arguments before
 * evaluating the body. Mirrors the Java `decodeJsFunction` +
 * `applyCaptures` JSNI helper.
 */
function decodeJsFunction(
  tree: StateTreeLike,
  fnObject: Record<string, unknown>,
  originalJson: string
): (...args: unknown[]) => unknown {
  const body = fnObject['body'];
  if (typeof body !== 'string') {
    throw new Error(`@v-fn 'body' must be a string in ${originalJson}`);
  }
  const capturesJson = fnObject['captures'];
  if (!Array.isArray(capturesJson)) {
    throw new Error(`@v-fn 'captures' must be an array in ${originalJson}`);
  }
  const captures = capturesJson.map((capture) => ClientJsonCodec.decodeWithTypeInfo(tree, capture));

  // Optional 'args': names of runtime parameters the manifested function
  // should accept at call time, after the bound captures.
  const argsJson = fnObject['args'];
  let argNames: string[];
  if (argsJson === undefined || argsJson === null) {
    argNames = [];
  } else if (Array.isArray(argsJson)) {
    argNames = argsJson.map((a) => String(a));
  } else {
    throw new Error(`@v-fn 'args' must be an array in ${originalJson}`);
  }

  const paramNames: string[] = [];
  for (let i = 0; i < captures.length; i++) {
    paramNames.push('$' + i);
  }
  paramNames.push(...argNames);
  // eslint-disable-next-line @typescript-eslint/no-implied-eval
  const fn = new Function(...paramNames, body) as (...args: unknown[]) => unknown;

  // Wrap `fn` so captures are prepended to runtime args, keeping `this`
  // under the caller's control (Function.prototype.bind would pre-bind it).
  return function (this: unknown, ...runtimeArgs: unknown[]): unknown {
    const allArgs = captures.concat(runtimeArgs);
    return fn.apply(this, allArgs);
  };
}
