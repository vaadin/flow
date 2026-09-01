import { expect } from '@open-wc/testing';
import { ExecuteJavaScriptProcessor } from '../../../../../main/frontend/internal/client/flow/ExecuteJavaScriptProcessor';
import { ExistingElementMap } from '../../../../../main/frontend/internal/client/ExistingElementMap';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { Reactive } from '../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeProperties';
import { type RecordedCalls, recordingRegistry } from './stateTreeTestRegistry';
import { TestRegistry } from '../testRegistry';

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
  describe('execute', () => {
    it('passes the parameters and code of each invocation on', () => {
      // Ported from execute_parametersAndCodeAreValidAndNoNodeParameters.
      const processor = new CollectingExecuteJavaScriptProcessor(treeRegistry());

      processor.execute([['script1'], ['param1', 'param2', 'script2']]);

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

    it('passes a node parameter as the element it is bound to', () => {
      // Ported from execute_nodeParametersAreCorrectlyPassed.
      const registry = treeRegistry({ existingElementMap: true });
      const processor = new CollectingExecuteJavaScriptProcessor(registry);
      const node = registeredNode(registry, 10);
      const element = document.createElement('div');
      node.setDomNode(element);

      processor.execute([[{ '@v-node': node.getId() }, '$0']]);

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

      processor.execute([[{ '@v-node': node.getId() }, '$0']]);

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

      processor.execute([[{ '@v-node': node.getId() }, '$0']]);

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

      processor.execute([[{ '@v-node': node.getId() }, '$0']]);

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
      const lifecycleStates: string[] = [];
      const registry = {
        lifecycleStates,
        getStateTree: () => ({ getNode: () => null }),
        getApplicationConfiguration: () => ({ getApplicationId: () => 'ROOT-1', isProductionMode: () => false }),
        getUILifecycle: () => ({ isTerminated: () => false, setState: (state: string) => lifecycleStates.push(state) })
      };
      return registry;
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
      const registry = {
        getStateTree: () => tree,
        getApplicationConfiguration: () => ({ getApplicationId: () => 'test', isProductionMode: () => false }),
        getUILifecycle: () => ({ isTerminated: () => false, setState: () => {} })
      };

      const expectedNodeId = 10;
      const expectedChannelId = 20;

      // The @v-return parameter decodes to a callback; the expression calls it.
      new ExecuteJavaScriptProcessor(registry as never).execute([
        [{ '@v-return': [expectedNodeId, expectedChannelId] }, '$0(2)']
      ]);

      expect(recorded.returnChannelMessages).to.deep.equal([
        { nodeId: expectedNodeId, channelId: expectedChannelId, args: [2] }
      ]);
    });

    it('runs an invocation expression', () => {
      // Beyond the Java suite.
      new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([['globalThis.__ejpRan = true;']]);
      expect((globalThis as Record<string, unknown>).__ejpRan).to.be.true;
    });

    it('binds invocation parameters to $0, $1, ...', () => {
      // Beyond the Java suite.
      new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([['hello', 'globalThis.__ejpParam = $0;']]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('hello');
    });

    it('exposes the app id with the per-UI suffix stripped', () => {
      // Beyond the Java suite.
      new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([['globalThis.__ejpParam = this.$appId;']]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('ROOT');
    });

    it('exposes the registry on the context', () => {
      // Beyond the Java suite.
      const registry = makeRegistry();
      new ExecuteJavaScriptProcessor(registry as never).execute([
        ['globalThis.__ejpParam = this.registry === undefined;']
      ]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal(false);
    });

    it('reports an unknown node to an element callback as a reference error', () => {
      // Beyond the Java suite.
      // getNode throws when the argument is not a state-node parameter; the
      // executed code sees that as a thrown ReferenceError.
      new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([
        ['try { this.attachExistingElement({}); } catch (e) { globalThis.__ejpParam = e.constructor.name; }']
      ]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('ReferenceError');
    });

    it('catches exceptions thrown by the executed code', () => {
      // Beyond the Java suite.
      expect(() =>
        new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([['throw new Error("boom");']])
      ).to.not.throw();
    });

    it('exposes stopApplication on the context, terminating the UI lifecycle', () => {
      // Beyond the Java suite.
      const registry = makeRegistry();
      new ExecuteJavaScriptProcessor(registry as never).execute([['this.stopApplication();']]);
      expect(registry.lifecycleStates).to.deep.equal(['TERMINATED']);
    });
  });
});
