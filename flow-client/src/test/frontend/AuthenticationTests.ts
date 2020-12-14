const {describe, it, beforeEach, afterEach, after} = intern.getPlugin('interface.bdd');
const {expect} = intern.getPlugin('chai');
const {fetchMock} = intern.getPlugin('fetchMock');
const {sinon} = intern.getPlugin('sinon');

import { ConnectClient, InvalidSessionMiddleware, login, logout } from "../../main/resources/META-INF/resources/frontend";

// `connectClient.call` adds the host and context to the endpoint request.
// we need to add this origin when configuring fetch-mock
const base = window.location.origin;

/* global btoa localStorage setTimeout URLSearchParams Request Response */
describe('Authentication', () => {

  after(() => {
    // @ts-ignore
    delete window.Vaadin.TypeScript;
  });

  describe('login', () => {
    beforeEach(() => fetchMock
      .post(base + '/connect/FooEndpoint/fooMethod', {fooData: 'foo'})
    );

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
  });

  describe("logout", () => {
    afterEach(() => fetchMock.restore());

    it('should set the csrf token on logout', async () => {
      fetchMock.get('/logout', {
        body: 'window.Vaadin = {TypeScript: {"csrfToken":"6a60700e-852b-420f-a126-a1c61b73d1ba"}};',
        redirectUrl: '/logout?login'
      });
      await logout();
      expect(fetchMock.calls()).to.have.lengthOf(1);
      expect((window as any).Vaadin.TypeScript.csrfToken).to.equal("6a60700e-852b-420f-a126-a1c61b73d1ba");
    });

    it('should clear the csrf token on failed server logout', async () => {
      const fakeError = new Error('unable to connect');
      fetchMock.get('/logout', () => {
        throw fakeError;
      });
      try {
        await logout();
      } catch (err) {
        expect(err).to.equal(fakeError);
      }
      expect(fetchMock.calls()).to.have.lengthOf(1);
      expect((window as any).Vaadin.TypeScript.csrfToken).to.be.undefined
    });
  });

  describe("InvalidSessionMiddleWare", ()=>{
    afterEach(() => fetchMock.restore());

    it("should invoke the onInvalidSession callback on 401 response", async ()=>{
      fetchMock.post(base + '/connect/FooEndpoint/fooMethod', 401)

      const invalidSessionCallback = sinon.spy(()=>{
        // mock to pass authentication
        fetchMock.restore();
        fetchMock.post(base + '/connect/FooEndpoint/fooMethod', {fooData: 'foo'})
        return {
          error: false,
          token: "csrf-token"
        }
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
});
