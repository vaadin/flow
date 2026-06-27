import { expect } from '@open-wc/testing';
import { getContextExecutionObject, invokeJavaScript } from '../../main/frontend/internal/ExecuteJavaScriptProcessor';

describe('ExecuteJavaScriptProcessor', () => {
  function makeCallbacks() {
    const calls: Array<[string, unknown[]]> = [];
    const record =
      (name: string) =>
      (...args: unknown[]) => {
        calls.push([name, args]);
      };
    const nodes = new Map<unknown, unknown>();
    const callbacks = {
      getNode: (element: unknown) => {
        const node = nodes.get(element);
        if (node == null) {
          throw new ReferenceError('There is no a StateNode for the given argument.');
        }
        return node;
      },
      attachExistingElement: record('attachExistingElement'),
      populateModelProperties: record('populateModelProperties'),
      registerUpdatableModelProperties: record('registerUpdatableModelProperties'),
      stopApplication: record('stopApplication'),
      registerInitializer: record('registerInitializer'),
      disposeInitializer: record('disposeInitializer')
    };
    return { calls, nodes, callbacks };
  }

  it('strips the trailing per-UI suffix from the app id', () => {
    const { callbacks } = makeCallbacks();
    const object = getContextExecutionObject('ROOT-1', {}, callbacks);
    expect(object.$appId).to.equal('ROOT');
  });

  it('exposes the registry and the node resolver', () => {
    const { callbacks } = makeCallbacks();
    const registry = { marker: true };
    const object = getContextExecutionObject('app', registry, callbacks);
    expect(object.registry).to.equal(registry);
    expect(object.getNode).to.equal(callbacks.getNode);
  });

  it('resolves the node via getNode before invoking element callbacks', () => {
    const { calls, nodes, callbacks } = makeCallbacks();
    const element = {};
    const node = { id: 7 };
    nodes.set(element, node);
    const object = getContextExecutionObject('app', {}, callbacks);

    (object.attachExistingElement as (...a: unknown[]) => void)(element, 'sibling', 'div', 'id');
    (object.populateModelProperties as (...a: unknown[]) => void)(element, ['p']);
    (object.registerUpdatableModelProperties as (...a: unknown[]) => void)(element, ['q']);

    expect(calls).to.deep.equal([
      ['attachExistingElement', [node, 'sibling', 'div', 'id']],
      ['populateModelProperties', [node, ['p']]],
      ['registerUpdatableModelProperties', [node, ['q']]]
    ]);
  });

  it('throws when an element callback targets an unknown node', () => {
    const { callbacks } = makeCallbacks();
    const object = getContextExecutionObject('app', {}, callbacks);
    expect(() => (object.attachExistingElement as (...a: unknown[]) => void)({}, null, 'div', 'id')).to.throw(
      ReferenceError
    );
  });

  it('passes node-taking callbacks straight through', () => {
    const { calls, callbacks } = makeCallbacks();
    const object = getContextExecutionObject('app', {}, callbacks);
    const node = { id: 1 };

    (object.stopApplication as () => void)();
    (object.registerInitializer as (...a: unknown[]) => void)(node, 'id', 'cleanup');
    (object.disposeInitializer as (...a: unknown[]) => void)(node, 'id');

    expect(calls).to.deep.equal([
      ['stopApplication', []],
      ['registerInitializer', [node, 'id', 'cleanup']],
      ['disposeInitializer', [node, 'id']]
    ]);
  });

  describe('invokeJavaScript', () => {
    it('runs the expression with the parameters bound to $0, $1, ...', () => {
      const target: Record<string, unknown> = {};
      invokeJavaScript(['$0', '$1', '$0.value = $1;'], [target, 42], {}, true);
      expect(target.value).to.equal(42);
    });

    it('runs the expression with the given context as this', () => {
      const context: Record<string, unknown> = {};
      invokeJavaScript(['this.ran = true;'], [], context, true);
      expect(context.ran).to.be.true;
    });

    it('catches exceptions thrown by the executed code', () => {
      expect(() => invokeJavaScript(['throw new Error("boom");'], [], {}, false)).to.not.throw();
      expect(() => invokeJavaScript(['throw new Error("boom");'], [], {}, true)).to.not.throw();
    });
  });
});
