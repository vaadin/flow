import { expect } from '@open-wc/testing';
import { bindServerEventHandlerNames } from '../../main/frontend/internal/binding/ServerEventHandlerBinder';

// com.vaadin.flow.internal.nodefeature.NodeFeatures.CLIENT_DELEGATE_HANDLERS
const CLIENT_DELEGATE_HANDLERS = 19;

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
function fakeNode(list: ReturnType<typeof fakeList>, featureId = CLIENT_DELEGATE_HANDLERS): any {
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

describe('ServerEventHandlerBinder', () => {
  it('defines the existing handler names on the element $server object', () => {
    const node = fakeNode(fakeList(['foo', 'bar']));
    const element = {} as any;
    bindServerEventHandlerNames(element, node);
    expect(typeof element.$server.foo).to.equal('function');
    expect(typeof element.$server.bar).to.equal('function');
  });

  it('defines a method that sends a template event to the server', () => {
    const node = fakeNode(fakeList(['foo']));
    const element = {} as any;
    bindServerEventHandlerNames(element, node);
    // The element overload binds with returnValue=true, so get() installs the
    // promise handler and the call reserves promise id 0.
    element.$server.foo();
    expect(node.sent).to.deep.equal([{ methodName: 'foo', promiseId: 0 }]);
  });

  it('adds and removes methods as the feature list is spliced', () => {
    const list = fakeList(['foo']);
    const node = fakeNode(list);
    const element = {} as any;
    bindServerEventHandlerNames(element, node);

    list.fireSplice([], ['bar']);
    expect(typeof element.$server.bar).to.equal('function');

    list.fireSplice(['foo'], []);
    expect('foo' in element.$server).to.be.false;
  });

  it('defines nothing for an empty list but still tracks later additions', () => {
    const list = fakeList([]);
    const node = fakeNode(list);
    const element = {} as any;
    bindServerEventHandlerNames(element, node);
    // get() is only called lazily, so an empty list creates no $server yet.
    expect(element.$server).to.equal(undefined);

    list.fireSplice([], ['baz']);
    expect(typeof element.$server.baz).to.equal('function');
  });

  it('returns an EventRemover that detaches the splice listener', () => {
    const list = fakeList(['foo']);
    const node = fakeNode(list);
    const element = {} as any;
    const remover = bindServerEventHandlerNames(element, node);
    remover.remove();
    // After removal a splice no longer defines methods.
    list.fireSplice([], ['bar']);
    expect('bar' in element.$server).to.be.false;
  });

  it('uses the supplied object provider and feature id (no return promise)', () => {
    const list = fakeList(['foo']);
    const node = fakeNode(list, 7);
    const server: Record<string, any> = {};
    bindServerEventHandlerNames(() => server, node, 7, false);
    expect(typeof server.foo).to.equal('function');
    server.foo();
    // returnValue=false => no promise id reservation, just -1.
    expect(node.sent).to.deep.equal([{ methodName: 'foo', promiseId: -1 }]);
  });
});
