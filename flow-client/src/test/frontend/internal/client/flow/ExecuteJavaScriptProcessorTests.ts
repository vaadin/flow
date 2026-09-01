import { expect } from '@open-wc/testing';
import { ExecuteJavaScriptProcessor } from '../../../../../main/frontend/internal/client/flow/ExecuteJavaScriptProcessor';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { type RecordedCalls, recordingRegistry } from './stateTreeTestRegistry';

// Ported from com.vaadin.client.GwtExecuteJavaScriptElementUtilsTest (the
// return-channel case, which drives this class). The five execute_* and seven
// isBound_* cases of ExecuteJavaScriptProcessorTest each need state nodes built
// for them and are tracked in the retrofit backlog; the remaining cases here
// cover the expression execution and the context object, and have no Java
// counterpart.
describe('ExecuteJavaScriptProcessor', () => {
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
