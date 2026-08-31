import { expect } from '@open-wc/testing';
import {
  XhrConnection,
  XhrResponseHandler
} from '../../../../../main/frontend/internal/client/communication/XhrConnection';

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

describe('XhrConnection', () => {
  describe('class', () => {
    it('builds the UIDL request URI from the configuration', () => {
      const connection = new XhrConnection(makeRegistry());
      expect(connection.getUri()).to.equal('/app?v-r=uidl&v-uiId=7');
    });

    it('routes a valid 200 response to the message handler', () => {
      const registry = makeRegistry();
      const handler = new XhrResponseHandler(registry);
      handler.setPayload({ rpc: [] });
      handler.onSuccess({ responseText: '{"syncId":3}' } as any);
      expect(registry.calls).to.deep.equal(['ok', 'handled']);
      expect(registry.getHandled()).to.deep.equal({ syncId: 3 });
    });

    it('reports invalid content when the response is not JSON', () => {
      const registry = makeRegistry();
      const handler = new XhrResponseHandler(registry);
      handler.setPayload({ rpc: [] });
      handler.onSuccess({ responseText: 'not json' } as any);
      expect(registry.calls).to.deep.equal(['invalidContent']);
    });

    it('routes an invalid status code (no exception) to xhrInvalidStatusCode', () => {
      const registry = makeRegistry();
      const handler = new XhrResponseHandler(registry);
      handler.setPayload({ rpc: [] });
      handler.onFail({} as any, null);
      expect(registry.calls).to.deep.equal(['invalidStatus']);
    });

    it('routes a network exception to xhrException', () => {
      const registry = makeRegistry();
      const handler = new XhrResponseHandler(registry);
      handler.setPayload({ rpc: [] });
      handler.onFail({} as any, new Error('boom'));
      expect(registry.calls).to.deep.equal(['exception']);
    });

    describe('beyond the Java suite', () => {
      // A fake XMLHttpRequest standing in for the real one, so send() can be
      // driven without a server. It records whether an "error" listener was
      // registered, which is what would report a network failure twice.
      function fakeXhr(behaviour: { throwOnSend?: boolean; status?: number }) {
        const state = { errorListener: false, opened: '', sent: 0 };
        const xhr = {
          readyState: 0,
          status: 0,
          responseText: '',
          withCredentials: false,
          onreadystatechange: null as (() => void) | null,
          set onerror(_listener: unknown) {
            state.errorListener = true;
          },
          open: (_method: string, url: string) => {
            state.opened = url;
          },
          setRequestHeader: () => {},
          send: () => {
            state.sent += 1;
            if (behaviour.throwOnSend === true) {
              throw new Error('blocked by the browser');
            }
            xhr.readyState = 4;
            xhr.status = behaviour.status ?? 0;
            xhr.onreadystatechange?.();
            // A network failure fires "error" after the DONE ready state; the
            // connection must not be listening for it.
            if (state.errorListener) {
              throw new Error('the error event would report the failure twice');
            }
          }
        };
        return { xhr, state };
      }

      function withFakeXhr<T>(fake: ReturnType<typeof fakeXhr>, run: () => T): T {
        const original = window.XMLHttpRequest;
        const stub = function XhrStub() {
          return fake.xhr;
        };
        // The code under test compares against XMLHttpRequest.DONE.
        (stub as unknown as { DONE: number }).DONE = 4;
        (window as unknown as { XMLHttpRequest: unknown }).XMLHttpRequest = stub;
        try {
          return run();
        } finally {
          (window as unknown as { XMLHttpRequest: unknown }).XMLHttpRequest = original;
        }
      }

      it('reports a failed request once, from the ready-state handler only', () => {
        const registry = makeRegistry();
        const fake = fakeXhr({ status: 0 });
        withFakeXhr(fake, () => new XhrConnection(registry).send({ rpc: [] }));
        expect(registry.calls).to.deep.equal(['invalidStatus']);
        expect(fake.state.errorListener).to.be.false;
      });

      it('reports a synchronous send failure as an exception', () => {
        const registry = makeRegistry();
        const fake = fakeXhr({ throwOnSend: true });
        withFakeXhr(fake, () => new XhrConnection(registry).send({ rpc: [] }));
        expect(registry.calls).to.deep.equal(['exception']);
      });

      it('refuses to send a payload holding a dom node reference', () => {
        const registry = makeRegistry();
        const fake = fakeXhr({ status: 200 });
        expect(() =>
          withFakeXhr(fake, () => new XhrConnection(registry).send({ rpc: [document.createElement('div')] }))
        ).to.throw(/dom node reference/);
        expect(fake.state.sent).to.equal(0);
      });
    });
  });
});
