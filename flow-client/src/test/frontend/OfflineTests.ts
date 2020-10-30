/* tslint:disable: no-unused-expression */
const {describe, it, beforeEach, afterEach, after} = intern.getPlugin('interface.bdd');
const {expect} = intern.getPlugin('chai');
const {fetchMock} = intern.getPlugin('fetchMock');
const {sinon} = intern.getPlugin('sinon');

import {
  ConnectClient,
} from "../../main/resources/META-INF/resources/frontend/Connect";

import {openDB} from "idb";
import {offline} from "../../main/resources/META-INF/resources/frontend/Offline";

// `connectClient.call` adds the host and context to the endpoint request.
// we need to add this origin when configuring fetch-mock
const base = window.location.origin;
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
  
    afterEach(() => sinon.restore());
  
    it("Should return a DeferrableResult that retains request meta when invoking deferRequest offline", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => false);
      sinon.stub(offline, "cacheEndpointRequest").callsFake((request:any) => {
        if (!request.id) {
          request.id = 100;
        }
        return request;
      });
  
      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      expect(result.isDeferred).to.be.true;
      expect(result.endpointRequest?.endpoint).to.equal('FooEndpoint');
      expect(result.endpointRequest?.method).to.equal('fooMethod');
      expect(result.endpointRequest?.params?.fooData).to.equal('foo');
    })
  
    it("Should cache the endpoint request when invoking deferRequest offline", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => false);
  
      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      const db = await openDB('request-queue');
      const cachedRequest = await db.get('requests', result.endpointRequest?.id as number);
  
      expect(cachedRequest.endpoint).to.equal('FooEndpoint');
      expect(cachedRequest.method).to.equal('fooMethod');
      expect(cachedRequest.params?.fooData).to.equal('foo');
  
      await db.clear('requests');
      db.close();
    })
  
    it("Should not invoke the client.call method when invoking deferRequest offline", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => false);
      sinon.stub(offline, "cacheEndpointRequest");
  
      const callMethod = sinon.stub(client, "call");
  
      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      expect(callMethod.called).to.be.false;
    })
  
    it("should return true when checking the isDefered prooperty of the return value of invoking deferRequest method offline", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => false);
      sinon.stub(client, "call");
      sinon.stub(offline, "cacheEndpointRequest");
  
      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      expect(result.isDeferred).to.be.true;
    })
  
    it("should return undefined when checking the result prooperty of the return value of invoking deferRequest method offline", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => false);
      sinon.stub(client, "call");
      sinon.stub(offline, "cacheEndpointRequest");
  
      const returnValue = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      expect(returnValue.result).to.be.undefined;
    })
  
    it("Should invoke the client.call method when invoking deferRequest online", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => true);
      const callMethod = sinon.stub(client, "call");
  
      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      expect(callMethod.called).to.be.true;
    })
  
    it("Should not invoke the client.cacheEndpointRequest method when invoking deferRequest online", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => true);
      sinon.stub(client, "call");
      const cacheEndpointRequestMock = sinon.stub(offline, "cacheEndpointRequest");
  
      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      expect(cacheEndpointRequestMock.called).to.be.false;
    })
  
    it("should return false when checking the isDefered prooperty of the return value of invoking deferRequest method online", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => true);
      sinon.stub(client, "call");
      sinon.stub(offline, "cacheEndpointRequest");
  
      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      expect(result.isDeferred).to.be.false;
    })
  
    it("should return undefined when checking the endpointRequest prooperty of the return value of invoking deferRequest method offline", async () => {
      sinon.stub(offline, "checkOnline").callsFake(() => true);
      sinon.stub(client, "call");
      sinon.stub(offline, "cacheEndpointRequest");
  
      const returnValue = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });
  
      expect(returnValue.endpointRequest).to.be.undefined;
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
  
    async function insertARequest(numberOfRequests=1) {
      const db = await offline.openOrCreateDB();
      for(let i=0; i<numberOfRequests; i++){
        await db.put('requests', {endpoint: 'FooEndpoint', method:'fooMethod', params:{ fooData: 'foo' }});
      }
      expect(await db.count('requests')).to.equal(numberOfRequests);
      db.close();
    }
  
    async function verifyNumberOfRequsetsInTheQueue(numberOfRequests=1) {
      const db = await offline.openOrCreateDB();
      expect(await db.count('requests')).to.equal(numberOfRequests);
      db.close();
    }
  
    beforeEach(async () => {
      client = new ConnectClient();
      requestCallStub = sinon.stub(client, 'requestCall').callsFake(async () => {
        await new Promise(resolve => setTimeout(resolve, 10))
      });
    });
  
    afterEach(async () => {
      const db = await offline.openOrCreateDB();
      await db.clear('requests');
      db.close();
    });
  
    it("should check and submit the cached requests when receiving online event", () => {
      const submitMethod = sinon.stub(ConnectClient.prototype, "processDeferredCalls");
      client = new ConnectClient();
      self.dispatchEvent(new Event('online'));
      expect(submitMethod.called).to.be.true;
      submitMethod.restore();
    })
  
    it("should submit the cached request when receiving online event", async () => {
      await insertARequest(3);
  
      debugger;
      await client.processDeferredCalls();
  
      await verifyNumberOfRequsetsInTheQueue(0);
    })
  
    it("should keep the request if submission fails", async () => {
      await insertARequest();
  
      fakeRequestCallFails();
  
      try {
        await client.processDeferredCalls();
      } catch (_) {
        // expected
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);
      }
    });
  
    it('should reject if submission fails', async () => {
      await insertARequest();
  
      fakeRequestCallFails();
  
      let error: Error | undefined;
  
      try {
        await client.processDeferredCalls();
      } catch (e) {
        // expected
        error = e;
      }
  
      expect(error).to.be.instanceOf(Error);
    });
  
    it("should be able to resubmit cached request that was failed to submit", async () => {
      await insertARequest();
  
      fakeRequestCallFails();
  
      try {
        await client.processDeferredCalls();
      } catch (_) {
        // expected
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);
  
        requestCallStub.restore();
        sinon.stub(client, "requestCall");
  
        await client.processDeferredCalls();
  
        await verifyNumberOfRequsetsInTheQueue(0);
      }
    });
  
    it("should only submit once when receiving multiple online events", async () => {
      await insertARequest();
  
      await Promise.all([
        client.processDeferredCalls(),
        client.processDeferredCalls(),
        client.processDeferredCalls()
      ])
  
      expect(requestCallStub.calledOnce).to.be.true;
    })
  
    it("should only submit once when receiving multiple online events after a failed submission", async () => {
      await insertARequest();
  
      fakeRequestCallFails();
  
      try {
        await client.processDeferredCalls();
      } catch (_) {
        // expected
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);
  
        requestCallStub.restore();
        sinon.stub(client, "requestCall");
  
        await Promise.all([
          client.processDeferredCalls(),
          client.processDeferredCalls(),
          client.processDeferredCalls()
        ])
  
        expect(requestCallStub.calledOnce).to.be.true;
      }
    });
  
    it('should invoke middleware with isDeferred context', async () => {
      fetchMock.post(base + '/connect/FooEndpoint/fooMethod', {fooData: 'foo'});
  
      requestCallStub.restore();
  
      const spyMiddleware = sinon.spy(async(context: any, next?: any) => {
        expect(context.endpoint).to.equal('FooEndpoint');
        expect(context.method).to.equal('fooMethod');
        expect(context.params).to.deep.equal({fooData: 'foo'});
        expect(context.request).to.be.instanceOf(Request);
        expect(context.isDeferred).to.be.true;
        return next(context);
      });
      client.middlewares = [spyMiddleware];
  
      try {
        await insertARequest();
  
        expect(spyMiddleware.called).to.be.false;
  
        await client.processDeferredCalls();
  
        expect(spyMiddleware.called).to.be.true;
      } finally {
        fetchMock.restore();
      }
    });
  
    it('should invoke deferredCallHandler', async () => {
      await insertARequest();
  
      const onDeferredCallStub = sinon.stub().resolves();
      client.deferredCallHandler = {
        handleDeferredCall: onDeferredCallStub
      };
  
      await client.processDeferredCalls();
  
      expect(onDeferredCallStub.callCount).to.equal(1);
      const [call] = onDeferredCallStub.getCall(0).args;
      expect(call.endpoint).to.equal('FooEndpoint');
      expect(call.method).to.equal('fooMethod');
      expect(call.params).to.deep.equal({fooData: 'foo'});
  
      await verifyNumberOfRequsetsInTheQueue(0);
    });
  
    it('should provide result promise in deferredCallHandler callback', async () => {
      const resultData = {fooData: 'bar'};
      fetchMock.post(base + '/connect/FooEndpoint/fooMethod', resultData);
      requestCallStub.restore();
  
      const onDeferredCallStub = sinon.stub().callsFake(async (_call: any, resultPromise: Promise<any>) => {
        const result = await resultPromise;
        expect(result).to.deep.equal(resultData);
      });
      client.deferredCallHandler = onDeferredCallStub;
  
      try {
        await insertARequest();
        await client.processDeferredCalls();
  
        await verifyNumberOfRequsetsInTheQueue(0);
      } finally {
        fetchMock.restore();
      }
    });
  
    it('should reject if onDeferredCall callback rejects', async () => {
      const onDeferredCallStub = sinon.stub().rejects();
      client.deferredCallHandler = {
        handleDeferredCall: onDeferredCallStub
      };
  
      let error: Error | undefined;
  
      try {
        await insertARequest();
        await client.processDeferredCalls();
      } catch (e) {
        // expected
        error = e;
      } finally {
        expect(error).to.be.instanceOf(Error);
      }
    });
  
    it('should keep request in the queue when user calls to keep the call in the queue', async () => {
      const onDeferredCallStub = sinon.stub().rejects();
      client.deferredCallHandler = {
        handleDeferredCall: onDeferredCallStub
      };
  
      try {
        await insertARequest();
        await client.processDeferredCalls();
      } catch(_) {
        // expected
        //const [call] = onDeferredCallStub.getCall(0).args;
        //call.keepInTheQueue();
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);
      }
    });
  });
});
