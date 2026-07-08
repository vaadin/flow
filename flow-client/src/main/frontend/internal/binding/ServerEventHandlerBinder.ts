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

// TypeScript port of com.vaadin.client.flow.binding.ServerEventHandlerBinder,
// built on top of the ServerEventObject. It binds the server event handler names
// from a node feature onto the element's $server object and keeps them in sync
// as the feature's list changes.

import type { EventRemover } from '../reactive/reactive';
import { NodeFeatures } from '../nodefeature/NodeFeatures';
import { defineMethod, get, removeMethod, type ServerEventNode, type ServerObject } from '../ServerEventObject';

/** The splice-event slice that the handler-name listener reads. */
interface HandlerNamesSpliceEvent {
  getRemove(): unknown[];
  getAdd(): unknown[];
}

/** The slice of NodeList that holds the server event handler names. */
interface HandlerNamesList {
  length(): number;
  get(index: number): unknown;
  addSpliceListener(listener: (event: HandlerNamesSpliceEvent) => void): EventRemover;
}

/** The slice of StateNode that the binder uses (also satisfies defineMethod). */
export interface ServerEventHandlerNode extends ServerEventNode {
  getList(featureId: number): HandlerNamesList;
}

/**
 * Registers all the server event handler names found in the
 * CLIENT_DELEGATE_HANDLERS feature of the state node as
 * `serverObject.<methodName>`, and listens for changes to keep `$server` in
 * sync. Mirrors the (element, node) overload of bindServerEventHandlerNames.
 */
export function bindServerEventHandlerNames(element: Element, node: ServerEventHandlerNode): EventRemover;

/**
 * Registers all the server event handler names found in the given feature in
 * the $server object supplied by objectProvider, and listens for changes to
 * keep it in sync. Mirrors the (objectProvider, node, featureId, returnValue)
 * overload of bindServerEventHandlerNames.
 */
export function bindServerEventHandlerNames(
  objectProvider: () => ServerObject,
  node: ServerEventHandlerNode,
  featureId: number,
  returnValue: boolean
): EventRemover;

export function bindServerEventHandlerNames(
  elementOrProvider: Element | (() => ServerObject),
  node: ServerEventHandlerNode,
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
