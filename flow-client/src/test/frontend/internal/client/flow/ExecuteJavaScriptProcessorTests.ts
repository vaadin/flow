import { expect } from '@open-wc/testing';
import { UIState } from '../../../../../main/frontend/internal/client/UILifecycle';
import { ExecuteJavaScriptProcessor } from '../../../../../main/frontend/internal/client/flow/ExecuteJavaScriptProcessor';
import { ExistingElementMap } from '../../../../../main/frontend/internal/client/ExistingElementMap';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { Reactive } from '../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeProperties';
import { ConstantPool } from '../../../../../main/frontend/internal/client/flow/ConstantPool';
import { type RecordedCalls, recordingRegistry } from './stateTreeTestRegistry';
import { TestRegistry, testRegistry } from '../testRegistry';

// What to run is a constant of the message rather than part of the invocation,
// so a case writes it at the end of an invocation, as the message reads, and
// this puts it in the pool and names it the way the server does.
let nextConstant = 0;
function execute(processor: ExecuteJavaScriptProcessor, registry: TestRegistry, invocations: unknown[][]): void {
  const named = invocations.map((invocation) => {
    nextConstant += 1;
    const key = `constant-${nextConstant}`;
    registry.getConstantPool().importFromJson({ [key]: invocation[invocation.length - 1] });
    return [...invocation.slice(0, -1), key];
  });
  processor.execute(named);
}

// Ported from com.vaadin.client.flow.ExecuteJavaScriptProcessorTest and
// com.vaadin.client.GwtExecuteJavaScriptElementUtilsTest (the return-channel
// case, which drives this class). The cases that exercise the expression
// execution and the context object have no Java counterpart and say so.

// Records what the processor would have executed, as the Java suite's
// CollectingExecuteJavaScriptProcessor does, and answers isBound from a flag the
// case sets rather than from the node.
class CollectingExecuteJavaScriptProcessor extends ExecuteJavaScriptProcessor {
  readonly parameterNamesAndCodeList: string[][] = [];

  readonly parametersList: unknown[][] = [];

  readonly nodeParametersList: Array<Map<unknown, StateNode>> = [];

  bound = true;

  protected override invoke(
    parameterNamesAndCode: string[],
    parameters: unknown[],
    nodeParameters: Map<unknown, StateNode>
  ): void {
    this.parameterNamesAndCodeList.push(parameterNamesAndCode);
    this.parametersList.push(parameters);
    this.nodeParametersList.push(nodeParameters);
  }

  protected override isBound(): boolean {
    return this.bound;
  }
}

// The processor with its own isBound, which the Java suite calls from the
// enclosing class; TypeScript has no package access, so a subclass exposes it.
class TestJsProcessor extends ExecuteJavaScriptProcessor {
  callIsBound(node: StateNode): boolean {
    return this.isBound(node);
  }
}

// A registry holding only the state tree, as the Java fixture's anonymous
// Registry does.
function treeRegistry(services: { existingElementMap?: boolean } = {}): TestRegistry {
  const registry = new TestRegistry();
  registry.register('ConstantPool', new ConstantPool());
  registry.register('StateTree', new StateTree(registry));
  if (services.existingElementMap === true) {
    registry.register('ExistingElementMap', new ExistingElementMap());
  }
  return registry;
}

// A node registered on the registry's tree, and the element a binding would set.
function registeredNode(registry: TestRegistry, id: number): StateNode {
  const node = new StateNode(id, registry.getStateTree());
  registry.getStateTree().registerNode(node);
  return node;
}

describe('ExecuteJavaScriptProcessor', () => {
  describe('JavaScript definition calls', () => {
    // What the server sends: an object naming a function of the bundle, which
    // is identified by a hash of the JavaScript it runs
    const GREETING = 'a'.repeat(64);
    const VALUE = 'b'.repeat(64);
    const greeting = { f: GREETING };
    const value = { f: VALUE };

    type DefinitionFunction = (this: unknown, ...args: unknown[]) => unknown;

    type DefinitionWindow = Window & {
      Vaadin?: {
        Flow?: { jsDefinitions?: Record<string, DefinitionFunction>; jsDefinitionNames?: Record<string, string> };
      };
    };

    // Registers a function the way the generated bundle does, with the name a
    // message calls it by, which a development bundle registers with it.
    function registerDefinition(functionId: string, fn: DefinitionFunction, name?: string): void {
      const vaadin = (window as DefinitionWindow).Vaadin ?? {};
      (window as DefinitionWindow).Vaadin = vaadin;
      vaadin.Flow = vaadin.Flow ?? {};
      vaadin.Flow.jsDefinitions = vaadin.Flow.jsDefinitions ?? {};
      vaadin.Flow.jsDefinitions[functionId] = fn;
      if (name !== undefined) {
        vaadin.Flow.jsDefinitionNames = vaadin.Flow.jsDefinitionNames ?? {};
        vaadin.Flow.jsDefinitionNames[functionId] = name;
      }
    }

    function fixture(): { processor: ExecuteJavaScriptProcessor; registry: TestRegistry } {
      const registry = testRegistry({
        ConstantPool: new ConstantPool(),
        StateTree: { getNode: () => null },
        ApplicationConfiguration: { getApplicationId: () => 'ROOT-1', isProductionMode: () => false }
      });
      return { processor: new ExecuteJavaScriptProcessor(registry), registry };
    }

    function run(invocation: unknown[]): void {
      const { processor, registry } = fixture();
      execute(processor, registry, [invocation]);
    }

    afterEach(() => {
      const flow = (window as DefinitionWindow).Vaadin?.Flow;
      for (const functionId of [GREETING, VALUE]) {
        delete flow?.jsDefinitions?.[functionId];
        delete flow?.jsDefinitionNames?.[functionId];
      }
    });

    it('runs the function from the bundle against the element', () => {
      const calls: Array<{ thisArg: unknown; args: unknown[] }> = [];
      registerDefinition(GREETING, function (this: unknown, greeting: unknown) {
        calls.push({ thisArg: this, args: [greeting] });
      });
      const element = { tagName: 'div' };

      run(['Hello', element, greeting]);

      expect(calls).to.have.lengthOf(1);
      expect(calls[0].thisArg).to.equal(element);
      expect(calls[0].args).to.eql(['Hello']);
    });

    it('runs a call that has nothing to run on without a this', () => {
      // What a call made on the page carries: the arguments, and nothing
      // where a call made on an element has the element
      const calls: unknown[] = [];
      registerDefinition(GREETING, function (this: unknown, greeting: unknown) {
        calls.push({ thisArg: this, greeting });
      });

      run(['Hello', null, greeting]);

      expect(calls).to.eql([{ thisArg: null, greeting: 'Hello' }]);
    });

    it('passes the return value to the success channel', async () => {
      registerDefinition(VALUE, () => 'answer');
      const resolved: unknown[] = [];
      const element = { tagName: 'div' };

      run([element, (returned: unknown) => resolved.push(returned), () => {}, value]);
      // Settled in microtasks: a macrotask wait would also pick up the
      // asynchronous rethrow that the expression cases leave behind.
      await Promise.resolve();
      await Promise.resolve();

      expect(resolved).to.eql(['answer']);
    });

    it('does not run a call whose parameters do not match the function', () => {
      let calls = 0;
      registerDefinition(GREETING, (_greeting: unknown) => {
        calls += 1;
      });

      // One argument the function takes, but no element to apply it to: the
      // invocation and the bundle disagree about the signature, which is the
      // same disagreement as an invocation that carries one parameter too
      // many.
      run(['Hello', greeting]);

      expect(calls).to.equal(0);
    });

    it('reports a mismatch to the error channel, naming the function as it was written', () => {
      let calls = 0;
      registerDefinition(
        VALUE,
        () => {
          calls += 1;
          return 'answer';
        },
        'com.acme.GreeterJs.readValue/0'
      );
      const errors: unknown[] = [];
      const element = { tagName: 'div' };

      // Subscribed to, but one channel short of what the server sends.
      run([element, (error: unknown) => errors.push(error), value]);

      expect(calls).to.equal(0);
      // Reported rather than left hanging: the pending result on the server
      // would otherwise never complete.
      expect(errors).to.have.lengthOf(1);
      // A development bundle registers what a developer wrote next to the
      // function, so a message says more than a hash does
      expect(String(errors[0])).to.contain('com.acme.GreeterJs.readValue/0');
    });

    it('splits the parameters of a variadic call by the count the server sent', () => {
      const calls: Array<{ thisArg: unknown; args: unknown[] }> = [];
      // What the build generates for a method whose last parameter collects
      // the arguments that follow the fixed ones: a rest parameter, which
      // does not count towards the length of the function.
      registerDefinition(GREETING, function (this: unknown, name: unknown, ...rest: unknown[]) {
        calls.push({ thisArg: this, args: [name, rest] });
      });
      const element = { tagName: 'div' };

      run(['greet', 'Alice', 'Bob', element, { f: GREETING, n: 3 }]);

      expect(calls).to.have.lengthOf(1);
      expect(calls[0].thisArg).to.equal(element);
      expect(calls[0].args).to.eql(['greet', ['Alice', 'Bob']]);
    });

    it('reports a function that is not in the bundle to the error channel', () => {
      const errors: unknown[] = [];
      const element = { tagName: 'div' };

      run([element, () => {}, (error: unknown) => errors.push(error), value]);

      expect(errors).to.have.lengthOf(1);
      // Nothing registered it, so the message has only the identifier
      expect(String(errors[0])).to.contain(VALUE);
    });
  });

  describe('execute', () => {
    it('passes the parameters and code of each invocation on', () => {
      // Ported from execute_parametersAndCodeAreValidAndNoNodeParameters.
      const registry = treeRegistry();
      const processor = new CollectingExecuteJavaScriptProcessor(registry);

      execute(processor, registry, [['script1'], ['param1', 'param2', 'script2']]);

      expect(processor.parameterNamesAndCodeList).to.have.length(2);
      expect(processor.parametersList).to.have.length(2);
      expect(processor.nodeParametersList).to.have.length(2);

      expect(processor.parameterNamesAndCodeList[0]).to.deep.equal(['script1']);
      expect(processor.parametersList[0]).to.have.length(0);

      expect(processor.parameterNamesAndCodeList[1]).to.deep.equal(['$0', '$1', 'script2']);
      expect(processor.parametersList[1]).to.deep.equal(['param1', 'param2']);

      expect(processor.nodeParametersList[0].size).to.equal(0);
      expect(processor.nodeParametersList[1].size).to.equal(0);
    });

    it('runs nothing for an invocation whose constant is not there', () => {
      // A message that named a constant of one that never arrived, or arrived
      // out of order: running the name as a script is the one thing that must
      // not happen.
      const registry = treeRegistry();
      const processor = new CollectingExecuteJavaScriptProcessor(registry);

      processor.execute([['neverimported']]);

      expect(processor.parameterNamesAndCodeList).to.have.length(0);
    });

    it('runs a string constant as an expression, whatever it looks like', () => {
      // A function is named by an object, so an expression that happens to
      // read like the identifier of one is still an expression
      const registry = treeRegistry();
      const processor = new CollectingExecuteJavaScriptProcessor(registry);

      execute(processor, registry, [['a'.repeat(64)]]);

      expect(processor.parameterNamesAndCodeList).to.deep.equal([['a'.repeat(64)]]);
    });

    it('passes a node parameter as the element it is bound to', () => {
      // Ported from execute_nodeParametersAreCorrectlyPassed.
      const registry = treeRegistry({ existingElementMap: true });
      const processor = new CollectingExecuteJavaScriptProcessor(registry);
      const node = registeredNode(registry, 10);
      const element = document.createElement('div');
      node.setDomNode(element);

      execute(processor, registry, [[{ '@v-node': node.getId() }, '$0']]);

      expect(processor.nodeParametersList).to.have.length(1);
      expect(processor.nodeParametersList[0].size).to.equal(1);
      expect(processor.nodeParametersList[0].get(element)).to.equal(node);
    });

    it('waits for a virtual child awaiting initialization', () => {
      // Ported from execute_nodeParameterIsVirtualChildAwaitingInit.
      const registry = treeRegistry();
      const processor = new CollectingExecuteJavaScriptProcessor(registry);
      const node = new StateNode(11, registry.getStateTree());
      node
        .getMap(NodeFeatures.ELEMENT_DATA)
        .getProperty(NodeProperties.PAYLOAD)
        .setValue({ [NodeProperties.TYPE]: NodeProperties.INJECT_BY_ID });
      registry.getStateTree().registerNode(node);

      execute(processor, registry, [[{ '@v-node': node.getId() }, '$0']]);

      // The invocation has not been executed
      expect(processor.nodeParametersList).to.have.length(0);

      // emulate binding
      const element = document.createElement('div');
      node.setDomNode(element);
      Reactive.flush();

      expect(processor.nodeParametersList).to.have.length(1);
      expect(processor.nodeParametersList[0].size).to.equal(1);
      expect(processor.nodeParametersList[0].get(element)).to.equal(node);
    });

    it('waits for a node that is not bound yet', () => {
      // Ported from execute_nodeParameterIsHidden.
      const registry = treeRegistry();
      const processor = new CollectingExecuteJavaScriptProcessor(registry);
      const node = registeredNode(registry, 31);
      processor.bound = false;

      execute(processor, registry, [[{ '@v-node': node.getId() }, '$0']]);

      expect(processor.nodeParametersList).to.have.length(0);

      // emulate binding
      const element = document.createElement('div');
      node.setDomNode(element);
      processor.bound = true;
      Reactive.flush();

      expect(processor.nodeParametersList).to.have.length(1);
      expect(processor.nodeParametersList[0].size).to.equal(1);
      expect(processor.nodeParametersList[0].get(element)).to.equal(node);
    });

    it('executes for a node that is no virtual child', () => {
      // Ported from execute_nodeParameterIsNotVirtualChild.
      const registry = treeRegistry();
      const processor = new CollectingExecuteJavaScriptProcessor(registry);
      const node = registeredNode(registry, 12);

      execute(processor, registry, [[{ '@v-node': node.getId() }, '$0']]);

      // The invocation has been executed
      expect(processor.nodeParametersList).to.have.length(1);
    });
  });

  describe('isBound', () => {
    // Each case builds the node the way the Java one does: 37 for the node under
    // test, 43 for its parent, and an element standing in for a bound DOM node.
    function bindable(): { processor: TestJsProcessor; registry: TestRegistry } {
      const registry = treeRegistry();
      return { processor: new TestJsProcessor(registry), registry };
    }

    it('is false for a node with no element', () => {
      // Ported from isBound_noElement_notBound.
      const { processor, registry } = bindable();
      expect(processor.callIsBound(new StateNode(37, registry.getStateTree()))).to.be.false;
    });

    it('is true for a node with an element and no bound property', () => {
      // Ported from isBound_hasElementHasNoFeature_bound.
      const { processor, registry } = bindable();
      const node = new StateNode(37, registry.getStateTree());
      node.setDomNode(document.createElement('div'));
      expect(processor.callIsBound(node)).to.be.true;
    });

    it('is true for a node whose bound property is true', () => {
      // Ported from isBound_hasElementHasFeatureAndBound_bound.
      const { processor, registry } = bindable();
      const node = new StateNode(37, registry.getStateTree());
      node.setDomNode(document.createElement('div'));
      node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY).setValue(true);
      expect(processor.callIsBound(node)).to.be.true;
    });

    it('is false for a node whose bound property is false', () => {
      // Ported from isBound_hasElementHasFeatureAndNotBound_notBound.
      const { processor, registry } = bindable();
      const node = new StateNode(37, registry.getStateTree());
      node.setDomNode(document.createElement('div'));
      node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY).setValue(false);
      expect(processor.callIsBound(node)).to.be.false;
    });

    it('is true for a node with a bound parent', () => {
      // Ported from isBound_hasElementHasNoFeatureAndBoundParent_bound.
      const { processor, registry } = bindable();
      const node = new StateNode(37, registry.getStateTree());
      const parent = new StateNode(43, registry.getStateTree());
      node.setParent(parent);
      const element = document.createElement('div');
      node.setDomNode(element);
      parent.setDomNode(element);
      expect(processor.callIsBound(node)).to.be.true;
    });

    it('is false for a node with an unbound parent', () => {
      // Ported from isBound_hasElementHasNoFeatureAndUnboundParent_notBound.
      const { processor, registry } = bindable();
      const node = new StateNode(37, registry.getStateTree());
      const parent = new StateNode(43, registry.getStateTree());
      node.setParent(parent);
      node.setDomNode(document.createElement('div'));
      expect(processor.callIsBound(node)).to.be.false;
    });

    it('is false for a bound node with an unbound parent', () => {
      // Ported from isBound_hasElementHasFeatureAndBoundAndUnboundParent_notBound.
      const { processor, registry } = bindable();
      const node = new StateNode(37, registry.getStateTree());
      node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY).setValue(true);
      const parent = new StateNode(43, registry.getStateTree());
      node.setParent(parent);
      node.setDomNode(document.createElement('div'));
      expect(processor.callIsBound(node)).to.be.false;
    });
  });

  describe('class execute', () => {
    function makeRegistry() {
      const lifecycleStates: UIState[] = [];
      return {
        lifecycleStates,
        registry: testRegistry({
          ConstantPool: new ConstantPool(),
          StateTree: { getNode: () => null },
          ApplicationConfiguration: { getApplicationId: () => 'ROOT-1', isProductionMode: () => false },
          UILifecycle: { isTerminated: () => false, setState: (state: UIState) => lifecycleStates.push(state) }
        })
      };
    }

    // The invocation of one expression that most cases here run
    function runExpression(expression: string): void {
      const registry = makeRegistry().registry;
      execute(new ExecuteJavaScriptProcessor(registry), registry, [[expression]]);
    }

    afterEach(() => {
      delete (globalThis as Record<string, unknown>).__ejpRan;
      delete (globalThis as Record<string, unknown>).__ejpParam;
    });

    it('passes a return channel to the expression and sends its arguments to the server', () => {
      // Ported from testReturnChannel_passedToExecJavaScript_messageSentToServer.
      const built = recordingRegistry();
      const recorded: RecordedCalls = built.recorded;
      const tree = new StateTree(built.registry);
      const registry = testRegistry({
        ConstantPool: new ConstantPool(),
        StateTree: tree,
        ApplicationConfiguration: { getApplicationId: () => 'test', isProductionMode: () => false },
        UILifecycle: { isTerminated: () => false, setState: () => {} }
      });

      const expectedNodeId = 10;
      const expectedChannelId = 20;

      // The @v-return parameter decodes to a callback; the expression calls it.
      execute(new ExecuteJavaScriptProcessor(registry), registry, [
        [{ '@v-return': [expectedNodeId, expectedChannelId] }, '$0(2)']
      ]);

      expect(recorded.returnChannelMessages).to.deep.equal([
        { nodeId: expectedNodeId, channelId: expectedChannelId, args: [2] }
      ]);
    });

    it('runs an invocation expression', () => {
      // Beyond the Java suite.
      runExpression('globalThis.__ejpRan = true;');
      expect((globalThis as Record<string, unknown>).__ejpRan).to.be.true;
    });

    it('binds invocation parameters to $0, $1, ...', () => {
      // Beyond the Java suite.
      const registry = makeRegistry().registry;
      execute(new ExecuteJavaScriptProcessor(registry), registry, [['hello', 'globalThis.__ejpParam = $0;']]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('hello');
    });

    it('exposes the app id with the per-UI suffix stripped', () => {
      // Beyond the Java suite.
      runExpression('globalThis.__ejpParam = this.$appId;');
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('ROOT');
    });

    it('exposes the registry on the context', () => {
      // Beyond the Java suite.
      runExpression('globalThis.__ejpParam = this.registry === undefined;');
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal(false);
    });

    it('reports an unknown node to an element callback as a reference error', () => {
      // Beyond the Java suite.
      // getNode throws when the argument is not a state-node parameter; the
      // executed code sees that as a thrown ReferenceError.
      runExpression(
        'try { this.attachExistingElement({}); } catch (e) { globalThis.__ejpParam = e.constructor.name; }'
      );
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('ReferenceError');
    });

    it('catches exceptions thrown by the executed code', () => {
      // Beyond the Java suite.
      expect(() => runExpression('throw new Error("boom");')).to.not.throw();
    });

    it('exposes stopApplication on the context, terminating the UI lifecycle', () => {
      // Beyond the Java suite.
      const fixture = makeRegistry();
      execute(new ExecuteJavaScriptProcessor(fixture.registry), fixture.registry, [['this.stopApplication();']]);
      expect(fixture.lifecycleStates).to.deep.equal([UIState.TERMINATED]);
    });
  });
});
