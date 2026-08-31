// Beyond the Java suite: XhrConnection has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.
import { expect } from '@open-wc/testing';
import {
  XhrConnection,
  XhrResponseHandler
} from '../../../../../main/frontend/internal/client/communication/XhrConnection';

function makeRegistry() {
  const calls: string[] = [];
  const endedHandlers: Array<() => void> = [];
  let handled: unknown = undefined;
  const registry: any = {
    calls,
    endedHandlers,
    getHandled: () => handled,
    getRequestResponseTracker: () => ({
      addResponseHandlingEndedHandler: (handler: () => void) => {
        endedHandlers.push(handler);
      }
    }),
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
      function fakeXhr(behaviour: { throwOnSend?: boolean; status?: number; stayOpened?: boolean }) {
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
            if (behaviour.stayOpened === true) {
              // OPENED: the request was never actually dispatched.
              xhr.readyState = 1;
              return;
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

      it('re-sends a request WebKit may have ignored during navigation', async () => {
        // A request that never leaves readyState OPENED is re-sent every 250 ms
        // while the beforeunload flag is set, which is the browser bug this
        // retry loop works around. The test browser reports the WebKit engine,
        // so the branch is reachable.
        const registry = makeRegistry();
        const fake = fakeXhr({ stayOpened: true });
        const connection = new XhrConnection(registry);
        // beforeunload sets the flag that arms the retry.
        window.dispatchEvent(new Event('beforeunload'));

        withFakeXhr(fake, () => connection.send({ rpc: [] }));
        expect(fake.state.sent).to.equal(1);

        await new Promise((resolve) => {
          setTimeout(resolve, 400);
        });
        expect(fake.state.sent).to.be.greaterThan(1);

        // Ending the response handling clears the flag, which stops the loop.
        // The retry already scheduled still re-sends once — it checks the flag
        // only after resending, as Java does — and no further one is scheduled.
        registry.endedHandlers.forEach((handler: () => void) => handler());
        await new Promise((resolve) => {
          setTimeout(resolve, 400);
        });
        const sentWhenStopped = fake.state.sent;
        await new Promise((resolve) => {
          setTimeout(resolve, 400);
        });
        expect(fake.state.sent).to.equal(sentWhenStopped);
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
