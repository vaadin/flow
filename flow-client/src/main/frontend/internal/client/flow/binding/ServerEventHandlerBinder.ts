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

type Registration = { remove(): void };

type ServerEventObjectLike = {
  defineMethod(methodName: string, node: unknown, returnValue: boolean): void;
  removeMethod(methodName: string): void;
};

type StateNodeLike = {
  getList(featureId: number): {
    length(): number;
    get(index: number): unknown;
    addSpliceListener(listener: (event: { getRemove(): unknown[]; getAdd(): unknown[] }) => void): Registration;
  };
};

/**
 * Binds and updates server-event handler names. Migrated from
 * `com.vaadin.client.flow.binding.ServerEventHandlerBinder`.
 *
 * Both Java callers go through `bindServerEventHandlerNames`. The original
 * short form (`bindServerEventHandlerNames(element, node)`) is reconstructed
 * on the Java facade side as an `@JsOverlay` that delegates here with the
 * `CLIENT_DELEGATE_HANDLERS` feature id, returnValue=true, and a supplier
 * that resolves to `ServerEventObject.get(element)`.
 */
export const ServerEventHandlerBinder = {
  // eslint-disable-next-line @typescript-eslint/max-params
  bindServerEventHandlerNames(
    objectProvider: () => ServerEventObjectLike,
    node: StateNodeLike,
    featureId: number,
    returnValue: boolean
  ): Registration {
    const serverEventHandlerNamesList = node.getList(featureId);
    if (serverEventHandlerNamesList.length() > 0) {
      const object = objectProvider();
      for (let i = 0; i < serverEventHandlerNamesList.length(); i++) {
        object.defineMethod(serverEventHandlerNamesList.get(i) as string, node, returnValue);
      }
    }
    return serverEventHandlerNamesList.addSpliceListener((event) => {
      const serverObject = objectProvider();
      const remove = event.getRemove();
      for (const name of remove) {
        serverObject.removeMethod(name as string);
      }
      const add = event.getAdd();
      for (const name of add) {
        serverObject.defineMethod(name as string, node, returnValue);
      }
    });
  }
};
