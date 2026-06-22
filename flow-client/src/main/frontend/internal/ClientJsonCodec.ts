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

// Implementations migrated from ClientJsonCodec.java, registered on
// window.Vaadin.Flow.internal.ClientJsonCodec by registerInternals; the Java
// methods delegate here. Also bundled to ES5 for the HtmlUnit used by GwtTests.

type JsFunction = (...args: unknown[]) => unknown;

/**
 * Creates the function exposed for a server return channel. Calling it forwards
 * all of its arguments (as an array) to the supplied sender, which the Java
 * caller wires (exception-guarded) to ServerConnector.sendReturnChannelMessage.
 */
export function createReturnChannelCallback(sendMessage: (args: unknown[]) => void): JsFunction {
  return (...args: unknown[]) => {
    sendMessage(args);
  };
}

/**
 * Wraps `fn` in a function that prepends `captures` to the runtime arguments
 * before delegating, while leaving `this` controlled by the caller (unlike
 * Function.prototype.bind, which would pre-bind `this`).
 */
export function applyCaptures(fn: JsFunction, captures: ArrayLike<unknown>): JsFunction {
  return function (this: unknown, ...callArgs: unknown[]): unknown {
    const args = Array.from(captures).concat(callArgs);
    return fn.apply(this, args);
  };
}
