import { expect } from '@open-wc/testing';
import { NodeFeatures } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { JsonConstants } from '../../../../../../main/frontend/internal/flow/shared/JsonConstants';
import { bindServerEventHandlerNames } from '../../../../../../main/frontend/internal/client/flow/binding/ServerEventHandlerBinder';
import { getMethods } from '../../../../../../main/frontend/internal/client/flow/binding/ServerEventObject';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { BindGuardStateNode, type CollectingTree, StateNode, bind, makeCollectingTree } from '../bindingTestHelpers';

// A NodeList stand-in holding handler names, with a hook to fire a splice event.
function fakeList(items: string[]) {
  const listeners: Array<(e: { getRemove(): unknown[]; getAdd(): unknown[] }) => void> = [];
  return {
    items,
    length: () => items.length,
    get: (i: number) => items[i],
    addSpliceListener(listener: (e: { getRemove(): unknown[]; getAdd(): unknown[] }) => void) {
      listeners.push(listener);
      return { remove: () => listeners.splice(listeners.indexOf(listener), 1) };
    },
    fireSplice(remove: string[], add: string[]) {
      listeners.forEach((l) => l({ getRemove: () => remove, getAdd: () => add }));
    }
  };
}

// A StateNode stand-in: a feature list plus the slice defineMethod needs.
function fakeNode(list: ReturnType<typeof fakeList>, featureId: number): any {
  const sent: Array<{ methodName: string; promiseId: number }> = [];
  return {
    sent,
    getList: (id: number) => (id === featureId ? list : fakeList([])),
    getMap: () => ({ hasPropertyValue: () => false }),
    getDomNode: () => null,
    getTree: () => ({
      getRegistry: () => ({ getConstantPool: () => ({ get: () => [] }) }),
      sendTemplateEventToServer: (_n: unknown, methodName: string, _args: unknown[], promiseId: number) =>
        sent.push({ methodName, promiseId })
    })
  };
}

// Client-callable handler binding, exercised the way GwtEventHandlerTest does
// it: bind a node carrying CLIENT_DELEGATE_HANDLERS to a real element and read
// the resulting $server object back off the element.
describe('ServerEventHandlerBinder', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: any;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    // Populate the "element data" feature so the node can be bound as a plain
    // element.
    node.getMap(NodeFeatures.ELEMENT_DATA);
    element = document.createElement('div');
  });

  function handlers() {
    return node.getList(NodeFeatures.CLIENT_DELEGATE_HANDLERS);
  }

  // assertPublishedMethods: the $server object carries exactly these methods.
  function expectPublishedMethods(expected: string[]): void {
    const published = element.$server === undefined ? [] : getMethods(element.$server);
    expect([...published].sort()).to.deep.equal([...expected].sort());
  }

  function hasPromise(promiseId: number): boolean {
    return promiseId in element.$server[JsonConstants.RPC_PROMISE_CALLBACK_NAME].promises;
  }

  it('puts nothing in the DOM when there is no server event handler', () => {
    // Ported from testNoServerEventHandler_nothingInDom.
    bind(node, element);
    Reactive.flush();

    expect(element.$server).to.equal(undefined);
  });

  it('returns a promise per client-callable invocation and settles it by id', async () => {
    // Ported from testClientCallablePromises.
    const methodName = 'publishedMethod';

    handlers().add(0, methodName);
    bind(node, element);
    Reactive.flush();

    const serverObject = element.$server;

    const promise0 = serverObject[methodName]();
    expect(promise0).to.not.equal(undefined);
    expect(harness.templateEvents[0].promiseId).to.equal(0);
    expect(hasPromise(0)).to.be.true;

    const promise1 = serverObject[methodName]();
    expect(harness.templateEvents[1].promiseId).to.equal(1);
    expect(hasPromise(1)).to.be.true;

    // completePromise(element, 0, true, 'promise0')
    serverObject[JsonConstants.RPC_PROMISE_CALLBACK_NAME](0, true, 'promise0');
    expect(await promise0).to.equal('promise0');
    expect(hasPromise(0), 'Promise handlers should be cleared').to.be.false;

    // completePromise(element, 1, false, null)
    serverObject[JsonConstants.RPC_PROMISE_CALLBACK_NAME](1, false, null);
    const message = await (promise1 as Promise<unknown>).then(
      () => 'resolved',
      (error: Error) => String(error)
    );
    expect(message).to.equal('Error: Something went wrong. Check server-side logs for more information.');
    expect(hasPromise(1), 'Promise handlers should be cleared').to.be.false;
  });

  it('publishes a client-callable method on the element', () => {
    // Ported from testClientCallableMethodInDom.
    handlers().add(0, 'publishedMethod');
    bind(node, element);
    Reactive.flush();

    expectPublishedMethods(['publishedMethod']);
  });

  it('publishes a handler added after the initial binding', () => {
    // Ported from testAddClientCallableHandlerMethod.
    handlers().add(0, 'initialMethod');
    bind(node, element);
    Reactive.flush();
    expectPublishedMethods(['initialMethod']);

    handlers().add(0, 'newFirstMethod');
    expectPublishedMethods(['initialMethod', 'newFirstMethod']);
  });

  it('unpublishes handlers removed from the feature list', () => {
    // Ported from testRemoveServerEventHandlerMethod.
    handlers().add(0, 'method1');
    handlers().add(1, 'method2');
    handlers().add(2, 'method3');
    bind(node, element);
    expectPublishedMethods(['method1', 'method2', 'method3']);

    handlers().splice(1, 2);
    expectPublishedMethods(['method1']);

    handlers().add(0, 'new1');
    handlers().add(2, 'new2');
    expectPublishedMethods(['new1', 'method1', 'new2']);

    handlers().splice(0, 1, ['foo', 'bar']);
    expectPublishedMethods(['foo', 'bar', 'method1', 'new2']);
  });

  // Ported from GwtMultipleBindingTest.testClientCallableMethodDoubleBind: a
  // second bind must not re-read the client-delegate-handlers feature.
  it('binding twice does not re-read the client-callable feature', () => {
    const guarded = new BindGuardStateNode(3, harness.tree, (m) => expect.fail(m));
    harness.tree.registerNode(guarded);
    guarded.getMap(NodeFeatures.ELEMENT_DATA);
    guarded.getList(NodeFeatures.CLIENT_DELEGATE_HANDLERS).add(0, 'foo');
    const guardedElement = document.createElement('div');

    bind(guarded, guardedElement);
    Reactive.flush();

    guarded.setBound();
    bind(guarded, guardedElement);
    Reactive.flush();
  });

  // Beyond the Java suite: the objectProvider/featureId overload has no GWT
  // counterpart, so it is exercised directly here.
  describe('beyond the Java suite', () => {
    it('uses the supplied object provider and feature id (no return promise)', () => {
      const list = fakeList(['foo']);
      const providerNode = fakeNode(list, 7);
      const server: Record<string, any> = {};
      bindServerEventHandlerNames(() => server, providerNode, 7, false);
      expect(typeof server.foo).to.equal('function');
      server.foo();
      // returnValue=false => no promise id reservation, just -1.
      expect(providerNode.sent).to.deep.equal([{ methodName: 'foo', promiseId: -1 }]);
    });
  });
});
