/* tslint:disable: no-unused-expression */
const {describe, it, beforeEach, afterEach, after} = intern.getPlugin('interface.bdd');
const {expect} = intern.getPlugin('chai');
const {fetchMock} = intern.getPlugin('fetchMock');
const {sinon} = intern.getPlugin('sinon');

import {
  ConnectClient,
  EndpointError,
  EndpointResponseError,
  EndpointValidationError,
} from "../../main/resources/META-INF/resources/frontend/Connect";

import {openDB} from "idb";

// `connectClient.call` adds the host and context to the endpoint request.
// we need to add this origin when configuring fetch-mock
const base = window.location.origin;

/* global btoa localStorage setTimeout URLSearchParams Request Response */
describe('ConnectClient', () => {

  function myMiddleware(ctx: any, next?: any) {
    return next(ctx);
  }

  beforeEach(() => localStorage.clear());

  after(() => {
    // @ts-ignore
    delete window.Vaadin;
  });

  it('should be exported', () => {
    expect(ConnectClient).to.be.ok;
  });

  it('should instantiate without arguments', () => {
    const client = new ConnectClient();
    expect(client).to.be.instanceOf(ConnectClient);

  });

  describe('constructor options', () => {
    it('should support prefix', () => {
      const client = new ConnectClient({prefix: '/foo'});
      expect(client).to.have.property('prefix', '/foo');
    });

    it('should support middlewares', () => {
      const client = new ConnectClient({middlewares: [myMiddleware]});
      expect(client).to.have.property('middlewares')
        .deep.equal([myMiddleware]);
    });

    it('should support onDeferredCall', () => {
      const defaultClient = new ConnectClient();
      expect(defaultClient.onDeferredCall).to.be.undefined;

      const onDeferredCall = sinon.stub().resolves();
      const client = new ConnectClient({onDeferredCall});
      expect(client).to.have.property('onDeferredCall').equal(onDeferredCall);
    })
  });

  describe('prefix', () => {
    it('should have default prefix', () => {
      const client = new ConnectClient();
      expect(client).to.have.property('prefix', '/connect');
    });

    it('should allow setting new prefix', () => {
      const client = new ConnectClient();
      client.prefix = '/foo';
      expect(client).to.have.property('prefix', '/foo');
    });
  });

  describe('middlewares', () => {
    it('should have empty middlewares by default', () => {
      const client = new ConnectClient();
      expect(client).to.have.property('middlewares')
        .deep.equal([]);
    });

    it('should allow setting middlewares', () => {
      const client = new ConnectClient();
      client.middlewares = [myMiddleware];
      expect(client).to.have.property('middlewares')
        .deep.equal([myMiddleware]);
    });
  });

  describe('call method', () => {
    beforeEach(() => fetchMock
      .post(base + '/connect/FooEndpoint/fooMethod', {fooData: 'foo'})
    );

    afterEach(() => fetchMock.restore());

    let client: ConnectClient;

    beforeEach(() => {
      client = new ConnectClient();
    });

    it('should require 2 arguments', async() => {
      try {
        // @ts-ignore
        await client.call();
      } catch (err) {
        expect(err).to.be.instanceOf(TypeError)
          .and.have.property('message').that.has.string('2 arguments required');
      }

      try {
        // @ts-ignore
        await client.call('FooEndpoint');
      } catch (err) {
        expect(err).to.be.instanceOf(TypeError)
          .and.have.property('message').that.has.string('2 arguments required');
      }

    });

    it('should fetch endpoint and method from default prefix', async() => {
      expect(fetchMock.calls()).to.have.lengthOf(0); // no premature requests

      await client.call('FooEndpoint', 'fooMethod');

      expect(fetchMock.calls()).to.have.lengthOf(1);
      expect(fetchMock.lastUrl()).to.equal(base + '/connect/FooEndpoint/fooMethod');
    });

    it('should return Promise', () => {
      const returnValue = client.call('FooEndpoint', 'fooMethod');
      expect(returnValue).to.be.a('promise');
    });

    it('should use POST request', async() => {
      await client.call('FooEndpoint', 'fooMethod');

      expect(fetchMock.lastOptions()).to.include({method: 'POST'});
    });

    it('should call Flow.loading indicator', async() => {
      let calls = '';
      (window as any).Vaadin.Flow = {loading: (action: boolean) => calls += action};
      await client.call('FooEndpoint', 'fooMethod');
      expect(calls).to.equal('truefalse');
    });

    it('should use JSON request headers', async() => {
      await client.call('FooEndpoint', 'fooMethod');

      const headers = fetchMock.lastOptions().headers;
      expect(headers).to.deep.include({
        'accept': 'application/json',
        'content-type': 'application/json'
      });
    });

    it('should set header for preventing CSRF', async() => {
      debugger;
      await client.call('FooEndpoint', 'fooMethod');

      const headers = fetchMock.lastOptions().headers;
      expect(headers).to.deep.include({
        'x-csrf-token': ''
      });
    });

    it('should set header for preventing CSRF using Flow csrfToken', async() => {
      // @ts-ignore
      const OriginalVaadin = window.Vaadin;
      // @ts-ignore
      window.Vaadin = {TypeScript: {csrfToken: 'foo'}};

      await client.call('FooEndpoint', 'fooMethod');

      const headers = fetchMock.lastOptions().headers;
      expect(headers).to.deep.include({
        'x-csrf-token': 'foo'
      });

      // @ts-ignore
      window.Vaadin = OriginalVaadin;
    });

    it('should resolve to response JSON data', async() => {
      const data = await client.call('FooEndpoint', 'fooMethod');
      expect(data).to.deep.equal({fooData: 'foo'});
    });

    it('should reject if response is not ok', async() => {
      fetchMock.post(base + '/connect/FooEndpoint/notFound', 404);
      try {
        await client.call('FooEndpoint', 'notFound');
      } catch (err) {
        expect(err).to.be.instanceOf(EndpointError)
          .and.have.property('message').that.has.string('404 Not Found');
      }
    });

    it('should reject with extra parameters in the exception if response body has the data', async() => {
      const expectedObject = {
        message: 'Something bad happened on the backend side',
        type: 'java.lang.IllegalStateException',
        detail: {one: 'two'}
      };
      fetchMock.post(base + '/connect/FooEndpoint/vaadinException', {
        body: expectedObject, status: 400
      });

      try {
        await client.call('FooEndpoint', 'vaadinException');
      } catch (err) {
        expect(err).to.be.instanceOf(EndpointError);
        expect(err).to.have.property('message').that.is.string(expectedObject.message);
        expect(err).to.have.property('type').that.is.string(expectedObject.type);
        expect(err).to.have.deep.property('detail', expectedObject.detail);
      }
    });

    it('should reject with extra unexpected response parameters in the exception if response body has the data', async() => {
      const body = 'Unexpected error';
      const errorResponse = new Response(
          body,
          {
            status: 500,
            statusText: 'Internal Server Error'
          }
      );
      fetchMock.post(base + '/connect/FooEndpoint/vaadinConnectResponse', errorResponse);

      try {
        await client.call('FooEndpoint', 'vaadinConnectResponse');
      } catch (err) {
        expect(err).to.be.instanceOf(EndpointResponseError);
        expect(err).to.have.property('message').that.is.string(body);
        expect(err).to.have.deep.property('response', errorResponse);
      }
    });

    it('should reject with extra validation parameters in the exception if response body has the data', async() => {
      const expectedObject = {
        type: 'com.vaadin.connect.exception.EndpointValidationException',
        message: 'Validation failed',
        validationErrorData: [
          {
            parameterName: 'input',
            message: 'Input cannot be an empty or blank string'
          }
        ]
      };
      fetchMock.post(base + '/connect/FooEndpoint/validationException', {
        body: expectedObject, status: 400
      });

      try {
        await client.call('FooEndpoint', 'validationException');
      } catch (err) {
        expect(err).to.be.instanceOf(EndpointValidationError);
        expect(err).to.have.property('message').that.is.string(expectedObject.message);
        expect(err).to.have.property('type').that.is.string(expectedObject.type);
        expect(err).to.have.property('detail');
        expect(err).to.have.deep.property('validationErrorData', expectedObject.validationErrorData);
      }
    });

    it('should reject if fetch is rejected', async() => {
      fetchMock.post(
        base + '/connect/FooEndpoint/reject',
        Promise.reject(new TypeError('Network failure'))
      );

      try {
        await client.call('FooEndpoint', 'reject');
      } catch (err) {
        expect(err).to.be.instanceOf(TypeError)
          .and.have.property('message').that.has.string('Network failure');
      }
    });

    it('should fetch from custom prefix', async() => {
      fetchMock.post(base + '/fooPrefix/BarEndpoint/barMethod', {barData: 'bar'});

      client.prefix = '/fooPrefix';
      const data = await client.call('BarEndpoint', 'barMethod');

      expect(data).to.deep.equal({barData: 'bar'});
      expect(fetchMock.lastUrl()).to.equal(base + '/fooPrefix/BarEndpoint/barMethod');
    });

    it('should pass 3rd argument as JSON request body', async() => {
      await client.call('FooEndpoint', 'fooMethod', {fooParam: 'foo'});

      const request = fetchMock.lastCall().request;
      expect(request).to.exist;
      expect(await request.json()).to.deep.equal({fooParam: 'foo'});
    });

    describe('middleware invocation', () => {
      it('should not invoke middleware before call', async() => {
        const spyMiddleware = sinon.spy(async(context: any, next?: any) => {
          return next(context);
        });
        client.middlewares = [spyMiddleware];

        (expect(spyMiddleware).to.not.be as any).called;
      });

      it('should invoke middleware during call', async() => {
        const spyMiddleware = sinon.spy(async(context: any, next?: any) => {
          expect(context.endpoint).to.equal('FooEndpoint');
          expect(context.method).to.equal('fooMethod');
          expect(context.params).to.deep.equal({fooParam: 'foo'});
          expect(context.request).to.be.instanceOf(Request);
          expect(context.isDeferred).to.be.false;
          return next(context);
        });
        client.middlewares = [spyMiddleware];

        await client.call(
          'FooEndpoint',
          'fooMethod',
          {fooParam: 'foo'}
        );

        (expect(spyMiddleware).to.be as any).calledOnce;
      });

      it('should allow modified request', async() => {
        const myUrl = 'https://api.example.com/';
        fetchMock.post(myUrl, {});

        const myMiddleware = async(context: any, next?: any) => {
          context.request = new Request(
            myUrl,
            {
              method: 'POST',
              headers: {...context.request.headers,
                'X-Foo': 'Bar'},
              body: '{"baz": "qux"}'
            }
          );
          return next(context);
        };

        client.middlewares = [myMiddleware];
        await client.call('FooEndpoint', 'fooMethod', {fooParam: 'foo'});

        const request = fetchMock.lastCall().request;
        expect(request.url).to.equal(myUrl);
        expect(request.headers.get('X-Foo')).to.equal('Bar');
        expect(await request.text()).to.equal('{"baz": "qux"}');
      });

      it('should allow modified response', async() => {
        const myMiddleware = async(_context: any, _next?: any) => {
          return new Response('{"baz": "qux"}');
        };

        client.middlewares = [myMiddleware];
        const responseData = await client.call('FooEndpoint', 'fooMethod', {fooParam: 'foo'});

        expect(responseData).to.deep.equal({baz: 'qux'});
      });

      it('should invoke middlewares in order', async() => {
        const firstMiddleware = sinon.spy(async(context: any, next?: any) => {
          (expect(secondMiddleware).to.not.be as any).called;
          const response = await next(context);
          (expect(secondMiddleware).to.be as any).calledOnce;
          return response;
        });

        const secondMiddleware = sinon.spy(async(context: any, next?: any) => {
          (expect(firstMiddleware).to.be as any).calledOnce;
          return next(context);
        });

        client.middlewares = [firstMiddleware, secondMiddleware];

        (expect(firstMiddleware).to.not.be as any).called;
        (expect(secondMiddleware).to.not.be as any).called;

        await client.call('FooEndpoint', 'fooMethod', {fooParam: 'foo'});

        (expect(firstMiddleware).to.be as any).calledOnce;
        (expect(secondMiddleware).to.be as any).calledOnce;
        (expect(firstMiddleware).to.be as any).calledBefore(secondMiddleware);
      });

      it('should carry the context and the response', async() => {
        const myRequest = new Request('');
        const myResponse = new Response('{}');
        const myContext = {foo: 'bar', request: myRequest};

        const firstMiddleware = async(_context?: any, next?: any) => {
          // Pass modified context
          const response = await next(myContext);
          // Expect modified response
          expect(response).to.equal(myResponse);
          return response;
        };

        const secondMiddleware = async(context: any, _next?: any) => {
          // Expect modified context
          expect(context).to.equal(myContext);
          // Pass modified response
          return myResponse;
        };

        client.middlewares = [firstMiddleware, secondMiddleware];
        await client.call('FooEndpoint', 'fooMethod', {fooParam: 'foo'});
      });
    });
  });

  describe("Defer Request", () => {
    let client: ConnectClient;

    beforeEach(() => {
      client = new ConnectClient();
    });

    it("Should return a DeferrableResult that retains request meta when invoking deferRequest offline", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => false);
      sinon.stub(client, "cacheEndpointRequest").callsFake((request:any) => {
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
      sinon.stub(client, "checkOnline").callsFake(() => false);

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
      sinon.stub(client, "checkOnline").callsFake(() => false);
      sinon.stub(client, "cacheEndpointRequest");

      const callMethod = sinon.stub(client, "call");

      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(callMethod.called).to.be.false;
    })

    it("should return true when checking the isDefered prooperty of the return value of invoking deferRequest method offline", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => false);
      sinon.stub(client, "call");
      sinon.stub(client, "cacheEndpointRequest");

      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(result.isDeferred).to.be.true;
    })

    it("should return undefined when checking the result prooperty of the return value of invoking deferRequest method offline", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => false);
      sinon.stub(client, "call");
      sinon.stub(client, "cacheEndpointRequest");

      const returnValue = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(returnValue.result).to.be.undefined;
    })

    it("Should invoke the client.call method when invoking deferRequest online", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => true);
      const callMethod = sinon.stub(client, "call");

      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(callMethod.called).to.be.true;
    })

    it("Should not invoke the client.cacheEndpointRequest method when invoking deferRequest online", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => true);
      sinon.stub(client, "call");
      const cacheEndpointRequest = sinon.stub(client, "cacheEndpointRequest");

      await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(cacheEndpointRequest.called).to.be.false;
    })

    it("should return false when checking the isDefered prooperty of the return value of invoking deferRequest method online", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => true);
      sinon.stub(client, "call");
      sinon.stub(client, "cacheEndpointRequest");

      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(result.isDeferred).to.be.false;
    })

    it("should return undefined when checking the endpointRequest prooperty of the return value of invoking deferRequest method offline", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => true);
      sinon.stub(client, "call");
      sinon.stub(client, "cacheEndpointRequest");

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
      const db = await (client as any).openOrCreateDB();
      for(let i=0; i<numberOfRequests; i++){
        await db.put('requests', {endpoint: 'FooEndpoint', method:'fooMethod', params:{ fooData: 'foo' }});
      }
      expect(await db.count('requests')).to.equal(numberOfRequests);
      db.close();
    }

    async function verifyNumberOfRequsetsInTheQueue(numberOfRequests=1) {
      const db = await (client as any).openOrCreateDB();
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
      const db = await (client as any).openOrCreateDB();
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

    it('should invoke onDeferredCall callback', async () => {
      await insertARequest();

      const onDeferredCallStub = sinon.stub().resolves();
      client.onDeferredCall = onDeferredCallStub;

      await client.processDeferredCalls();

      expect(onDeferredCallStub.callCount).to.equal(1);
      const [call, promiseResult] = onDeferredCallStub.getCall(0).args;
      expect(call.endpoint).to.equal('FooEndpoint');
      expect(call.method).to.equal('fooMethod');
      expect(call.params).to.deep.equal({fooData: 'foo'});
      expect(promiseResult).to.be.instanceOf(Promise);

      await verifyNumberOfRequsetsInTheQueue(0);
    });

    it('should provide result promise in onDeferredCall callback', async () => {
      const resultData = {fooData: 'bar'};
      fetchMock.post(base + '/connect/FooEndpoint/fooMethod', resultData);
      requestCallStub.restore();

      const onDeferredCallStub = sinon.stub().callsFake(async (_call: any, resultPromise: Promise<any>) => {
        const result = await resultPromise;
        expect(result).to.deep.equal(resultData);
      });
      client.onDeferredCall = onDeferredCallStub;

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
      client.onDeferredCall = onDeferredCallStub;

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

    it('should keep request in the queue when onDeferredCall callback rejects', async () => {
      const onDeferredCallStub = sinon.stub().rejects();
      client.onDeferredCall = onDeferredCallStub;

      try {
        await insertARequest();
        await client.processDeferredCalls();
      } catch(_) {
        // expected
      } finally {
        await verifyNumberOfRequsetsInTheQueue(1);
      }
    });
  });
});
