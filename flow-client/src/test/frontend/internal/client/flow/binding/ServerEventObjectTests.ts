import { expect } from '@open-wc/testing';
import {
  defineMethod,
  get,
  getIfPresent,
  getMethods,
  rejectPromises,
  removeMethod
} from '../../../../../../main/frontend/internal/client/flow/binding/ServerEventObject';

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
  it('the promise callback resolves or rejects the stored promise and clears it', () => {
    const server: Record<string, any> = get({} as any);
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
    const server: Record<string, any> = get({} as any);
    server.foo = () => {};
    server.bar = () => {};
    expect(getMethods(server as any)).to.deep.equal(['foo', 'bar']);
  });

  it('rejectPromises rejects every pending promise', () => {
    const server: Record<string, any> = get({} as any);
    const rejected: string[] = [];
    server[NAME].promises[0] = [() => {}, (e: Error) => rejected.push(e.message)];
    server[NAME].promises[1] = [() => {}, (e: Error) => rejected.push(e.message)];
    rejectPromises(server as any, NAME);
    expect(rejected).to.deep.equal(['Client is resynchronizing', 'Client is resynchronizing']);
  });

  it('sends only the node id of a model object, and null when it has none', () => {
    // The DOM node exposes the model object via its Polymer get(path).
    const withNodeId = { get: (path: string) => (path === 'foo' ? { nodeId: 7, bar: 'x' } : null) };
    const node = fakeNode({ doIt: { value: 'key' } }, { key: ['foo'] }, withNodeId);
    const server: Record<string, any> = {};
    defineMethod(server, 'doIt', node, false);

    server.doIt({});
    expect(node.sent[0].args).to.deep.equal([{ nodeId: 7 }]);

    // A model object without a node id is sent as null.
    const noNodeId = fakeNode({ doIt: { value: 'key' } }, { key: ['foo'] }, { get: () => ({ bar: 'x' }) });
    const other: Record<string, any> = {};
    defineMethod(other, 'doIt', noNodeId, false);

    other.doIt({});
    expect(noNodeId.sent[0].args).to.deep.equal([null]);
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
    const server: Record<string, any> = get({} as any);
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

  it('evaluates an event expression against the event and the server object', () => {
    // An event expression is compiled to an (event, element) function, where
    // the element is the object the method is defined on.
    const node = fakeNode({ doIt: { value: 'key' } }, { key: ['event.detail + element.offset'] });
    const server: Record<string, any> = { offset: 3 };
    defineMethod(server, 'doIt', node, false);

    server.doIt({ detail: 5 });
    expect(node.sent[0].args).to.deep.equal([8]);
  });
});
