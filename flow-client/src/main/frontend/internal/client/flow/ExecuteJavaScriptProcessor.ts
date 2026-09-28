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
// getContextExecutionObject / element-utils / needsRebind.

import type { Registry } from '../Registry';
import { NodeProperties } from '../../flow/internal/nodefeature/NodeProperties';
import { NodeFeatures } from '../../flow/internal/nodefeature/NodeFeatures';
import { assert } from '../../assert';
import { needsRebind } from './binding/SimpleElementBindingStrategy';
import { decodeStateNode, decodeWithTypeInfo } from './util/ClientJsonCodec';
import {
  attachExistingElement,
  disposeInitializer,
  populateModelProperties,
  registerInitializer,
  registerUpdatableModelProperties
} from '../ExecuteJavaScriptElementUtils';
import { Reactive } from './reactive/Reactive';
import type { StateNode } from './StateNode';
import { UIState } from '../UILifecycle';
import { Console } from '../Console';
import { JsonConstants } from '../../flow/shared/JsonConstants';

// NodeFeatures.NodeFeatures.ELEMENT_DATA / NodeProperties

// The $entry-wrapped callbacks the executed script invokes via its context.
// attachExistingElement / populateModelProperties / registerUpdatableModelProperties
// take a resolved StateNode (looked up via getNode); the rest take their
// arguments directly.
interface ContextCallbacks {
  getNode: (element: unknown) => StateNode;

  attachExistingElement: (node: StateNode, previousSibling: Element | null, tagName: string, id: number) => void;
  populateModelProperties: (node: StateNode, properties: string[]) => void;
  registerUpdatableModelProperties: (node: StateNode, properties: string[]) => void;
  stopApplication: () => void;
  registerInitializer: (node: StateNode, id: number, cleanup: () => void) => void;
  disposeInitializer: (node: StateNode, id: number) => void;
}

type JsDefinitionFunction = (this: unknown, ...args: unknown[]) => unknown;

/**
 * What an invocation of declared JavaScript names instead of an expression:
 * the function of the bundle to run, identified by a hash of the JavaScript it
 * runs. An invocation that runs an expression names the expression itself, a
 * string, so the two are told apart by what the constant is rather than by
 * what it says.
 */
type JsFunctionConstant = Record<typeof JsonConstants.UIDL_KEY_JS_FUNCTION, string> &
  Partial<Record<typeof JsonConstants.UIDL_KEY_JS_ARGUMENT_COUNT, number>>;

type ReturnChannel = (value: unknown) => void;

/**
 * Reports the given message through the error channel of an invocation, which
 * is its last parameter when it was subscribed to, so that the pending result
 * of the call is completed rather than left hanging on the server.
 */
function reportThroughChannel(parameters: unknown[], message: string): void {
  const lastParameter = parameters[parameters.length - 1];
  if (typeof lastParameter === 'function') {
    (lastParameter as ReturnChannel)(message);
  }
}

/**
 * What the generated bundle registers on the page: a function per declared
 * expression, and, outside production, what a developer wrote for each of
 * them.
 */
function getDeclaredJavaScript(): {
  jsDefinitions?: Record<string, JsDefinitionFunction>;
  jsDefinitionNames?: Record<string, string>;
} {
  return (
    (
      window as unknown as {
        Vaadin?: {
          Flow?: { jsDefinitions?: Record<string, JsDefinitionFunction>; jsDefinitionNames?: Record<string, string> };
        };
      }
    ).Vaadin?.Flow ?? {}
  );
}

/**
 * Looks up the function that the build generated for declared JavaScript. The
 * registry is populated by the generated bundle, so the function is ordinary
 * bundled code and nothing has to be compiled from a string here.
 */
function findDeclaredFunction(functionId: string): JsDefinitionFunction | undefined {
  return getDeclaredJavaScript().jsDefinitions?.[functionId];
}

/**
 * What to call a function in a message: what a developer wrote, which a
 * development bundle registers next to the function itself, and the identifier
 * of the function when it does not, as in production.
 */
function getNameOf(functionId: string): string {
  return getDeclaredJavaScript().jsDefinitionNames?.[functionId] ?? functionId;
}

/**
 * Processes the result of `Page.executeJs` on the client. `Page` is a
 * flow-server class, outside this port, so the reference stays a code span.
 */
export class ExecuteJavaScriptProcessor {
  readonly #registry: Registry;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: Registry) {
    this.#registry = registry;
  }

  /**
   * Executes invocations received from the server.
   *
   * @param invocations - a JSON containing invocation data
   */
  execute(invocations: unknown[][]): void {
    for (const invocation of invocations) {
      this.#handleInvocation(invocation);
    }
  }

  #handleInvocation(invocation: unknown[]): void {
    const tree = this.#registry.getStateTree();
    // Last item names what to run in the constant pool, the rest are parameters.
    const parameterCount = invocation.length - 1;

    const parameterNamesAndCode: string[] = [];
    const parameters: unknown[] = [];
    const nodeParameters = new Map<unknown, StateNode>();

    for (let i = 0; i < parameterCount; i++) {
      const parameterJson = invocation[i];
      // The real StateTree's ServerConnector has sendReturnChannelMessage (used
      // by the @v-return branch); StateTree's narrower type omits it.
      const parameter = decodeWithTypeInfo(tree, parameterJson);
      parameters.push(parameter);
      parameterNamesAndCode.push(`$${i}`);

      const stateNode = decodeStateNode(tree, parameterJson);
      if (stateNode !== null) {
        if (this.#isVirtualChildAwaitingInitialization(stateNode) || !this.isBound(stateNode)) {
          // Defer until the node's DOM is set, then retry the whole invocation.
          stateNode.addDomNodeSetListener(() => {
            Reactive.addPostFlushListener(() => this.#handleInvocation(invocation));
            return true;
          });
          return;
        }
        nodeParameters.set(parameter, stateNode);
      }
    }

    // What to run is a constant of the message, the same way for an
    // expression and for a call of declared JavaScript, so an expression the
    // server runs again costs a reference rather than its own text.
    const whatToRun = this.#registry
      .getConstantPool()
      .get<string | JsFunctionConstant | null>(invocation[invocation.length - 1] as string);
    if (whatToRun === null) {
      Console.error(
        `No constant for the invocation ${JSON.stringify(invocation)}. Reload the page to pick up the current state.`
      );
      return;
    }

    if (typeof whatToRun === 'object') {
      // A call of declared JavaScript: the bundle has the function, the
      // server sent only which one to run. The node parameters are for the
      // context object an expression runs against, whose `getNode` maps an
      // element back to its state node; a declared function runs against the
      // element itself and has no context, so there is nothing that could ask.
      this.invokeFromBundle(
        whatToRun[JsonConstants.UIDL_KEY_JS_FUNCTION],
        parameters,
        whatToRun[JsonConstants.UIDL_KEY_JS_ARGUMENT_COUNT]
      );
      return;
    }

    parameterNamesAndCode.push(whatToRun);
    this.invoke(parameterNamesAndCode, parameters, nodeParameters);
  }

  // A virtual child injected by id / as a sub-template is awaiting initialization
  // until its DOM node is created.
  #isVirtualChildAwaitingInitialization(node: StateNode): boolean {
    if (node.getDomNode() !== null || node.getTree().getNode(node.getId()) === null) {
      return false;
    }
    const elementData = node.getMap(NodeFeatures.ELEMENT_DATA);
    if (elementData.hasPropertyValue(NodeProperties.PAYLOAD)) {
      const value = elementData.getProperty(NodeProperties.PAYLOAD).getValue();
      if (value !== null && typeof value === 'object') {
        const type = (value as Record<string, unknown>)[NodeProperties.TYPE];
        return type === NodeProperties.INJECT_BY_ID || type === NodeProperties.TEMPLATE_IN_TEMPLATE;
      }
    }
    return false;
  }

  // A node is bound once it has a DOM node that does not need rebinding, and so
  // is each of its ancestors.
  protected isBound(node: StateNode): boolean {
    const isNodeBound = node.getDomNode() !== null && !needsRebind(node);
    const parent = node.getParent();
    if (!isNodeBound || parent === null) {
      return isNodeBound;
    }
    return this.isBound(parent);
  }

  /**
   * Executes the actual invocation.
   *
   * Protected instead of private for testing purposes, as in Java.
   *
   * @param parameterNamesAndCode - an array consisting of parameter names
   *          followed by the JavaScript expression to execute
   * @param parameters - an array of parameter values
   * @param nodeParameters - the node parameters
   */
  protected invoke(
    parameterNamesAndCode: string[],
    parameters: unknown[],
    nodeParameters: Map<unknown, StateNode>
  ): void {
    const configuration = this.#registry.getApplicationConfiguration();
    const getNode = (element: unknown): StateNode => {
      const node = nodeParameters.get(element);
      if (node === undefined) {
        throw new ReferenceError('There is no a StateNode for the given argument.');
      }
      return node;
    };
    const context = getContextExecutionObject(configuration.getApplicationId(), this.#registry, {
      getNode,
      attachExistingElement,
      populateModelProperties,
      registerUpdatableModelProperties,
      stopApplication: () => {
        const lifecycle = this.#registry.getUILifecycle();
        if (!lifecycle.isTerminated()) {
          lifecycle.setState(UIState.TERMINATED);
        }
      },
      registerInitializer,
      disposeInitializer
    });
    invokeJavaScript(parameterNamesAndCode, parameters, context, configuration.isProductionMode());
  }

  /**
   * Executes a call made through a JavaScript definition: looks the function up
   * in the registry that the generated bundle populates and applies it to the
   * element, with the arguments of the call. Nothing is compiled from a string,
   * which is what makes this path work under a content security policy that
   * does not allow `unsafe-eval`.
   *
   * Protected instead of private for testing purposes, as `invoke` is.
   *
   * @param functionId - the identifier of the function to run
   * @param parameters - the decoded parameters: the arguments of the call, the
   *          element to apply the function to, and the return value channels
   *          when the call is subscribed to
   * @param sentArgumentCount - how many arguments the call carries, sent for a
   *          function that collects them into a rest parameter and does not
   *          report them in its length
   */
  protected invokeFromBundle(functionId: string, parameters: unknown[], sentArgumentCount?: number): void {
    const name = getNameOf(functionId);
    const fn = findDeclaredFunction(functionId);
    if (fn === undefined) {
      const message = `No JavaScript in the bundle for ${name}. The JavaScript definition is annotated with @JsDefinition, but the build did not collect it.`;
      Console.error(message);
      // The server appends the two channels after everything else, or neither
      // of them, so the error channel is the last parameter. Report through it
      // when there is one, or the pending result of the call is never
      // completed on the server.
      reportThroughChannel(parameters, message);
      return;
    }

    // The function takes the arguments of the call, so what follows them is
    // the element to apply it to, and then the two return value channels when
    // the call is subscribed to. Nothing else may be in there, so a count that
    // does not add up means the invocation was not built for this function,
    // and reading the element out of it by index would bind an argument as
    // `this`. Say so instead of running the call.
    const argumentCount = sentArgumentCount ?? fn.length;
    const afterTheArguments = parameters.length - argumentCount;
    if (afterTheArguments !== 1 && afterTheArguments !== 3) {
      const message = `Expected ${argumentCount} arguments and the element for ${name} but the invocation carries ${parameters.length} parameters. Reload the page to pick up the current signature.`;
      Console.error(message);
      reportThroughChannel(parameters, message);
      return;
    }

    const returns = afterTheArguments === 3;
    const onSuccess = returns ? (parameters[argumentCount + 1] as ReturnChannel) : undefined;
    const onError = returns ? (parameters[argumentCount + 2] as ReturnChannel) : undefined;

    // The element the definition was obtained from is the parameter after the
    // arguments, and it is what the function runs against.
    const thisArg = parameters[argumentCount];
    try {
      const result = fn.apply(thisArg, parameters.slice(0, argumentCount));
      if (onSuccess !== undefined) {
        Promise.resolve(result).then(onSuccess, (error: unknown) => onError?.(`${error}`));
      }
    } catch (exception) {
      Console.reportStacktrace(exception);
      Console.error(`Exception is thrown while running ${name}. Stacktrace will be dumped separately.`);
      onError?.(`${exception}`);
    }
  }
}

/**
 * Builds the object the executed JavaScript runs against (its `this`). The
 * application id has its trailing per-UI suffix (`-<number>`) stripped so the
 * script sees the stable app id.
 */
function getContextExecutionObject(
  appId: string,
  registry: unknown,
  callbacks: ContextCallbacks
): Record<string, unknown> {
  const object: Record<string, unknown> = {};
  object.getNode = callbacks.getNode;
  object.$appId = appId.replace(/-\d+$/, '');
  object.registry = registry;
  object.attachExistingElement = (parent: unknown, previousSibling: unknown, tagName: unknown, id: unknown): void =>
    callbacks.attachExistingElement(
      callbacks.getNode(parent),
      previousSibling as Element | null,
      tagName as string,
      id as number
    );
  object.populateModelProperties = (element: unknown, properties: unknown): void =>
    callbacks.populateModelProperties(callbacks.getNode(element), properties as string[]);
  object.registerUpdatableModelProperties = (element: unknown, properties: unknown): void =>
    callbacks.registerUpdatableModelProperties(callbacks.getNode(element), properties as string[]);
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
function invokeJavaScript(
  parameterNamesAndCode: string[],
  parameters: unknown[],
  context: object,
  productionMode: boolean
): void {
  assert(
    parameterNamesAndCode.length === parameters.length + 1,
    'Expected one more entry in parameterNamesAndCode than there are parameters'
  );

  try {
    // The last entry is the expression; the rest are parameter names.
    const fn = new Function(...parameterNamesAndCode) as (this: object, ...args: unknown[]) => unknown;
    fn.apply(context, parameters);
  } catch (exception) {
    // Reported through the ported Console, which rethrows asynchronously and so
    // is not subject to the production-mode log suppression - keeping the stack
    // that a logged message alone would lose.
    Console.reportStacktrace(exception);
    Console.error('Exception is thrown during JavaScript execution. Stacktrace will be dumped separately.');
    if (!productionMode) {
      Console.error(exception);
      // Java brackets the snippets then strips the brackets, netting the join.
      Console.error(`The error has occurred in the JS code: '${parameterNamesAndCode.join(', ')}'`);
    }
  }
}
