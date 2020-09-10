/* tslint:disable: no-unused-expression */
const {describe, it, beforeEach, afterEach, after} = intern.getPlugin('interface.bdd');
const {expect} = intern.getPlugin('chai');
const {fetchMock} = intern.getPlugin('fetchMock');
const {sinon} = intern.getPlugin('sinon');

import { ConnectClient, EndpointCallContinue, EndpointError, EndpointResponseError, EndpointValidationError, InvalidSessionMiddleware, login, logout } from "../../main/resources/META-INF/resources/frontend/Connect";

import { clear, get, Store } from 'idb-keyval';

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
        const myMiddleware = async(context: any, next?: any) => {
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

        const firstMiddleware = async(context?: any, next?: any) => {
          // Pass modified context
          const response = await next(myContext);
          // Expect modified response
          expect(response).to.equal(myResponse);
          return response;
        };

        const secondMiddleware = async(context: any, next?: any) => {
          // Expect modified context
          expect(context).to.equal(myContext);
          // Pass modified response
          return myResponse;
        };

        client.middlewares = [firstMiddleware, secondMiddleware];
        await client.call('FooEndpoint', 'fooMethod', {fooParam: 'foo'});
      });
    });

    describe('login', () => {
      afterEach(() => fetchMock.restore());

      it('should return an error on invalid credentials', async () => {
        fetchMock.post('/login', { redirectUrl: '/login?error' });
        const result = await login('invalid-username', 'invalid-password');
        const expectedResult = {
          error: true,
          errorTitle: 'Incorrect username or password.',
          errorMessage: 'Check that you have entered the correct username and password and try again.'
        };

        expect(fetchMock.calls()).to.have.lengthOf(1);
        expect(result).to.deep.equal(expectedResult);
      })

      it('should return a CSRF token on valid credentials', async () => {
        fetchMock.post('/login', {
          body: 'window.Vaadin = {TypeScript: {"csrfToken":"6a60700e-852b-420f-a126-a1c61b73d1ba"}};',
          redirectUrl: '/'
        });
        const result = await login('valid-username', 'valid-password');
        const expectedResult = {
          error: false,
          errorTitle: '',
          errorMessage: '',
          token: '6a60700e-852b-420f-a126-a1c61b73d1ba'
        };

        expect(fetchMock.calls()).to.have.lengthOf(1);
        expect(result).to.deep.equal(expectedResult);
      })

      it('should return an error on other unexpected responses', async () => {
        const body = 'Unexpected error';
        const errorResponse = new Response(
          body,
          {
            status: 500,
            statusText: 'Internal Server Error'
          }
        );
        fetchMock.post('/login', errorResponse);
        const result = await login('valid-username', 'valid-password');
        const expectedResult = {
          error: true,
          errorTitle: 'Error',
          errorMessage: 'Something went wrong when trying to login.'
        };

        expect(fetchMock.calls()).to.have.lengthOf(1);
        expect(result).to.deep.equal(expectedResult);
      })
    })
  });

  describe("logout", () => {
    it('should set the csrf token on logout', async () => {
      fetchMock.get('/logout', {
        body: 'window.Vaadin = {TypeScript: {"csrfToken":"6a60700e-852b-420f-a126-a1c61b73d1ba"}};',
        redirectUrl: '/logout?login'
      });
      await logout();
      expect(fetchMock.calls()).to.have.lengthOf(1);
      expect((window as any).Vaadin.TypeScript.csrfToken).to.equal("6a60700e-852b-420f-a126-a1c61b73d1ba");
    });
  });

  describe("InvalidSessionMiddleWare", ()=>{
    afterEach(() => fetchMock.restore());

    it("should invoke the onInvalidSession callback on 401 response", async ()=>{
      fetchMock.post(base + '/connect/FooEndpoint/fooMethod', 401)
      
      const invalidSessionCallback = sinon.spy((continueFunc: EndpointCallContinue)=>{
        // mock to pass authentication
        fetchMock.restore();
        fetchMock.post(base + '/connect/FooEndpoint/fooMethod', {fooData: 'foo'})
        continueFunc("csrf-token");
      });
      const middleware = new InvalidSessionMiddleware(invalidSessionCallback);
      
      const client = new ConnectClient({middlewares:[middleware]});
        
      await client.call('FooEndpoint','fooMethod');
      
      expect(invalidSessionCallback.calledOnce).to.be.true;

      const headers = fetchMock.lastOptions().headers;
      expect(headers).to.deep.include({
        'x-csrf-token': 'csrf-token'
      });
    })

    it("should not invoke the onInvalidSession callback on 200 response", async ()=>{
      fetchMock.post(base + '/connect/FooEndpoint/fooMethod', {fooData: 'foo'})
      
      const invalidSessionCallback = sinon.spy();
      const middleware = new InvalidSessionMiddleware(invalidSessionCallback);
      
      const client = new ConnectClient({middlewares:[middleware]});
      await client.call('FooEndpoint', 'fooMethod');
      
      expect(invalidSessionCallback.called).to.be.false;
    })
  });

  describe("Defer Request", () => {
    let client: ConnectClient;
    const offlineRequestQueue = new Store('cached-vaadin-endpoint-requests');

    beforeEach(() => {
      client = new ConnectClient();
    });

    afterEach(() => {
      fetchMock.restore();
      clear(offlineRequestQueue);
    });

    it("Should return a DeferrableResult that retains request meta when invoking deferRequest offline", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => false);

      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      expect(result.isDeferred).to.be.true;
      expect(result.endpointRequest?.endpoint).to.equal('FooEndpoint');
      expect(result.endpointRequest?.method).to.equal('fooMethod');
      expect(result.endpointRequest?.params?.fooData).to.equal('foo');
    })

    it("Should cache the endpoint request when invoking deferRequest offline", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => false);

      const result = await client.deferrableCall('FooEndpoint', 'fooMethod', { fooData: 'foo' });

      const endpointRequetsStore = new Store('cached-vaadin-endpoint-requests');
      const cachedRequest = await get(result.endpointRequest?.id as string, endpointRequetsStore);

      expect((cachedRequest as any).endpoint).to.equal('FooEndpoint');
      expect((cachedRequest as any).method).to.equal('fooMethod');
      expect((cachedRequest as any).params?.fooData).to.equal('foo');
    })

    it("Should not invoke the client.call method when invoking deferRequest offline", async () => {
      sinon.stub(client, "checkOnline").callsFake(() => false);
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
});
