import { expect } from '@open-wc/testing';
import { XhrConnection } from '../../main/frontend/internal/XhrConnection';

function makeRegistry() {
  const calls: string[] = [];
  let handled: unknown = undefined;
  const registry: any = {
    calls,
    getHandled: () => handled,
    getRequestResponseTracker: () => ({ addResponseHandlingEndedHandler: () => {} }),
    getConnectionStateHandler: () => ({
      xhrInvalidStatusCode: () => calls.push('invalidStatus'),
      xhrException: () => calls.push('exception'),
      xhrInvalidContent: () => calls.push('invalidContent'),
      xhrOk: () => calls.push('ok')
    }),
    getMessageHandler: () => ({
      handleMessage: (json: unknown) => {
        handled = json;
        calls.push('handled');
      }
    }),
    getApplicationConfiguration: () => ({ getServiceUrl: () => '/app', getUIId: () => 7 })
  };
  return registry;
}

describe('XhrConnection (class)', () => {
  it('builds the UIDL request URI from the configuration', () => {
    const connection = new XhrConnection(makeRegistry());
    expect(connection.getUri()).to.equal('/app?v-r=uidl&v-uiId=7');
  });

  it('routes a valid 200 response to the message handler', () => {
    const registry = makeRegistry();
    const connection = new XhrConnection(registry);
    connection.onResponseSuccess({ responseText: '{"syncId":3}' } as any, { rpc: [] });
    expect(registry.calls).to.deep.equal(['ok', 'handled']);
    expect(registry.getHandled()).to.deep.equal({ syncId: 3 });
  });

  it('reports invalid content when the response is not JSON', () => {
    const registry = makeRegistry();
    const connection = new XhrConnection(registry);
    connection.onResponseSuccess({ responseText: 'not json' } as any, { rpc: [] });
    expect(registry.calls).to.deep.equal(['invalidContent']);
  });

  it('routes an invalid status code (no exception) to xhrInvalidStatusCode', () => {
    const registry = makeRegistry();
    const connection = new XhrConnection(registry);
    connection.onResponseFail({} as any, { rpc: [] }, null);
    expect(registry.calls).to.deep.equal(['invalidStatus']);
  });

  it('routes a network exception to xhrException', () => {
    const registry = makeRegistry();
    const connection = new XhrConnection(registry);
    connection.onResponseFail({} as any, { rpc: [] }, new Error('boom'));
    expect(registry.calls).to.deep.equal(['exception']);
  });
});
