const {describe, it, beforeEach, afterEach} = intern.getPlugin('interface.bdd');
const {expect} = intern.getPlugin('chai');
const {fetchMock} = intern.getPlugin('fetchMock');
const {sinon} = intern.getPlugin('sinon');

import { ConnectClient, VaadinConnectError, VaadinConnectValidationError, createRequest } from "../../main/resources/META-INF/resources/frontend/Connect";

// `connectClient.call` adds the host and context to the service request.
// we need to add this origin when configuring fetch-mock
const base = window.location.origin;

/* global btoa localStorage setTimeout URLSearchParams Request Response */
describe('ConnectClient', () => {

  function generateOAuthJson() {
    const jwt = btoa('{"alg": "HS256", "typ": "JWT"}');
    // expiration comes in seconds from Vaadin Connect Server
    // We add 400ms to accessToken and 800ms to refreshToken
    const accessToken = btoa(`{"exp": ${Date.now() / 1000 + 0.400}, "user_name": "foo"}`);
    const refreshToken = btoa(`{"exp": ${Date.now() / 1000 + 0.800}}`);

    return {
      access_token: `${jwt}.${accessToken}.SIGNATURE`,
      refresh_token: `${jwt}.${refreshToken}.SIGNATURE`,
      exp: 10
    };
  }

  async function sleep(ms: number) {
    await new Promise(resolve => setTimeout(resolve, ms));
  }

  function myMiddleware(ctx: any, next?: any) {
    return next(ctx);
  }

  beforeEach(() => localStorage.clear());

  it('should be exported', () => {
    expect(ConnectClient).to.be.ok;
  });

  it('should instantiate without arguments', () => {
    const client = new ConnectClient();
    expect(client).to.be.instanceOf(ConnectClient);
  });

  describe('constructor options', () => {
    it('should support endpoint', () => {
      const client = new ConnectClient({endpoint: '/foo'});
      expect(client).to.have.property('endpoint', '/foo');
    });

    it('should support tokenEndpoint', () => {
      const client = new ConnectClient({tokenEndpoint: '/foo'});
      expect(client).to.have.property('tokenEndpoint', '/foo');
    });

    it('should support middlewares', () => {
      const client = new ConnectClient({middlewares: [myMiddleware]});
      expect(client).to.have.property('middlewares')
        .deep.equal([myMiddleware]);
    });
  });

  describe('endpoint', () => {
    it('should have default endpoint', () => {
      const client = new ConnectClient();
      expect(client).to.have.property('endpoint', '/connect');
    });

    it('should allow setting new endpoint', () => {
      const client = new ConnectClient();
      client.endpoint = '/foo';
      expect(client).to.have.property('endpoint', '/foo');
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
      .post(base + '/connect/FooService/fooMethod', {fooData: 'foo'})
    );

    afterEach(() => fetchMock.restore());
    let a : number = 1;
    a = 2;
    console.log(a);

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
        await client.call('FooService');
      } catch (err) {
        expect(err).to.be.instanceOf(TypeError)
          .and.have.property('message').that.has.string('2 arguments required');
      }
    });

    it('should fetch service and method from default endpoint', async() => {
      expect(fetchMock.calls()).to.have.lengthOf(0); // no premature requests

      await client.call('FooService', 'fooMethod');

      expect(fetchMock.calls()).to.have.lengthOf(1);
      expect(fetchMock.lastUrl()).to.equal(base + '/connect/FooService/fooMethod');
    });

    it('should return Promise', () => {
      const returnValue = client.call('FooService', 'fooMethod');
      expect(returnValue).to.be.a('promise');
    });

    it('should use POST request', async() => {
      await client.call('FooService', 'fooMethod');

      expect(fetchMock.lastOptions()).to.include({method: 'POST'});
    });

    it('should use JSON request headers', async() => {
      await client.call('FooService', 'fooMethod');

      const headers = fetchMock.lastOptions().headers;
      expect(headers).to.deep.include({
        'accept': 'application/json',
        'content-type': 'application/json'        
      });
    });

    it('should set header for preventing CSRF', async() => {
      await client.call('FooService', 'fooMethod');

      const headers = fetchMock.lastOptions().headers;
      expect(headers).to.deep.include({
        'x-requested-with': 'Vaadin CCDM'
      });
    });

    it('should resolve to response JSON data', async() => {
      const data = await client.call('FooService', 'fooMethod');
      expect(data).to.deep.equal({fooData: 'foo'});
    });

    it('should reject if response is not ok', async() => {
      fetchMock.post(base + '/connect/FooService/notFound', 404);
      try {
        await client.call('FooService', 'notFound');
      } catch (err) {
        expect(err).to.be.instanceOf(VaadinConnectError)
          .and.have.property('message').that.has.string('404 Not Found');
      }
    });

    it('should reject with extra parameters in the exception if response body has the data', async() => {
      const expectedObject = {
        message: 'Something bad happened on the backend side',
        type: 'java.lang.IllegalStateException',
        detail: {one: 'two'}
      };
      fetchMock.post(base + '/connect/FooService/vaadinException', {
        body: expectedObject, status: 400
      });

      try {
        await client.call('FooService', 'vaadinException');
      } catch (err) {
        expect(err).to.be.instanceOf(VaadinConnectError);
        expect(err).to.have.property('message').that.is.string(expectedObject.message);
        expect(err).to.have.property('type').that.is.string(expectedObject.type);
        expect(err).to.have.deep.property('detail', expectedObject.detail);
      }
    });

    it('should reject with extra validation parameters in the exception if response body has the data', async() => {
      const expectedObject = {
        type: 'com.vaadin.connect.exception.VaadinConnectValidationException',
        message: 'Validation failed',
        validationErrorData: [
          {
            parameterName: 'input',
            message: 'Input cannot be an empty or blank string'
          }
        ]
      };
      fetchMock.post(base + '/connect/FooService/validationException', {
        body: expectedObject, status: 400
      });

      try {
        await client.call('FooService', 'validationException');
      } catch (err) {
        expect(err).to.be.instanceOf(VaadinConnectValidationError);
        expect(err).to.have.property('message').that.is.string(expectedObject.message);
        expect(err).to.have.property('type').that.is.string(expectedObject.type);
        expect(err).to.have.property('detail');
        expect(err).to.have.deep.property('validationErrorData', expectedObject.validationErrorData);
      }
    });

    it('should reject if fetch is rejected', async() => {
      fetchMock.post(
        base + '/connect/FooService/reject',
        Promise.reject(new TypeError('Network failure'))
      );

      try {
        await client.call('FooService', 'reject');
      } catch (err) {
        expect(err).to.be.instanceOf(TypeError)
          .and.have.property('message').that.has.string('Network failure');
      }
    });

    it('should fetch from custom endpoint', async() => {
      fetchMock.post(base + '/fooEndpoint/BarService/barMethod', {barData: 'bar'});

      client.endpoint = '/fooEndpoint';
      const data = await client.call('BarService', 'barMethod');

      expect(data).to.deep.equal({barData: 'bar'});
      expect(fetchMock.lastUrl()).to.equal(base + '/fooEndpoint/BarService/barMethod');
    });

    it('should pass 3rd argument as JSON request body', async() => {
      await client.call('FooService', 'fooMethod', {fooParam: 'foo'});

      const requestBody = fetchMock.lastCall().request.body;
      expect(requestBody).to.exist;
      expect(JSON.parse(requestBody.toString())).to.deep.equal({fooParam: 'foo'});
    });

    describe('middleware invocation', () => {
      it('should not invoke middleware before call', async() => {
        const spyMiddleware = sinon.spy(async(context: any, next?: any) => {
          return await next(context);
        });
        client.middlewares = [spyMiddleware];

        (expect(spyMiddleware).to.not.be as any).called;
      });

      it('should invoke middleware during call', async() => {
        const spyMiddleware = sinon.spy(async(context: any, next?: any) => {
          expect(context.service).to.equal('FooService');
          expect(context.method).to.equal('fooMethod');
          expect(context.params).to.deep.equal({fooParam: 'foo'});
          expect(context.options)
            .to.deep.equal({requireCredentials: true});
          expect(context.request).to.be.instanceOf(Request);
          return await next(context);
        });
        client.middlewares = [spyMiddleware];

        await client.call(
          'FooService',
          'fooMethod',
          {fooParam: 'foo'},
          {requireCredentials: true}
        );

        (expect(spyMiddleware).to.be as any).calledOnce;
      });

      it('should allow modified request', async() => {
        const myUrl = 'https://api.example.com/';
        fetchMock.post(myUrl, {});

        const myMiddleware = async(context: any, next?: any) => {
          context.request = createRequest(
            myUrl,
            {
              method: 'POST',
              headers: Object.assign({}, context.request.headers, {
                'X-Foo': 'Bar'
              }),
              body: '{"baz": "qux"}'
            }
          );
          return await next(context);
        };

        client.middlewares = [myMiddleware];
        await client.call('FooService', 'fooMethod', {fooParam: 'foo'});

        const request = fetchMock.lastCall().request;
        expect(request.url).to.equal(myUrl);
        expect(request.headers.get('X-Foo')).to.equal('Bar');
        expect(request.body).to.exist;
        // expect(request.body.toString()).to.equal('{"baz": "qux"}');
      });

      it('should allow modified response', async() => {
        const myMiddleware = async(context: any, next?: any) => {
          return new Response('{"baz": "qux"}');
        };

        client.middlewares = [myMiddleware];
        const responseData = await client.call('FooService', 'fooMethod', {fooParam: 'foo'});

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
          return await next(context);
        });

        client.middlewares = [firstMiddleware, secondMiddleware];

        (expect(firstMiddleware).to.not.be as any).called;
        (expect(secondMiddleware).to.not.be as any).called;

        await client.call('FooService', 'fooMethod', {fooParam: 'foo'});

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
        await client.call('FooService', 'fooMethod', {fooParam: 'foo'});
      });
    });
  });

  describe('login method', () => {
    let client: ConnectClient;

    beforeEach(() => {
      client = new ConnectClient({credentials: sinon.fake
        .returns({username: 'user', password: 'abc123'})});
      fetchMock.post(client.tokenEndpoint, generateOAuthJson);
    });

    afterEach(() => {
      fetchMock.restore();
    });

    it('should request token endpoint with credentials when calling login', async() => {

      const token = await client.login();
      const [[url, {method, body}]] = fetchMock.calls();

      expect(token).not.to.be.null;
      expect(method).to.equal('POST');
      expect(url).to.equal('/oauth/token');
      expect(body.toString())
        .to.equal('grant_type=password&username=user&password=abc123');
    });

    it('should request token endpoint only once after login', async() => {
      const vaadinEndpoint = '/connect/FooService/fooMethod';
      fetchMock.post(base + vaadinEndpoint, {fooData: 'foo'});
      const token = await client.login();
      await client.call('FooService', 'fooMethod');

      expect(token).not.to.be.null;
      expect(fetchMock.calls()).to.have.lengthOf(2);
      expect(fetchMock.calls()[0][0]).to.be.equal(client.tokenEndpoint);
      expect(fetchMock.calls()[1][0]).to.be.equal(base + vaadinEndpoint);
    });

    it('should use refreshToken if available', async() => {
      localStorage.setItem('vaadin.connect.refreshToken', generateOAuthJson().refresh_token);
      const newClient = new ConnectClient({credentials: client.credentials});
      const token = await newClient.login();

      expect(token).not.to.be.null;
      expect(fetchMock.calls()).to.have.lengthOf(1);
      (expect(newClient.credentials).not.to.be as any).called;

      let [, {body}] = fetchMock.calls()[0];
      body = new URLSearchParams(body);
      expect(body.get('grant_type')).to.be.equal('refresh_token');
    });
  });

  describe('checkLoggedIn method', () => {
    let client: ConnectClient;
    beforeEach(() => {
      client = new ConnectClient({credentials: sinon.fake
        .throws('Unexpected method call for credentials')});
      fetchMock.post(client.tokenEndpoint, generateOAuthJson);
    });

    afterEach(() => {
      fetchMock.restore();
    });

    it('should login successfully without asking for credentials', async() => {
      const isLoginSuccessful = await client.checkLoggedIn();
      expect(isLoginSuccessful).to.be.true;
    });
  });

  describe('tokenEndpoint', () => {
    it('should have default tokenEndpoint', () => {
      expect(new ConnectClient())
        .to.have.property('tokenEndpoint', '/oauth/token');
    });

    it('should allow setting new tokenEndpoint', () => {
      const client = new ConnectClient();
      client.tokenEndpoint = '/foo';
      expect(client).to.have.property('tokenEndpoint', '/foo');
    });
  });

  describe('credentials', () => {
    let client: ConnectClient;
    const vaadinEndpoint = '/connect/FooService/fooMethod';

    beforeEach(() => {
      client = new ConnectClient();
      fetchMock.post(base + vaadinEndpoint, {fooData: 'foo'});
    });

    afterEach(() => {
      fetchMock.restore();
    });

    describe('without credentials', () => {
      it('should not include Authorization header by default', async() => {
        await client.call('FooService', 'fooMethod');
        expect(fetchMock.lastOptions().headers)
          .to.not.have.property('authorization');
      });
    });

    describe('with credentials', () => {
      beforeEach(() => {
        const credentials = sinon.fake
          .returns({username: 'user', password: 'abc123'});
        client.credentials = credentials;
      });

      it('should ask for credentials when accessToken is missing', async() => {
        fetchMock.post(client.tokenEndpoint, generateOAuthJson);
        await client.call('FooService', 'fooMethod');
        (expect(client.credentials).to.be as any).calledOnce;
        (expect((client.credentials as any).lastCall).to.be as any).calledWithExactly();
      });

      it('should not request token endpoint when credentials are falsy', async() => {
        for (const falsy of [false, '', 0, null, undefined, NaN]) {
          client.credentials = sinon.fake.returns(falsy);
          await client.call('FooService', 'fooMethod');
          expect(fetchMock.lastUrl()).to.not.equal(client.tokenEndpoint);
        }
      });

      it('should ask for credencials again when one is missing', async() => {
        client.credentials = sinon.stub();
        (client.credentials as any).onCall(0).returns({password: 'abc123'});
        (client.credentials as any).onCall(1).returns({username: 'user'});
        (client.credentials as any).onCall(2).returns(false);

        await client.call('FooService', 'fooMethod');
        (expect(client.credentials).to.be as any).calledThrice;
      });

      it('should request token endpoint with credentials', async() => {
        fetchMock.post(client.tokenEndpoint, generateOAuthJson);

        await client.call('FooService', 'fooMethod');

        const [[url, {method, body}]] = fetchMock.calls();

        expect(method).to.equal('POST');
        expect(url).to.equal('/oauth/token');
        expect(body.toString())
          .to.equal('grant_type=password&username=user&password=abc123');
      });

      it('should require credentials when requireCredentials is not specified but other options are', async() => {
        fetchMock.post(client.tokenEndpoint, generateOAuthJson);

        await client.call('FooService', 'fooMethod', undefined, {});

        const [[url, {method, body}]] = fetchMock.calls();

        expect(method).to.equal('POST');
        expect(url).to.equal('/oauth/token');
        expect(body.toString())
          .to.equal('grant_type=password&username=user&password=abc123');
      });

      it('should expose accessToken data', async() => {
        fetchMock.post(client.tokenEndpoint, generateOAuthJson);

        await client.call('FooService', 'fooMethod');
        expect(client.token).to.be.ok;
        expect(client.token.exp).to.be.above(Date.now() / 1000);
        expect(client.token.user_name).to.be.equal('foo');
      });

      it('should not be able to modify accessToken data', async() => {
        fetchMock.post(client.tokenEndpoint, generateOAuthJson);

        await client.call('FooService', 'fooMethod');
        client.token.user_name = 'bar';
        expect(client.token.user_name).to.be.equal('foo');
      });

      it('should ask for credentials again when token response is 400 or 401', async() => {
        client.credentials = sinon.stub();
        (client.credentials as any).onCall(0).returns({username: 'user', password: 'abc123'});
        (client.credentials as any).onCall(1).returns({username: 'user', password: 'abc123'});
        (client.credentials as any).onCall(2).returns(false);

        fetchMock
          .post(client.tokenEndpoint,
            {body: {error: 'invalid_grant', error_description: 'Bad credentials'}, status: 400},
            {repeat: 1})
          .post(client.tokenEndpoint,
            {body: {error: 'unathorized', error_description: 'Unauthorized'}, status: 401},
            {repeat: 1, overwriteRoutes: false});

        const data = await client.call('FooService', 'fooMethod');
        expect(data).to.deep.equal({fooData: 'foo'});

        expect(fetchMock.calls().length).to.be.equal(3);

        (expect(client.credentials).to.be as any).calledThrice;
        (expect((client.credentials as any).getCall(0)).to.be as any).calledWithExactly();
        expect((client.credentials as any).getCall(1).args)
          .to.deep.equal([{message: 'Bad credentials'}]);
        expect((client.credentials as any).getCall(2).args)
          .to.deep.equal([{message: 'Unauthorized'}]);

        expect(fetchMock.calls()[0][0]).to.be.equal(client.tokenEndpoint);
        expect(fetchMock.calls()[1][0]).to.be.equal(client.tokenEndpoint);
        expect(fetchMock.calls()[2][0]).to.be.equal(base + vaadinEndpoint);
        expect(fetchMock.calls()[2][1].headers).not.to.have.property('Authorization');
      });

      it('should throw when token response is bad', async() => {
        const expectedBody = 'Server Internal Error';
        fetchMock.post(
          client.tokenEndpoint,
          {body: expectedBody, status: 500}
        );

        try {
          await client.call('FooService', 'fooMethod');
        } catch (err) {
          expect(err).to.be.instanceOf(VaadinConnectError)
            .and.have.property('message')
            .that.has.string(expectedBody);
          (expect(client.credentials).to.be as any).calledOnce;
        }
      });

      it('should use accessToken when token response is ok', async() => {
        const response = generateOAuthJson();
        fetchMock.post(client.tokenEndpoint, response);

        const data = await client.call('FooService', 'fooMethod');

        let [url, {method, headers, body}] = fetchMock.calls()[0];
        body = new URLSearchParams(body);
        expect(body.get('grant_type')).to.be.equal('password');
        expect(body.get('username')).to.be.equal('user');
        expect(body.get('password')).to.be.equal('abc123');

        [url, {method, headers, body}] = fetchMock.calls()[1];
        expect(method).to.equal('POST');
        expect(url).to.equal(base + vaadinEndpoint);
        expect(headers).to.deep.include({
          'authorization': `Bearer ${response.access_token}`
        });
        expect(data).to.deep.equal({fooData: 'foo'});
      });

      describe('refreshToken', () => {
        beforeEach(async() => {
          fetchMock.post(client.tokenEndpoint, generateOAuthJson);
        });

        it('should re-use login promise', async() => {
          await Promise.all([
            client.login(),
            client.login(),
            client.login(),
            client.login()]);
          (expect(client.credentials).to.be as any).calledOnce;
        });

        it('should re-use login in multiple calls', async() => {
          await Promise.all([
            client.call('FooService', 'fooMethod'),
            client.call('FooService', 'fooMethod')]);
          (expect(client.credentials).to.be as any).calledOnce;
        });

        it('should not call credentials if another auth request is pending', async() => {
          // do a First request to get an accessToken and a refreshToken
          await client.call('FooService', 'fooMethod');

          // Wait until accessToken expires but not the refreshToken
          // generated response has a expiration of 400ms for token and 800 for refresh
          await sleep(600);
          const call1 = client.call('FooService', 'fooMethod');
          const call2 = client.call('FooService', 'fooMethod');

          const [data1, data2] = await Promise.all([call1, call2]);

          expect(data1).to.deep.equal({fooData: 'foo'});
          expect(data2).to.deep.equal({fooData: 'foo'});
          (expect(client.credentials).to.be as any).calledOnce;
          expect(fetchMock.calls().length).to.be.equal(5);
        });

        it('should use refreshToken when accessToken is expired', async() => {
          // do a First request to get an accessToken and a refreshToken
          await client.call('FooService', 'fooMethod');

          // Wait until accessToken expires but not the refreshToken
          // generated response has a expiration of 400ms for token and 800 for refresh
          await sleep(600);
          const data = await client.call('FooService', 'fooMethod');

          expect(data).to.deep.equal({fooData: 'foo'});
          (expect(client.credentials).to.be as any).calledOnce;
          expect(fetchMock.calls().length).to.be.equal(4);

          let [, {body}] = fetchMock.calls()[2];
          body = new URLSearchParams(body);
          expect(body.get('grant_type')).to.be.equal('refresh_token');
          expect(body.get('refresh_token')).to.be.ok;
          expect(body.get('username')).to.be.null;
        });

        it('should call credentials if refreshToken is expired', async() => {
          // do a First request to get an accessToken and a refreshToken
          await client.call('FooService', 'fooMethod');

          // Wait until both accessToken and refresToken expire
          await sleep(1000);
          const data = await client.call('FooService', 'fooMethod');

          expect(data).to.deep.equal({fooData: 'foo'});
          (expect(client.credentials).to.be as any).calledTwice;
          expect(fetchMock.calls().length).to.be.equal(4);

          let [, {body}] = fetchMock.calls()[2];
          body = new URLSearchParams(body);
          expect(body.get('grant_type')).to.be.equal('password');
          expect(body.get('username')).to.be.equal('user');
          expect(body.get('password')).to.be.equal('abc123');
        });

        it('should not save refreshToken when stayLoggedIn is false', async() => {
          // A first request to get authentication but not storing refreshToken
          await client.call('FooService', 'fooMethod');
          expect(await localStorage.getItem('vaadin.connect.refreshToken')).not.to.be.ok;

          // emulate refresh page
          const newClient = new ConnectClient();
          newClient.credentials = client.credentials;
          await newClient.call('FooService', 'fooMethod');
          expect(await localStorage.getItem('vaadin.connect.refreshToken')).not.to.be.ok;

          (expect(client.credentials).to.be as any).calledTwice;
          expect(fetchMock.calls().length).to.be.equal(4);

          let [, {body}] = fetchMock.calls()[2];
          body = new URLSearchParams(body);
          expect(body.get('grant_type')).to.be.equal('password');
        });

        it('should not fail if stored accessToken is corrupted', async() => {
          localStorage.setItem('vaadin.connect.refreshToken', 'CORRUPTED-TOKEN');

          const newClient = new ConnectClient();
          newClient.credentials = client.credentials;
          await newClient.call('FooService', 'fooMethod');

          (expect(client.credentials).to.be as any).calledOnce;
          expect(fetchMock.calls().length).to.be.equal(2);

          let [, {body}] = fetchMock.calls()[0];
          body = new URLSearchParams(body);
          expect(body.get('grant_type')).to.be.equal('password');
        });

        it('should not use refreshToken if getting invalid_token response', async() => {
          localStorage.setItem('vaadin.connect.refreshToken', generateOAuthJson().refresh_token);
          fetchMock.restore();

          fetchMock
            .post(client.tokenEndpoint,
              {body: {error: 'invalid_token', error_description: 'Cannot convert access token to JSON'}, status: 401},
              {repeat: 1})
            .post(client.tokenEndpoint, generateOAuthJson,
              {repeat: 1, overwriteRoutes: false})
            .post(base + vaadinEndpoint, {fooData: 'foo'});

          const newClient = new ConnectClient({credentials: client.credentials});
          await newClient.call('FooService', 'fooMethod');

          (expect(newClient.credentials).to.be as any).calledOnce;
          expect(fetchMock.calls().length).to.be.equal(3);

          let [, {body}] = fetchMock.calls()[0];
          body = new URLSearchParams(body);
          expect(body.get('grant_type')).to.be.equal('refresh_token');

          [, {body}] = fetchMock.calls()[1];
          body = new URLSearchParams(body);
          expect(body.get('grant_type')).to.be.equal('password');
        });
      });

      describe('with {requireCredentials: false} option', () => {
        it('should include Authorization header if authorized before', async() => {
          // Simulate login
          const response = generateOAuthJson();
          fetchMock.post(client.tokenEndpoint, response);
          await client.call('FooService', 'fooMethod');

          await client.call('FooService', 'fooMethod', undefined,
            {requireCredentials: false});

          expect(fetchMock.lastOptions().headers).to.deep.include({
            'authorization': `Bearer ${response.access_token}`
          });
        });

        it('should not include Authorization header by default', async() => {
          await client.call('FooService', 'fooMethod', undefined,
            {requireCredentials: false});

          expect(fetchMock.calls().length).to.equal(1);
          expect(fetchMock.lastOptions().headers)
            .to.not.have.property('authorization');
        });

        it('should not ask for credentials', async() => {
          await client.call('FooService', 'fooMethod', undefined,
            {requireCredentials: false});

          (expect(client.credentials).to.not.be as any).called;
        });
      });

      describe('with stayLoggedIn', () => {
        beforeEach(() => {
          const credentials = sinon.fake
            .returns({username: 'user', password: 'abc123', stayLoggedIn: true});
          client.credentials = credentials;
        });

        it('should use refreshToken from localStorage when client refreshes', async() => {
          fetchMock.post(client.tokenEndpoint, generateOAuthJson);

          await client.call('FooService', 'fooMethod');
          expect(await localStorage.getItem('vaadin.connect.refreshToken')).to.be.ok;

          // refresh should re-use refreshToken
          const newClient = new ConnectClient();
          newClient.credentials = sinon.fake();
          await newClient.call('FooService', 'fooMethod');

          (expect(newClient.credentials).not.be as any).called;
          expect(fetchMock.calls().length).to.be.equal(4);

          let [, {body}] = fetchMock.calls()[2];
          body = new URLSearchParams(body);
          expect(body.get('grant_type')).to.be.equal('refresh_token');
        });

        describe('logout', () => {
          it('should remove tokens on logout', async() => {
            fetchMock.post(client.tokenEndpoint, generateOAuthJson);

            await client.call('FooService', 'fooMethod');
            expect(await localStorage.getItem('vaadin.connect.refreshToken')).to.be.ok;

            await client.logout();
            expect(await localStorage.getItem('vaadin.connect.refreshToken')).not.to.be.ok;

            expect(fetchMock.calls().length).to.be.equal(2);

            await client.call('FooService', 'fooMethod');
            expect(fetchMock.calls().length).to.be.equal(4);
          });

          it('should abort pending token request on logout', async() => {
            // Delay token response
            fetchMock.post(client.tokenEndpoint, () =>
              new Promise(resolve => setTimeout(() => resolve(generateOAuthJson()), 500)));
            // Logout before token request finishes
            setTimeout(() => client.logout(), 300);
            try {
              await client.call('FooService', 'fooMethod');
              expect.fail('token request not aborted');
            } catch (error) {
              expect(error.message).to.equal('The operation was aborted.');
            }
          });

          it('should not abort new request after logout', async() => {
            fetchMock.post(client.tokenEndpoint, generateOAuthJson);
            client.logout();
            await client.call('FooService', 'fooMethod');
            const data = await client.call('FooService', 'fooMethod');
            expect(data).to.deep.equal({fooData: 'foo'});
          });
        });
      });

    });
  });
});
