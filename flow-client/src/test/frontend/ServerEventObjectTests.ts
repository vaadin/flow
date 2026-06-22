import { expect } from '@open-wc/testing';
import {
  getMethods,
  getPolymerPropertyObject,
  initPromiseHandler,
  rejectPromises,
  removeMethod
} from '../../main/frontend/internal/ServerEventObject';

const NAME = '$$rpcPromiseCallback';

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
});
