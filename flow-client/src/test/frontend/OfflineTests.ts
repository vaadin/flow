/* tslint:disable: no-unused-expression */
const { describe, it, beforeEach, afterEach, after } = intern.getPlugin('interface.bdd');
const { expect } = intern.getPlugin('chai');
const { fetchMock } = intern.getPlugin('fetchMock');
const { sinon } = intern.getPlugin('sinon');

import {
  ConnectClient,
} from "../../main/resources/META-INF/resources/frontend/Connect";
import {
  ConnectionState, ConnectionStateStore
} from "../../main/resources/META-INF/resources/frontend/ConnectionState";

import { openDB } from "idb";
import { DeferredCallSubmitter, OfflineHelper } from "../../main/resources/META-INF/resources/frontend/Offline";

const VAADIN_DEFERRED_CALL_QUEUE_DB_NAME = 'vaadin-deferred-call-queue';
const VAADIN_DEFERRED_CALL_STORE_NAME = 'deferredCalls';

// `connectClient.call` adds the host and context to the endpoint request.
// we need to add this origin when configuring fetch-mock
const base = window.location.origin;
const $wnd = window as any;
const offline = new OfflineHelper();
describe("Offline", () => {
  beforeEach(() => localStorage.clear());

  after(() => {
    // @ts-ignore
    delete window.Vaadin;
  });

  describe("Defer Request", () => {
    let client: ConnectClient;

    beforeEach(() => {
      client = new ConnectClient();
    });

    afterEach(() => {
      sinon.restore();
      fetchMock.restore();
    });

    it("Should return a DeferrableResult that retains request meta when invoking deferRequest offline", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTION_LOST) };
      sinon.stub(OfflineHelper.prototype, "storeDeferredCall").callsFake((deferredCall: any) => {
        if (!deferredCall.id) {
          deferredCall.id = 100;
        }
        return { isDeferred: true, deferredCall };
      });

      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(result.isDeferred).to.be.true;
      expect(result.deferredCall?.endpoint).to.equal('FooEndpoint');
      expect(result.deferredCall?.method).to.equal('fooMethod');
      expect(result.deferredCall?.params?.fooData).to.equal('foo');
    })

    it("Should cache the endpoint call when invoking deferRequest offline", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTION_LOST) };

      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      const db = await openDB(VAADIN_DEFERRED_CALL_QUEUE_DB_NAME);
      const cachedRequest = await db.get(VAADIN_DEFERRED_CALL_STORE_NAME, result.deferredCall?.id as number);

      expect(cachedRequest.endpoint).to.equal('FooEndpoint');
      expect(cachedRequest.method).to.equal('fooMethod');
      expect(cachedRequest.params?.fooData).to.equal('foo');

      await db.clear(VAADIN_DEFERRED_CALL_STORE_NAME);
      db.close();
    })

    it("Should not invoke the client.call method when invoking deferRequest offline", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTION_LOST) };
      sinon.stub(OfflineHelper.prototype, "storeDeferredCall");

      const callMethod = sinon.stub(client, "call");

      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(callMethod.called).to.be.false;
    })

    it("Should invoke the client.call method when invoking deferRequest online", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTED) };
      const callMethod = sinon.stub(client, "call");

      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(callMethod.called).to.be.true;
    })

    it("Should not invoke the client.storeDeferredCall method when invoking deferRequest online", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTED) };
      sinon.stub(client, "call");
      const storeDeferredCallMock = sinon.stub(OfflineHelper.prototype, "storeDeferredCall");

      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(storeDeferredCallMock.called).to.be.false;
    })

    it("should return false when checking the isDefered prooperty of the return value of invoking deferRequest method online", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTED) };
      sinon.stub(client, "call");
      sinon.stub(OfflineHelper.prototype, "storeDeferredCall");

      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(result.isDeferred).to.be.false;
    })

    it("should return undefined when checking the endpointRequest prooperty of the return value of invoking deferRequest method offline", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTED) };
      sinon.stub(client, "call");
      sinon.stub(OfflineHelper.prototype, "storeDeferredCall");

      const returnValue = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(returnValue.deferredCall).to.be.undefined;
    })

    it("should defer endpoint call when server is not reachable even though browser is online", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTED) };
      const storeDeferredCall = sinon.stub(OfflineHelper.prototype, "storeDeferredCall");
      fetchMock.post(
        base + '/connect/FooEndpoint/fooMethod',
        Promise.reject(new TypeError('Failed to fetch'))
      )
  
      await client.deferrableCall('FooEndpoint', 'fooMethod');
  
      expect(storeDeferredCall.called).to.be.true;
    })
  
    it("should NOT defer endpoint call when server return error", async () => {
      $wnd.Vaadin.Flow = { connectionState: new ConnectionStateStore(ConnectionState.CONNECTED) };
      sinon.stub(OfflineHelper.prototype, "storeDeferredCall");
      const body = 'Unexpected error';
      const errorResponse = new Response(
        body,
        {
          status: 500,
          statusText: 'Internal Server Error'
        }
      );
      fetchMock.post(base + '/connect/FooEndpoint/fooMethod', errorResponse);
  
      try {
        await client.deferrableCall('FooEndpoint', 'fooMethod');
      } catch (error) {
        expect(error).to.be.instanceOf(Error)
          .and.have.property('message').that.has.string('Unexpected error');
      }
    })
  });

  describe("submit deferred calls", () => {
    let client: ConnectClient;
    let requestCallStub: any;

    function fakeRequestCallFails() {
      requestCallStub.callsFake(() => {
        throw new Error();
      });
    }

    async function insertARequest(numberOfRequests = 1) {
      const db = await (offline as any).openOrCreateDB();
      for (let i = 0; i < numberOfRequests; i++) {
        await db.put(VAADIN_DEFERRED_CALL_STORE_NAME, { endpoint: 'FooEndpoint', method: 'fooMethod', params: { fooData: 'foo' } });
      }
      expect(await db.count(VAADIN_DEFERRED_CALL_STORE_NAME)).to.equal(numberOfRequests);
      db.close();
    }

    async function verifyNumberOfRequsetsInTheQueue(numberOfRequests = 1) {
      const db = await (offline as any).openOrCreateDB();
      expect(await db.count(VAADIN_DEFERRED_CALL_STORE_NAME)).to.equal(numberOfRequests);
      db.close();
    }

    beforeEach(async () => {
      client = new ConnectClient();
      requestCallStub = sinon.stub(client, 'requestCall').callsFake(async () => {
        await new Promise(resolve => setTimeout(resolve, 10))
      });
    });

    afterEach(async () => {
      const db = await (offline as any).openOrCreateDB();
      await db.clear(VAADIN_DEFERRED_CALL_STORE_NAME);
      db.close();
      fetchMock.restore();
    });

    it("should check and submit the cached requests when receiving online event", () => {
      const submitMethod = sinon.stub(ConnectClient.prototype, "submitDeferredCalls");
      client = new ConnectClient();
      self.dispatchEvent(new Event('online'));
      expect(submitMethod.called).to.be.true;
      submitMethod.restore();
    })

    it("should submit the cached request when receiving online event", async () => {
      await insertARequest(3);

      await client.submitDeferredCalls();

      await verifyNumberOfRequsetsInTheQueue(0);
    })

    it("should keep the request if submission fails", async () => {
      await insertARequest();

      fakeRequestCallFails();

      try {
        await client.submitDeferredCalls();
      } catch (_) {
        // expected
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);
      }
    });

    it('should reject if submission fails', async () => {
      await insertARequest();

      fakeRequestCallFails();

      let errors: Error[] | undefined;

      try {
        await client.submitDeferredCalls();
      } catch (e) {
        // expected
        errors = e;
      }

      expect(errors?.length).to.equal(1);
    });

    it("should be able to resubmit cached request that was failed to submit", async () => {
      await insertARequest();

      fakeRequestCallFails();

      try {
        await client.submitDeferredCalls();
      } catch (_) {
        // expected
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);

        requestCallStub.restore();
        sinon.stub(client, "requestCall");

        await client.submitDeferredCalls();

        await verifyNumberOfRequsetsInTheQueue(0);
      }
    });

    it("should only submit once when receiving multiple online events", async () => {
      await insertARequest();

      await Promise.all([
        client.submitDeferredCalls(),
        client.submitDeferredCalls(),
        client.submitDeferredCalls()
      ])

      expect(requestCallStub.calledOnce).to.be.true;
    })

    it("should only submit once when receiving multiple online events after a failed submission", async () => {
      await insertARequest();

      fakeRequestCallFails();

      try {
        await client.submitDeferredCalls();
      } catch (_) {
        // expected
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);

        requestCallStub.restore();
        sinon.stub(client, "requestCall");

        await Promise.all([
          client.submitDeferredCalls(),
          client.submitDeferredCalls(),
          client.submitDeferredCalls()
        ])

        expect(requestCallStub.calledOnce).to.be.true;
      }
    });

    it('should invoke middleware with isDeferred context', async () => {
      fetchMock.post(base + '/connect/FooEndpoint/fooMethod', { fooData: 'foo' });

      requestCallStub.restore();

      const spyMiddleware = sinon.spy(async (context: any, next?: any) => {
        expect(context.endpoint).to.equal('FooEndpoint');
        expect(context.method).to.equal('fooMethod');
        expect(context.params).to.deep.equal({ fooData: 'foo' });
        expect(context.request).to.be.instanceOf(Request);
        expect(context.isDeferred).to.be.true;
        return next(context);
      });
      client.middlewares = [spyMiddleware];

      try {
        await insertARequest();

        expect(spyMiddleware.called).to.be.false;

        await client.submitDeferredCalls();

        expect(spyMiddleware.called).to.be.true;
      } finally {
        fetchMock.restore();
      }
    });

    it('should invoke deferredCallHandler', async () => {
      await insertARequest();

      const onDeferredCallStub = sinon.stub().resolves();
      client.deferredCallHandler = {
        handleDeferredCallSubmission: onDeferredCallStub
      };

      await client.submitDeferredCalls();

      expect(onDeferredCallStub.callCount).to.equal(1);
      const [call] = onDeferredCallStub.getCall(0).args;
      expect(call.deferredCall.endpoint).to.equal('FooEndpoint');
      expect(call.deferredCall.method).to.equal('fooMethod');
      expect(call.deferredCall.params).to.deep.equal({ fooData: 'foo' });

      await verifyNumberOfRequsetsInTheQueue(0);
    });

    it('should reject if onDeferredCall callback rejects', async () => {
      const onDeferredCallStub = sinon.stub().rejects();
      client.deferredCallHandler = {
        handleDeferredCallSubmission: onDeferredCallStub
      };

      let errors: Error[] | undefined;

      try {
        await insertARequest();
        await client.submitDeferredCalls();
      } catch (e) {
        // expected
        errors = e;
      } finally {
        expect(errors?.length).to.equal(1);
      }
    });

    it('should keep request in the queue when onDeferredCall callback rejects', async () => {
      const onDeferredCallStub = sinon.stub().rejects();
      client.deferredCallHandler = {
        handleDeferredCallSubmission: onDeferredCallStub
      };;
  
      try {
        await insertARequest();
        await client.submitDeferredCalls();
      } catch(_) {
        // expected
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);
      }
    });

    it('should set submitting status to false for all the request in the queue', async () => {
      try {
        await insertARequest(2);
        fakeRequestCallFails();
        await client.submitDeferredCalls();
      } catch (e) {
        // expected
      } finally {
        verifyNumberOfRequsetsInTheQueue(2)
        const db = await openDB(VAADIN_DEFERRED_CALL_QUEUE_DB_NAME);
        let cursor = await db.transaction(VAADIN_DEFERRED_CALL_STORE_NAME).store.openCursor();
        while (cursor) {
          expect(cursor.value.submitting).to.be.false;
          cursor = await cursor.continue();
        }
        await db.clear(VAADIN_DEFERRED_CALL_STORE_NAME);
        db.close;
      }
    });

    it('should submit all the endpoint calls in the queue even errors are encountered when submitting some calls', async () => {
      try {
        await insertARequest(3);
        fakeRequestCallFails();
        await client.submitDeferredCalls();
      } catch (e) {
        // expected
      } finally {
        expect(requestCallStub.callCount).to.equal(3);
        verifyNumberOfRequsetsInTheQueue(3)
      }
    });

    describe('deferred call handler', () => {
      it('should be able to show a notification when a deferred call submission succeeds', async () => {
        const notifyOnSucess = sinon.stub();
        client.deferredCallHandler = {
          async handleDeferredCallSubmission(deferrableCall: DeferredCallSubmitter) {
            await deferrableCall.submit();
            notifyOnSucess();
          }
        };

        try {
          await insertARequest();
          await client.submitDeferredCalls();
        } finally {
          expect(notifyOnSucess.calledOnce).to.be.true;
        }
      });

      it('should be able to show a notification when deferred call submission fails', async () => {
        const notifyOnFailure = sinon.stub();
        client.deferredCallHandler = {
          async handleDeferredCallSubmission(deferrableCall: DeferredCallSubmitter) {
            try {
              await deferrableCall.submit();
            } catch (error) {
              notifyOnFailure();
            }
          }
        };

        try {
          await insertARequest();
          fakeRequestCallFails()
          await client.submitDeferredCalls();
        } finally {
          expect(notifyOnFailure.calledOnce).to.be.true;
        }
      });

      it('should remove a succeeded endpoint call from the queue by default', async () => {
        client.deferredCallHandler = {
          async handleDeferredCallSubmission(deferrableCall: DeferredCallSubmitter) {
            await deferrableCall.submit();
          }
        };

        try {
          await insertARequest();
          await client.submitDeferredCalls();
        } finally {
          await verifyNumberOfRequsetsInTheQueue(0);
        }
      });

      it('should keep a failed endpoint in the queue by default', async () => {
        client.deferredCallHandler = {
          async handleDeferredCallSubmission(deferrableCall: DeferredCallSubmitter) {
            await deferrableCall.submit();
          }
        };

        try {
          await insertARequest();
          fakeRequestCallFails();
          await client.submitDeferredCalls();
        } catch (_) {
          // expected
        } finally {
          await verifyNumberOfRequsetsInTheQueue(1);
        }
      });

      it('should remove a failed endpoint call from the queue when user catches the error without calling deferredCallSubmitter.keepInTheQueue()', async () => {
        client.deferredCallHandler = {
          async handleDeferredCallSubmission(deferredCallSubmitter: DeferredCallSubmitter) {
            try {
              await deferredCallSubmitter.submit();
            } catch (error) {
              // swallow the error
            }
          }
        };

        try {
          await insertARequest();
          fakeRequestCallFails();
          await client.submitDeferredCalls();
        } finally {
          await verifyNumberOfRequsetsInTheQueue(0);
        }
      });

      it('should keep a failed endpoint call in the queue when user catches the error and calls deferredCallSubmitter.keepInTheQueue()', async () => {
        client.deferredCallHandler = {
          async handleDeferredCallSubmission(deferredCallSubmitter: DeferredCallSubmitter) {
            try {
              await deferredCallSubmitter.submit();
            } catch (error) {
              deferredCallSubmitter.keepDeferredCallInTheQueue();
            }
          }
        };

        try {
          await insertARequest();
          fakeRequestCallFails();
          await client.submitDeferredCalls();
        } finally {
          await verifyNumberOfRequsetsInTheQueue(1);
        }
      });

    });
  });
});
