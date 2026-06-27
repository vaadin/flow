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

// The executeJs context-object builder migrated from
// ExecuteJavaScriptProcessor.java, registered on
// window.Vaadin.Flow.internal.ExecuteJavaScriptProcessor by registerInternals;
// the Java method delegates here. The callbacks are supplied from the Java side
// already wrapped in $entry (so exceptions thrown asynchronously from the
// executed script still reach GWT's uncaught-exception handler). The element ->
// node resolution (getNode) keeps its $entry boundary on the Java side; here we
// only assemble the object the executed script runs against. Also bundled to
// ES5 for the HtmlUnit used by GwtTests.
//
// The ExecuteJavaScriptProcessor class below is the build-alongside TS port of
// the rest of ExecuteJavaScriptProcessor.java: it decodes the invocation
// parameters, defers until any referenced node is bound, then manifests and runs
// the expression against a context wired to the ExecuteJavaScriptElementUtils
// callbacks. Composes the ported decoders / invokeJavaScript /
// getContextExecutionObject / element-utils / needsRebind. The Registry/StateTree
// are contracts satisfied at cutover.

import { needsRebind } from './binding/SimpleElementBindingStrategy';
import { decodeStateNode, decodeWithTypeInfo } from './ClientJsonCodec';
import {
  attachExistingElement,
  disposeInitializer,
  populateModelProperties,
  registerInitializer,
  registerUpdatableModelProperties
} from './ExecuteJavaScriptElementUtils';
import { Reactive } from './reactive/reactive';
import type { StateNode } from './StateNode';
import type { StateTree } from './StateTree';
import { UIState } from './UILifecycle';

// NodeFeatures.ELEMENT_DATA / NodeProperties
const ELEMENT_DATA = 0;
const PAYLOAD = 'payload';
const TYPE_PROPERTY = 'type';
const INJECT_BY_ID = '@id';
const TEMPLATE_IN_TEMPLATE = 'subTemplate';

// The $entry-wrapped callbacks the executed script invokes via its context.
// attachExistingElement / populateModelProperties / registerUpdatableModelProperties
// take a resolved StateNode (looked up via getNode); the rest take their
// arguments directly.
interface ContextCallbacks {
  getNode: (element: unknown) => unknown;
  attachExistingElement: (node: unknown, previousSibling: unknown, tagName: unknown, id: unknown) => void;
  populateModelProperties: (node: unknown, properties: unknown) => void;
  registerUpdatableModelProperties: (node: unknown, properties: unknown) => void;
  stopApplication: () => void;
  registerInitializer: (node: unknown, id: unknown, cleanup: unknown) => void;
  disposeInitializer: (node: unknown, id: unknown) => void;
}

/**
 * Builds the object the executed JavaScript runs against (its `this`). The
 * application id has its trailing per-UI suffix (`-<number>`) stripped so the
 * script sees the stable app id.
 */
export function getContextExecutionObject(
  appId: string,
  registry: unknown,
  callbacks: ContextCallbacks
): Record<string, unknown> {
  const object: Record<string, unknown> = {};
  object.getNode = callbacks.getNode;
  object.$appId = appId.replace(/-\d+$/, '');
  object.registry = registry;
  object.attachExistingElement = (parent: unknown, previousSibling: unknown, tagName: unknown, id: unknown): void =>
    callbacks.attachExistingElement(callbacks.getNode(parent), previousSibling, tagName, id);
  object.populateModelProperties = (element: unknown, properties: unknown): void =>
    callbacks.populateModelProperties(callbacks.getNode(element), properties);
  object.registerUpdatableModelProperties = (element: unknown, properties: unknown): void =>
    callbacks.registerUpdatableModelProperties(callbacks.getNode(element), properties);
  object.stopApplication = callbacks.stopApplication;
  object.registerInitializer = callbacks.registerInitializer;
  object.disposeInitializer = callbacks.disposeInitializer;
  return object;
}

/**
 * Manifests and runs a server-sent JavaScript invocation: builds a function from
 * the parameter names followed by the expression, then applies it with the given
 * context as `this` and the parameter values as arguments. Exceptions are caught
 * and reported (the failing code is logged outside production mode). Mirrors
 * ExecuteJavaScriptProcessor.invoke (the context object is assembled by
 * getContextExecutionObject).
 */
export function invokeJavaScript(
  parameterNamesAndCode: string[],
  parameters: unknown[],
  context: object,
  productionMode: boolean
): void {
  try {
    // The last entry is the expression; the rest are parameter names.
    const fn = new Function(...parameterNamesAndCode) as (this: object, ...args: unknown[]) => unknown;
    fn.apply(context, parameters);
  } catch (exception) {
    console.error('Exception is thrown during JavaScript execution. Stacktrace will be dumped separately.');
    console.error(exception);
    if (!productionMode) {
      // Java brackets the snippets then strips the brackets, netting the join.
      console.error(`The error has occurred in the JS code: '${parameterNamesAndCode.join(', ')}'`);
    }
  }
}

/** The slice of Registry ExecuteJavaScriptProcessor uses. */
interface ExecuteJsRegistry {
  getStateTree(): StateTree;
  getApplicationConfiguration(): { getApplicationId(): string; isProductionMode(): boolean };
  getUILifecycle(): { isTerminated(): boolean; setState(state: string): void };
}

/** Executes server-sent JavaScript invocations against the live tree; mirrors ExecuteJavaScriptProcessor.java. */
export class ExecuteJavaScriptProcessor {
  private readonly registry: ExecuteJsRegistry;

  constructor(registry: ExecuteJsRegistry) {
    this.registry = registry;
  }

  /** Runs each invocation (an array of parameters followed by the JS expression). */
  execute(invocations: unknown[][]): void {
    for (const invocation of invocations) {
      this.handleInvocation(invocation);
    }
  }

  private handleInvocation(invocation: unknown[]): void {
    const tree = this.registry.getStateTree();
    // Last item is the script, the rest are parameters.
    const parameterCount = invocation.length - 1;

    const parameterNamesAndCode: string[] = [];
    const parameters: unknown[] = [];
    const nodeParameters = new Map<unknown, StateNode>();

    for (let i = 0; i < parameterCount; i++) {
      const parameterJson = invocation[i];
      // The real StateTree's ServerConnector has sendReturnChannelMessage (used
      // by the @v-return branch); StateTree's narrower type omits it.
      const parameter = decodeWithTypeInfo(tree as never, parameterJson);
      parameters.push(parameter);
      parameterNamesAndCode.push(`$${i}`);

      const stateNode = decodeStateNode(tree, parameterJson);
      if (stateNode !== null) {
        if (this.isVirtualChildAwaitingInitialization(stateNode) || !this.isBound(stateNode)) {
          // Defer until the node's DOM is set, then retry the whole invocation.
          stateNode.addDomNodeSetListener(() => {
            Reactive.addPostFlushListener(() => this.handleInvocation(invocation));
            return true;
          });
          return;
        }
        nodeParameters.set(parameter, stateNode);
      }
    }

    parameterNamesAndCode.push(invocation[invocation.length - 1] as string);
    this.invoke(parameterNamesAndCode, parameters, nodeParameters);
  }

  private invoke(
    parameterNamesAndCode: string[],
    parameters: unknown[],
    nodeParameters: Map<unknown, StateNode>
  ): void {
    const configuration = this.registry.getApplicationConfiguration();
    const getNode = (element: unknown): unknown => {
      const node = nodeParameters.get(element);
      if (node === undefined) {
        throw new ReferenceError('There is no a StateNode for the given argument.');
      }
      return node;
    };
    const context = getContextExecutionObject(configuration.getApplicationId(), this.registry, {
      getNode,
      attachExistingElement: (node, previousSibling, tagName, id) =>
        attachExistingElement(node as never, previousSibling as Element | null, tagName as string, id as number),
      populateModelProperties: (node, properties) => populateModelProperties(node as never, properties as string[]),
      registerUpdatableModelProperties: (node, properties) =>
        registerUpdatableModelProperties(node as never, properties as string[]),
      stopApplication: () => {
        const lifecycle = this.registry.getUILifecycle();
        if (!lifecycle.isTerminated()) {
          lifecycle.setState(UIState.TERMINATED);
        }
      },
      registerInitializer: (node, id, cleanup) =>
        registerInitializer(node as never, id as number, cleanup as () => void),
      disposeInitializer: (node, id) => disposeInitializer(node as never, id as number)
    });
    invokeJavaScript(parameterNamesAndCode, parameters, context, configuration.isProductionMode());
  }

  // A node is bound once it has a DOM node that does not need rebinding, and so
  // is each of its ancestors.
  private isBound(node: StateNode): boolean {
    const isNodeBound = node.getDomNode() !== null && !needsRebind(node as never);
    const parent = node.getParent();
    if (!isNodeBound || parent === null) {
      return isNodeBound;
    }
    return this.isBound(parent);
  }

  // A virtual child injected by id / as a sub-template is awaiting initialization
  // until its DOM node is created.
  private isVirtualChildAwaitingInitialization(node: StateNode): boolean {
    if (node.getDomNode() !== null || node.getTree().getNode(node.getId()) === null) {
      return false;
    }
    const elementData = node.getMap(ELEMENT_DATA);
    if (elementData.hasPropertyValue(PAYLOAD)) {
      const value = elementData.getProperty(PAYLOAD).getValue();
      if (value !== null && typeof value === 'object') {
        const type = (value as Record<string, unknown>)[TYPE_PROPERTY];
        return type === INJECT_BY_ID || type === TEMPLATE_IN_TEMPLATE;
      }
    }
    return false;
  }
}
