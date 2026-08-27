import { expect } from '@open-wc/testing';
import { ExecuteJavaScriptProcessor } from '../../../main/frontend/internal/client/flow/ExecuteJavaScriptProcessor';

// Ported from com.vaadin.client.flow.ExecuteJavaScriptProcessorTest.
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

    it('runs an invocation expression', () => {
      new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([['globalThis.__ejpRan = true;']]);
      expect((globalThis as Record<string, unknown>).__ejpRan).to.be.true;
    });

    it('binds invocation parameters to $0, $1, ...', () => {
      new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([['hello', 'globalThis.__ejpParam = $0;']]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('hello');
    });

    it('exposes the app id with the per-UI suffix stripped', () => {
      new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([['globalThis.__ejpParam = this.$appId;']]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('ROOT');
    });

    it('exposes the registry on the context', () => {
      const registry = makeRegistry();
      new ExecuteJavaScriptProcessor(registry as never).execute([
        ['globalThis.__ejpParam = this.registry === undefined;']
      ]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal(false);
    });

    it('reports an unknown node to an element callback as a reference error', () => {
      // getNode throws when the argument is not a state-node parameter; the
      // executed code sees that as a thrown ReferenceError.
      new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([
        ['try { this.attachExistingElement({}); } catch (e) { globalThis.__ejpParam = e.constructor.name; }']
      ]);
      expect((globalThis as Record<string, unknown>).__ejpParam).to.equal('ReferenceError');
    });

    it('catches exceptions thrown by the executed code', () => {
      expect(() =>
        new ExecuteJavaScriptProcessor(makeRegistry() as never).execute([['throw new Error("boom");']])
      ).to.not.throw();
    });

    it('exposes stopApplication on the context, terminating the UI lifecycle', () => {
      const registry = makeRegistry();
      new ExecuteJavaScriptProcessor(registry as never).execute([['this.stopApplication();']]);
      expect(registry.lifecycleStates).to.deep.equal(['TERMINATED']);
    });
  });
});
