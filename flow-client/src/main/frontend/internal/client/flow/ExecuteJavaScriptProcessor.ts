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
import { Console } from '../Console';
import { ExecuteJavaScriptElementUtils } from '../ExecuteJavaScriptElementUtils';
import { ClientJsonCodec } from './util/ClientJsonCodec';
import { Reactive } from './reactive/Reactive';

// Mirrors NodeFeatures.ELEMENT_DATA + NodeProperties.* literals.
const ELEMENT_DATA = 0;
const PAYLOAD = 'payload';
const TYPE_KEY = 'type';
const INJECT_BY_ID = '@id';
const TEMPLATE_IN_TEMPLATE = 'subTemplate';
const VISIBILITY_BOUND_PROPERTY = 'bound';

type StateNodeLike = {
  getId(): number;
  getDomNode(): unknown;
  getTree(): StateTreeLike;
  getParent(): StateNodeLike | null;
  getMap(featureId: number): NodeMapLike;
  addDomNodeSetListener(listener: (node: StateNodeLike) => boolean): unknown;
};

type NodeMapLike = {
  hasPropertyValue(name: string): boolean;
  getProperty(name: string): { getValue(): unknown };
};

type StateTreeLike = {
  getNode(id: number): StateNodeLike | null;
  getRegistry(): RegistryLike;
};

type RegistryLike = {
  getStateTree(): StateTreeLike;
  getUILifecycle(): { isTerminated(): boolean; setState(state: string): void };
  getApplicationConfiguration(): { isProductionMode(): boolean; getApplicationId(): string };
};

/**
 * Processes the result of `Page.executeJs(...)` payloads received from the
 * server. Migrated from `com.vaadin.client.flow.ExecuteJavaScriptProcessor`.
 *
 * Element-utility calls (`attachExistingElement`, `populateModelProperties`,
 * `registerUpdatableModelProperties`) dispatch directly into the migrated
 * `ExecuteJavaScriptElementUtils` TS module.
 */
export class ExecuteJavaScriptProcessor {
  private readonly registry: RegistryLike;

  constructor(registry: RegistryLike) {
    this.registry = registry;
  }

  execute(invocations: unknown[][]): void {
    for (const invocation of invocations) {
      this.handleInvocation(invocation);
    }
  }

  private handleInvocation(invocation: unknown[]): void {
    const tree = this.registry.getStateTree();
    // Last item is the script body; the rest are parameters.
    const parameterCount = invocation.length - 1;
    const parameterNamesAndCode: string[] = new Array(parameterCount + 1);
    const parameters: unknown[] = [];
    const map = new Map<unknown, StateNodeLike>();

    for (let i = 0; i < parameterCount; i++) {
      const parameterJson = invocation[i];
      const parameter = ClientJsonCodec.decodeWithTypeInfo(tree as never, parameterJson);
      parameters.push(parameter);
      parameterNamesAndCode[i] = '$' + i;
      const stateNode = ClientJsonCodec.decodeStateNode(tree as never, parameterJson) as StateNodeLike | null;
      if (stateNode !== null) {
        if (
          ExecuteJavaScriptProcessor.isVirtualChildAwaitingInitialization(stateNode) ||
          !ExecuteJavaScriptProcessor.isBound(stateNode)
        ) {
          stateNode.addDomNodeSetListener(() => {
            Reactive.addPostFlushListener(() => this.handleInvocation(invocation));
            return true;
          });
          return;
        }
        map.set(parameter, stateNode);
      }
    }

    parameterNamesAndCode[parameterNamesAndCode.length - 1] = invocation[invocation.length - 1] as string;
    this.invoke(parameterNamesAndCode, parameters, map);
  }

  private static isVirtualChildAwaitingInitialization(node: StateNodeLike): boolean {
    if (node.getDomNode() != null || node.getTree().getNode(node.getId()) === null) {
      return false;
    }
    const elementData = node.getMap(ELEMENT_DATA);
    if (elementData.hasPropertyValue(PAYLOAD)) {
      const value = elementData.getProperty(PAYLOAD).getValue() as Record<string, unknown> | null;
      if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
        const type = value[TYPE_KEY];
        return type === INJECT_BY_ID || type === TEMPLATE_IN_TEMPLATE;
      }
    }
    return false;
  }

  // Walks up the parent chain confirming each ancestor has a DOM node and is
  // not flagged for rebind. Mirrors SimpleElementBindingStrategy.needsRebind.
  private static isBound(node: StateNodeLike): boolean {
    const isNodeBound = node.getDomNode() != null && !ExecuteJavaScriptProcessor.needsRebind(node);
    if (!isNodeBound || node.getParent() === null) {
      return isNodeBound;
    }
    return ExecuteJavaScriptProcessor.isBound(node.getParent()!);
  }

  private static needsRebind(node: StateNodeLike): boolean {
    // Absence of value or "true" means no rebind needed; only literal `false`
    // signals rebind.
    return node.getMap(ELEMENT_DATA).getProperty(VISIBILITY_BOUND_PROPERTY).getValue() === false;
  }

  private invoke(
    parameterNamesAndCode: string[],
    parameters: unknown[],
    nodeParameters: Map<unknown, StateNodeLike>
  ): void {
    if (parameterNamesAndCode.length !== parameters.length + 1) {
      throw new Error('parameterNamesAndCode/parameters length mismatch');
    }
    try {
      // `new Function(name1, ..., nameN, body)` returns a function with the
      // given parameter names and body; .apply binds `this` to the context.
      const fn = new Function(...parameterNamesAndCode);
      fn.apply(
        this.getContextExecutionObject(nodeParameters, () => {
          if (!this.registry.getUILifecycle().isTerminated()) {
            this.registry.getUILifecycle().setState('TERMINATED');
          }
        }),
        parameters
      );
    } catch (exception) {
      Console.reportStacktrace(exception);
      Console.error('Exception is thrown during JavaScript execution. Stacktrace will be dumped separately.');
      if (!this.registry.getApplicationConfiguration().isProductionMode()) {
        const code = parameterNamesAndCode.join(', ');
        Console.error(`The error has occurred in the JS code: '${code}'`);
      }
    }
  }

  private getContextExecutionObject(nodeParameters: Map<unknown, StateNodeLike>, stopApplication: () => void): object {
    const cleanedAppId = this.registry.getApplicationConfiguration().getApplicationId().replace(/-\d+$/, '');
    return ExecuteJavaScriptProcessor.getContextExecutionObject(
      nodeParameters as Map<unknown, unknown>,
      cleanedAppId,
      this.registry,
      (parent, previousSibling, tagName, id) =>
        ExecuteJavaScriptElementUtils.attachExistingElement(
          parent as never,
          previousSibling as Element | null,
          tagName,
          id
        ),
      (node, properties) =>
        ExecuteJavaScriptElementUtils.populateModelProperties(node as never, properties as string[]),
      (node, properties) =>
        ExecuteJavaScriptElementUtils.registerUpdatableModelProperties(node as never, properties as string[]),
      stopApplication,
      (node, id, cleanup) => ExecuteJavaScriptElementUtils.registerInitializer(node as never, id, cleanup),
      (node, id) => ExecuteJavaScriptElementUtils.disposeInitializer(node as never, id)
    );
  }

  /**
   * Builds the per-execution `this`-object that user JS sees when Flow runs
   * `executeJs` payloads. Static so callers can compose contexts directly
   * without an enclosing processor instance.
   */
  // eslint-disable-next-line @typescript-eslint/max-params
  static getContextExecutionObject(
    nodeParameters: Map<unknown, unknown>,
    appId: string,
    registry: unknown,
    attachExisting: (parent: unknown, previousSibling: unknown, tagName: string, id: number) => void,
    populateModel: (node: unknown, properties: unknown) => void,
    registerUpdatable: (node: unknown, properties: unknown) => void,
    stopApplication: () => void,
    registerInitializer: (node: unknown, id: number, cleanup: () => void) => void,
    disposeInitializer: (node: unknown, id: number) => void
  ): object {
    const getNode = (element: unknown): unknown => {
      const node = nodeParameters.get(element);
      if (node == null) {
        throw new ReferenceError('There is no a StateNode for the given argument.');
      }
      return node;
    };
    return {
      getNode,
      $appId: appId,
      registry,
      attachExistingElement(parent: unknown, previousSibling: unknown, tagName: string, id: number) {
        attachExisting(getNode(parent), previousSibling, tagName, id);
      },
      populateModelProperties(element: unknown, properties: unknown) {
        populateModel(getNode(element), properties);
      },
      registerUpdatableModelProperties(element: unknown, properties: unknown) {
        registerUpdatable(getNode(element), properties);
      },
      stopApplication() {
        stopApplication();
      },
      registerInitializer(node: unknown, id: number, cleanup: () => void) {
        registerInitializer(node, id, cleanup);
      },
      disposeInitializer(node: unknown, id: number) {
        disposeInitializer(node, id);
      }
    };
  }
}
