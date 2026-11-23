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

/**
 * Binds and updates server object able to send notifications to the server.
 */

// TypeScript port of com.vaadin.client.flow.binding.ServerEventHandlerBinder,
// built on top of the ServerEventObject. It binds the server event handler names
// from a node feature onto the element's $server object and keeps them in sync
// as the feature's list changes.

import type { EventRemover } from '../../../EventRemover';
import { NodeFeatures } from '../../../flow/internal/nodefeature/NodeFeatures';
import type { StateNode } from '../StateNode';
import type * as ServerEventObject from './ServerEventObject';
import { defineMethod, get, removeMethod, type ServerObject } from './ServerEventObject';

/**
 * Registers all the server event handler names found in the
 * {@link NodeFeatures.CLIENT_DELEGATE_HANDLERS} feature in the state node
 * as `serverObject.<methodName>`. Additionally listens to
 * changes in the feature and updates `$server` accordingly.
 *
 * @param element - the element to update
 * @param node - the state node containing the feature
 * @returns a handle which can be used to remove the listener for the feature
 */
export function bindServerEventHandlerNames(element: Element, node: StateNode): EventRemover;

/**
 * Registers all the server event handler names found in the feature with
 * the `featureId` in the {@link ServerEventObject} `object`.
 * Additionally listens to changes in the feature and updates server event
 * object accordingly.
 *
 * @param objectProvider - the provider of the event object to update
 * @param node - the state node containing the feature
 * @param featureId - the feature id which contains event handler methods
 * @param returnValue - `true` if the handler should return a promise that
 *   will reflect the server-side result; `false` to not
 *   return any value
 * @returns a handle which can be used to remove the listener for the feature
 */
export function bindServerEventHandlerNames(
  objectProvider: () => ServerObject,
  node: StateNode,
  featureId: number,
  returnValue: boolean
): EventRemover;

// The two Java overloads -- (Element, StateNode) and (Supplier, StateNode, int,
// boolean) -- merge into one implementation, since TypeScript has no separate
// bodies per overload signature. The declared overloads above keep the Java call
// shapes; this signature discriminates on the first parameter and carries the
// defaults the two-argument Java overload passes on.
export function bindServerEventHandlerNames(
  elementOrProvider: Element | (() => ServerObject),
  node: StateNode,
  featureId: number = NodeFeatures.CLIENT_DELEGATE_HANDLERS,
  returnValue: boolean = true
): EventRemover {
  const objectProvider = typeof elementOrProvider === 'function' ? elementOrProvider : () => get(elementOrProvider);

  const serverEventHandlerNamesList = node.getList(featureId);

  if (serverEventHandlerNamesList.length() > 0) {
    const object = objectProvider();

    for (let i = 0; i < serverEventHandlerNamesList.length(); i++) {
      defineMethod(object, serverEventHandlerNamesList.get(i) as string, node, returnValue);
    }
  }

  return serverEventHandlerNamesList.addSpliceListener((e) => {
    const serverObject = objectProvider();

    const remove = e.getRemove();
    for (const name of remove) {
      removeMethod(serverObject, name as string);
    }

    const add = e.getAdd();
    for (const name of add) {
      defineMethod(serverObject, name as string, node, returnValue);
    }
  });
}
