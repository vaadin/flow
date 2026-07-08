import { expect } from '@open-wc/testing';
import {
  defineMethod,
  get,
  getEventData,
  getIfPresent,
  getMethods,
  getOrCreateExpression,
  getPolymerPropertyObject,
  initPromiseHandler,
  rejectPromises,
  removeMethod
} from '../../main/frontend/internal/ServerEventObject';

// com.vaadin.flow.shared.JsonConstants.RPC_PROMISE_CALLBACK_NAME
const NAME = '}p';

// com.vaadin.flow.internal.nodefeature.NodeFeatures.POLYMER_EVENT_LISTENERS
const POLYMER_EVENT_LISTENERS = 18;

// A minimal StateNode/StateTree stand-in for the defineMethod/getEventData
// contracts: a single feature map keyed by feature id, a constant pool, and a
// recorder for the template events sent to the server.
function fakeNode(
  listeners: Record<string, { value: unknown }> = {},
  constants: Record<string, unknown> = {},
  domNode: unknown = null
) {
  const sent: Array<{ methodName: string; args: unknown[]; promiseId: number }> = [];
  const map = {
    hasPropertyValue: (name: string) => name in listeners,
    getProperty: (name: string) => ({ getValue: () => listeners[name].value })
  };
  const node: any = {
    sent,
    getDomNode: () => domNode,
    getMap: (id: number) => (id === POLYMER_EVENT_LISTENERS ? map : { hasPropertyValue: () => false }),
    getTree: () => ({
      getRegistry: () => ({ getConstantPool: () => ({ get: (key: string) => constants[key] }) }),
      sendTemplateEventToServer: (_n: unknown, methodName: string, args: unknown[], promiseId: number) =>
        sent.push({ methodName, args, promiseId })
    })
  };
  return node;
}

describe('ServerEventObject', () => {
  it('initPromiseHandler installs a non-enumerable promise callback with an empty promise list', () => {
    const server: Record<string, any> = {};
    initPromiseHandler(server as any, NAME);
    expect(typeof server[NAME]).to.equal('function');
    expect(server[NAME].promises).to.deep.equal([]);
    // Non-enumerable so it is not reported as a server method.
    expect(Object.keys(server)).to.deep.equal([]);
  });

  it('the promise callback resolves or rejects the stored promise and clears it', () => {
    const server: Record<string, any> = {};
    initPromiseHandler(server as any, NAME);
    const resolved: unknown[] = [];
    const rejected: unknown[] = [];
    server[NAME].promises[0] = [(v: unknown) => resolved.push(v), (e: unknown) => rejected.push(e)];
    server[NAME](0, true, 'ok');
    expect(resolved).to.deep.equal(['ok']);
    expect(server[NAME].promises[0]).to.equal(undefined);

    server[NAME].promises[1] = [(v: unknown) => resolved.push(v), (e: unknown) => rejected.push(e)];
    server[NAME](1, false, null);
    expect(rejected).to.have.length(1);
    expect((rejected[0] as Error).message).to.contain('Something went wrong');

    // A missing promise id is ignored (node recreated after scheduling).
    server[NAME](99, true, 'x');
    expect(resolved).to.deep.equal(['ok']);
  });

  it('removeMethod deletes the named method', () => {
    const server: Record<string, any> = { doIt: () => {} };
    removeMethod(server as any, 'doIt');
    expect('doIt' in server).to.be.false;
  });

  it('getMethods returns the own enumerable keys', () => {
    const server: Record<string, any> = {};
    initPromiseHandler(server as any, NAME);
    server.foo = () => {};
    server.bar = () => {};
    expect(getMethods(server as any)).to.deep.equal(['foo', 'bar']);
  });

  it('rejectPromises rejects every pending promise', () => {
    const server: Record<string, any> = {};
    initPromiseHandler(server as any, NAME);
    const rejected: string[] = [];
    server[NAME].promises[0] = [() => {}, (e: Error) => rejected.push(e.message)];
    server[NAME].promises[1] = [() => {}, (e: Error) => rejected.push(e.message)];
    rejectPromises(server as any, NAME);
    expect(rejected).to.deep.equal(['Client is resynchronizing', 'Client is resynchronizing']);
  });

  it('getPolymerPropertyObject wraps a model object carrying a nodeId, else null', () => {
    const withNodeId = { get: (path: string) => (path === 'item' ? { nodeId: 7, foo: 'x' } : null) };
    expect(getPolymerPropertyObject(withNodeId, 'item')).to.deep.equal({ nodeId: 7 });

    const noNodeId = { get: () => ({ foo: 'x' }) };
    expect(getPolymerPropertyObject(noNodeId, 'item')).to.equal(null);

    // No get method => null.
    expect(getPolymerPropertyObject({}, 'item')).to.equal(null);
  });

  it('getIfPresent returns the $server object or null', () => {
    const element = { $server: { foo: 1 } } as any;
    expect(getIfPresent(element)).to.equal(element.$server);
    expect(getIfPresent({} as any)).to.equal(null);
  });

  it('get creates and installs a $server object with a promise handler, reusing an existing one', () => {
    const element = {} as any;
    const server = get(element);
    expect(element.$server).to.equal(server);
    expect(typeof server[NAME]).to.equal('function');
    expect(server[NAME].promises).to.deep.equal([]);
    // The promise handler is non-enumerable, so no spurious methods are reported.
    expect(getMethods(server)).to.deep.equal([]);
    // A second call returns the same object.
    expect(get(element)).to.equal(server);
  });

  it('getOrCreateExpression compiles and caches an (event, element) function', () => {
    const expr = getOrCreateExpression('event.detail + element.offset');
    expect(expr({ detail: 5 } as any, { offset: 3 })).to.equal(8);
    // Same string => same cached function instance.
    expect(getOrCreateExpression('event.detail + element.offset')).to.equal(expr);
  });

  it('defineMethod with no server-defined data sends all call arguments and no promise', () => {
    const node = fakeNode();
    const server: Record<string, any> = {};
    defineMethod(server, 'doIt', node, false);

    const result = server.doIt('a', 'b');
    expect(result).to.equal(undefined);
    expect(node.sent).to.have.length(1);
    expect(node.sent[0]).to.deep.equal({ methodName: 'doIt', args: ['a', 'b'], promiseId: -1 });
  });

  it('defineMethod runs an existing prototype method before sending', () => {
    const calls: string[] = [];
    const proto = { doIt: () => calls.push('proto') };
    const server: Record<string, any> = Object.create(proto);
    defineMethod(server, 'doIt', fakeNode(), false);
    server.doIt();
    expect(calls).to.deep.equal(['proto']);
  });

  it('defineMethod with returnPromise returns a promise the server callback settles', async () => {
    const node = fakeNode();
    const server: Record<string, any> = {};
    initPromiseHandler(server, NAME);
    defineMethod(server, 'doIt', node, true);

    const promise = server.doIt() as Promise<unknown>;
    expect(node.sent[0].promiseId).to.equal(0);
    // The server settles the stored promise by its id.
    server[NAME](0, true, 'result');
    expect(await promise).to.equal('result');
  });

  it('defineMethod sends the server-requested event data when listeners are defined', () => {
    // The method has a listener whose constant key maps to two expressions: one
    // event-based and one model property (resolved to a node id).
    const node = fakeNode({ doIt: { value: 'key' } }, { key: ['event.detail', 'event.model.item'] });
    const server: Record<string, any> = {};
    defineMethod(server, 'doIt', node, false);

    server.doIt({ detail: 42, model: { item: { nodeId: 9 } } });
    expect(node.sent[0].args).to.deep.equal([42, { nodeId: 9 }]);
  });

  it('getEventData returns null when no listener is defined for the method', () => {
    expect(getEventData({}, {} as any, 'missing', fakeNode())).to.equal(null);
  });

  it('getEventData reads a non-event expression as a Polymer property from the DOM node', () => {
    const domNode = { get: (path: string) => (path === 'foo' ? { nodeId: 3 } : null) };
    const node = fakeNode({ doIt: { value: 'key' } }, { key: ['foo'] }, domNode);
    expect(getEventData({}, {} as any, 'doIt', node)).to.deep.equal([{ nodeId: 3 }]);
  });
});
