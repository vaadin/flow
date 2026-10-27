import { expect } from '@open-wc/testing';
import * as AtmosphereJS from 'atmosphere.js';
import { testRegistry } from '../testRegistry';
import { setAtmosphereModule } from '../../../../../main/frontend/internal/client/communication/AtmosphereLoader';
import {
  AtmospherePushConnection,
  FragmentedMessage
} from '../../../../../main/frontend/internal/client/communication/AtmospherePushConnection';
import { URIResolver } from '../../../../../main/frontend/internal/client/URIResolver';

const tick = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, 0));

type Handler = (...args: unknown[]) => void;

interface AtmosphereCapture {
  request: Record<string, unknown> | null;
  handlers: Record<string, Handler> | null;
}

function setupPush(serviceUrl = '/app/', contextRootUrl = '/', pushParameters = new Map<string, string>()) {
  const log = {
    pushOk: 0,
    pushError: 0,
    pushClosed: 0,
    pushInvalidContent: [] as string[],
    pushNotConnected: 0,
    handled: [] as unknown[],
    pushed: [] as string[],
    closed: 0,
    pushClientTimeout: 0,
    pushScriptLoadErrors: [] as string[],
    lastSeenServerSyncId: 5,
    subscribeError: null as Error | null,
    onPushOk: () => {}
  };
  const capture: AtmosphereCapture = { request: null, handlers: null };
  const fakeSubscription = {
    push: (message: string) => log.pushed.push(message),
    close: async () => {
      log.closed++;
    }
  };
  // Install a fake Atmosphere library.
  setAtmosphereModule(
    Promise.resolve({
      Atmosphere: class {
        async subscribe(request: Record<string, unknown>, handlers: Record<string, Handler>) {
          capture.request = request;
          capture.handlers = handlers;
          if (log.subscribeError) {
            throw log.subscribeError;
          }
          return fakeSubscription;
        }
      }
    })
  );
  const registry = testRegistry({
    UILifecycle: { addHandler: () => ({ remove: () => {} }) },
    PushConfiguration: {
      getParameters: () => pushParameters,
      getPushServletMapping: () => null,
      isAlwaysXhrToServer: () => false
    },
    ConnectionStateHandler: {
      pushOk: () => {
        log.pushOk++;
        log.onPushOk();
      },
      pushError: () => log.pushError++,
      pushClosed: () => log.pushClosed++,
      pushClientTimeout: () => log.pushClientTimeout++,
      pushReconnectPending: () => {},
      pushInvalidContent: (_c: unknown, message: string) => log.pushInvalidContent.push(message),
      pushNotConnected: () => log.pushNotConnected++,
      pushScriptLoadError: (url: string) => log.pushScriptLoadErrors.push(url)
    },
    ApplicationConfiguration: {
      getServiceUrl: () => serviceUrl,
      getContextRootUrl: () => contextRootUrl,
      getUIId: () => 1,
      isProductionMode: () => false
    },
    // The real resolver, so the push url is built from the context root as in
    // production.
    URIResolver: new URIResolver(
      testRegistry({ ApplicationConfiguration: { getContextRootUrl: () => contextRootUrl } })
    ),
    MessageHandler: {
      getPushId: () => null,
      getLastSeenServerSyncId: () => log.lastSeenServerSyncId,
      handleMessage: (json: unknown) => log.handled.push(json)
    },
    ResourceLoader: { loadScript: () => {} }
  });
  return { registry, log, capture };
}

function response(transport: string, body = '', request: Record<string, unknown> = {}) {
  return { transport, responseBody: body, request };
}

// Reassembles fragments produced by FragmentedMessage back into the original
// message (strips the "<length>|" header from the first fragment).
function reassemble(fragments: string[]): string {
  const first = fragments[0];
  const delimiterIndex = first.indexOf('|');
  const length = Number(first.substring(0, delimiterIndex));
  const body = first.substring(delimiterIndex + 1) + fragments.slice(1).join('');
  return body.substring(0, length);
}

describe('AtmospherePushConnection', () => {
  afterEach(() => {
    setAtmosphereModule(null);
  });

  it('does nothing until the atmosphere library is available', async () => {
    // Beyond the Java suite: GwtAtmospherePushConnectionTest sets the library up in every case, so the not-yet-loaded gate has no Java case.
    // The connect waits for the library: while it loads, no subscription happens.
    const { registry, capture } = setupPush();
    setAtmosphereModule(new Promise(() => {}));
    new AtmospherePushConnection(registry);
    await tick();
    expect(capture.request).to.equal(null);
  });

  it('reports a push script that cannot be loaded', async () => {
    const { registry, log } = setupPush();
    setAtmosphereModule(Promise.reject(new Error('Failed to fetch')));
    new AtmospherePushConnection(registry);
    await tick();
    expect(log.pushScriptLoadErrors).to.deep.equal(['/app/VAADIN/static/push/vaadinPush.js']);
  });

  describe('FragmentedMessage', () => {
    function fragmentsOf(message: string): string[] {
      const fragmented = new FragmentedMessage(message);
      const fragments: string[] = [];
      while (fragmented.hasNextFragment()) {
        fragments.push(fragmented.getNextFragment());
      }
      return fragments;
    }

    it('emits a short message as a single length-prefixed fragment', () => {
      // Beyond the Java suite: FragmentedMessage has no Java case of its own; the Gwt suite only asserts the disconnect url.
      const fragments = fragmentsOf('hello');
      expect(fragments).to.deep.equal(['5|hello']);
      expect(reassemble(fragments)).to.equal('hello');
    });

    it('splits a long message into multiple fragments that reassemble', () => {
      // Beyond the Java suite: No Java case covers multi-fragment websocket pushes.
      const message = 'a'.repeat(5000); // > the 4095-char fragment size
      const fragments = fragmentsOf(message);
      expect(fragments.length).to.be.greaterThan(1);
      // Each fragment is at most the websocket fragment size.
      for (const fragment of fragments) {
        expect(fragment.length).to.be.at.most(4095);
      }
      expect(reassemble(fragments)).to.equal(message);
    });
  });

  describe('class', () => {
    it('is active and not yet bidirectional before connecting', async () => {
      // Beyond the Java suite: No Java case covers the state before a transport is known.
      const { registry } = setupPush();
      const connection = new AtmospherePushConnection(registry);
      await tick(); // let the deferred connect() run
      expect(connection.isActive()).to.be.true; // CONNECT_PENDING
      expect(connection.isBidirectional()).to.be.false; // no transport yet
      expect(connection.getTransportType()).to.equal(null);
    });

    it('subscribes with the default config and reconnects enabled', async () => {
      // Beyond the Java suite: The Gwt suite reads the subscribe url only; the config defaults have no Java case.
      const { registry, capture } = setupPush();
      new AtmospherePushConnection(registry);
      await tick();

      const request = capture.request!;
      expect(request.url).to.equal('/app/VAADIN/push?v-r=push&v-uiId=1');
      expect(request.transport).to.equal('websocket');
      expect(request.fallbackTransport).to.equal('long-polling');
      expect(request.trackMessageLength).to.be.true;
      expect(request.enableProtocol).to.be.true;
      expect(request.messageDelimiter).to.equal('|');
      expect(request.reconnect).to.be.true;
      // A constant delay, as the reconnectInterval of 5 s used to be
      const { customCalculate } = request.retryStrategy as { customCalculate: (attempt: number) => number };
      expect([1, 2, 10].map(customCalculate)).to.deep.equal([5000, 5000, 5000]);
    });

    it('reports an error when neither transport can connect', async () => {
      const { registry, log } = setupPush();
      log.subscribeError = new Error('Long-polling connection failed');
      const connection = new AtmospherePushConnection(registry);
      await tick();
      await tick();
      expect(log.pushError).to.equal(1);
      expect(connection.isActive()).to.be.false;
    });

    it('reports a client timeout', async () => {
      const { registry, log, capture } = setupPush();
      const connection = new AtmospherePushConnection(registry);
      await tick();
      capture.handlers!.clientTimeout({ transport: 'long-polling' });
      expect(log.pushClientTimeout).to.equal(1);
      expect(connection.isActive()).to.be.false;
    });

    it('becomes connected and bidirectional on a websocket open, and fragments pushes', async () => {
      // Beyond the Java suite: No Java case drives onOpen through to a push.
      const { registry, log, capture } = setupPush();
      const connection = new AtmospherePushConnection(registry);
      await tick();
      expect(capture.request).to.not.equal(null); // subscribe was called

      capture.handlers!.open(response('websocket'));
      expect(log.pushOk).to.equal(1);
      expect(connection.getTransportType()).to.equal('websocket');
      expect(connection.isBidirectional()).to.be.true;

      connection.push({ a: 1 });
      expect(log.pushed.length).to.be.greaterThan(0); // sent as websocket fragments
      expect(log.pushed.join('')).to.contain('{"a":1}');
    });

    it('routes a valid push message to the message handler and reports invalid content', async () => {
      // Beyond the Java suite: No Java case covers onMessage.
      const { registry, log, capture } = setupPush();
      new AtmospherePushConnection(registry);
      await tick();
      capture.handlers!.open(response('websocket'));

      capture.handlers!.message(response('websocket', '{"syncId":0}'));
      expect(log.handled).to.deep.equal([{ syncId: 0 }]);

      capture.handlers!.message(response('websocket', 'not json'));
      expect(log.pushInvalidContent).to.deep.equal(['not json']);
    });

    it('reports closes, and closes the subscription of an open connection', async () => {
      // Beyond the Java suite: No Java case covers onClose.
      const { registry, log, capture } = setupPush();
      const connection = new AtmospherePushConnection(registry);
      await tick();
      capture.handlers!.open(response('websocket'));

      capture.handlers!.close(response('websocket'));
      expect(log.pushClosed).to.equal(1);
      expect(connection.isActive()).to.be.true; // CONNECT_PENDING after close

      // Re-open then disconnect cleanly.
      capture.handlers!.reopen(response('websocket'));
      let disconnected = false;
      connection.disconnect(() => {
        disconnected = true;
      });
      expect(disconnected).to.be.true;
      expect(connection.isActive()).to.be.false; // DISCONNECTED
      await tick();
      expect(log.closed).to.equal(1);
    });

    it('reports an error only once atmosphere.js gives the connection up', async () => {
      // Beyond the Java suite: atmosphere.js 5 reports every failed attempt through its error handler.
      const { registry, log, capture } = setupPush();
      const connection = new AtmospherePushConnection(registry);
      await tick();
      capture.handlers!.open(response('websocket'));

      capture.handlers!.error(new Error('WebSocket connection error'));
      expect(log.pushError).to.equal(0);
      expect(connection.isActive()).to.be.true;

      capture.handlers!.failureToReconnect({}, response('websocket'));
      expect(log.pushError).to.equal(1);
      expect(connection.isActive()).to.be.false;
    });
  });

  describe('with atmosphere.js', () => {
    // Drives the real atmosphere.js over a fake WebSocket, so what is checked is
    // the contract between the two rather than a stand-in for the library.
    const NativeWebSocket = window.WebSocket;
    let sockets: FakeWebSocket[];

    class FakeWebSocket {
      static readonly OPEN = 1;
      readyState = 0;
      binaryType = '';
      readonly sent: string[] = [];
      onopen: (() => void) | null = null;
      onmessage: ((event: { data: string }) => void) | null = null;
      onerror: (() => void) | null = null;
      onclose: ((event: { code: number; reason: string }) => void) | null = null;

      readonly url: string;

      constructor(url: string) {
        this.url = url;
        sockets.push(this);
      }

      send(message: string): void {
        this.sent.push(message);
      }

      close(): void {
        this.readyState = 3;
        setTimeout(() => this.onclose?.({ code: 1000, reason: '' }), 0);
      }

      drop(): void {
        this.readyState = 3;
        this.onclose?.({ code: 1006, reason: '' });
      }

      open(): void {
        this.readyState = FakeWebSocket.OPEN;
        this.onopen?.();
      }

      receive(data: string): void {
        this.onmessage?.({ data });
      }
    }

    // The Atmosphere protocol handshake: uuid, heartbeat interval and padding,
    // length-prefixed as every message when trackMessageLength is on.
    const handshake = (uuid: string): string => {
      const body = `${uuid}|0|X|`;
      return `${body.length}|${body}`;
    };

    beforeEach(() => {
      sockets = [];
      (window as unknown as { WebSocket: unknown }).WebSocket = FakeWebSocket;
    });
    afterEach(() => {
      window.WebSocket = NativeWebSocket;
    });

    // setupPush installs a fake library; replace it with the real one.
    function setupAtmosphere(pushParameters?: Map<string, string>) {
      const setup = setupPush('/app/', '/', pushParameters);
      setAtmosphereModule(Promise.resolve(AtmosphereJS));
      return setup;
    }

    it('opens on the handshake and delivers length-prefixed messages', async () => {
      const { registry, log } = setupAtmosphere();
      const connection = new AtmospherePushConnection(registry);
      await tick();
      const socket = sockets[0];
      const url = new URL(socket.url);
      expect(url.protocol).to.equal('ws:');
      expect(url.pathname).to.equal('/app/VAADIN/push');
      expect(url.searchParams.get('X-Vaadin-LastSeenServerSyncId')).to.equal('5');
      expect(url.searchParams.get('X-atmo-protocol')).to.equal('true');

      // Messages queued while connecting are sent as soon as the connection
      // is reported open, which needs the subscription to be there already
      log.onPushOk = () => connection.push({ a: 1 });
      socket.open();
      expect(log.pushOk).to.equal(0); // waits for the handshake
      socket.receive(handshake('abc'));
      // open is delivered in a task of its own once subscribe() has resolved
      await tick();
      await tick();
      expect(log.pushOk).to.equal(1);
      expect(connection.getTransportType()).to.equal('websocket');
      expect(socket.sent).to.deep.equal(['7|{"a":1}']);

      socket.receive('12|{"syncId":0}');
      expect(log.handled).to.deep.equal([{ syncId: 0 }]);
    });

    it('does not reconnect when it closes the connection', async () => {
      const { registry, log } = setupAtmosphere();
      const connection = new AtmospherePushConnection(registry);
      await tick();
      sockets[0].open();
      sockets[0].receive(handshake('abc'));
      await tick();

      connection.disconnect(() => {});
      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(sockets).to.have.length(1);
      expect(log.pushClosed).to.equal(0);
    });

    it('delivers messages containing the delimiter after a reconnect', async () => {
      // The server sends the protocol handshake only on the first connection
      const { registry, log } = setupAtmosphere(new Map([['reconnectInterval', '10']]));
      const connection = new AtmospherePushConnection(registry);
      await tick();
      sockets[0].open();
      sockets[0].receive(handshake('abc'));
      await tick();

      log.lastSeenServerSyncId = 8;
      sockets[0].drop();
      await new Promise((resolve) => setTimeout(resolve, 50));
      expect(sockets).to.have.length(2);
      const reconnectUrl = new URL(sockets[1].url);
      expect(reconnectUrl.searchParams.get('X-Atmosphere-tracking-id')).to.equal('abc');
      // atmosphere.js reads the headers anew for every request
      expect(reconnectUrl.searchParams.get('X-Vaadin-LastSeenServerSyncId')).to.equal('8');
      sockets[1].open();
      const message = '{"a":"x|y|z|"}';
      sockets[1].receive(`${message.length}|${message}`);

      expect(log.handled).to.deep.equal([{ a: 'x|y|z|' }]);
      expect(connection.isActive()).to.be.true;
    });

    it('stays on websocket after repeated outages', async () => {
      // atmosphere.js 5 falls back only on the first connection and starts its
      // retry count anew on every reopen, so failed attempts from separate
      // outages do not add up to a fallback.
      const { registry } = setupAtmosphere(new Map([['reconnectInterval', '10']]));
      const connection = new AtmospherePushConnection(registry);
      await tick();
      sockets[0].open();
      sockets[0].receive(handshake('abc'));
      await tick();

      for (let outage = 0; outage < 3; outage++) {
        sockets[sockets.length - 1].drop();
        for (let failedAttempt = 0; failedAttempt < 2; failedAttempt++) {
          await new Promise((resolve) => setTimeout(resolve, 50));
          sockets[sockets.length - 1].drop();
        }
        await new Promise((resolve) => setTimeout(resolve, 50));
        sockets[sockets.length - 1].open();
      }

      expect(sockets).to.have.length(10);
      expect(connection.getTransportType()).to.equal('websocket');
      expect(connection.isActive()).to.be.true;
    });

    describe('over long-polling', () => {
      const nativeFetch = window.fetch;
      const nativeSendBeacon = navigator.sendBeacon;
      let requests: Array<{ url: string; method: string }>;
      let beacons: string[];

      beforeEach(() => {
        requests = [];
        beacons = [];
        // The first poll answers with the handshake, the next ones hang until
        // they are aborted, and anything else, as the close message, succeeds.
        window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
          const url = String(input);
          const method = init?.method ?? 'GET';
          requests.push({ url, method });
          if (method !== 'GET') {
            return new Response('');
          }
          if (requests.filter((request) => request.method === 'GET').length === 1) {
            return new Response(handshake('lp'));
          }
          return new Promise<Response>((_resolve, reject) => {
            init?.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
          });
        };
        navigator.sendBeacon = (url: string | URL) => {
          beacons.push(String(url));
          return true;
        };
      });
      afterEach(() => {
        window.fetch = nativeFetch;
        navigator.sendBeacon = nativeSendBeacon;
      });

      async function openLongPolling() {
        const setup = setupAtmosphere(new Map([['transport', 'long-polling']]));
        const connection = new AtmospherePushConnection(setup.registry);
        await new Promise((resolve) => setTimeout(resolve, 50));
        expect(setup.log.pushOk).to.equal(1);
        expect(connection.getTransportType()).to.equal('long-polling');
        return connection;
      }

      const isCloseMessage = (url: string): boolean => {
        const params = new URL(url, document.baseURI).searchParams;
        return params.get('X-Atmosphere-Transport') === 'close' && params.get('X-Atmosphere-tracking-id') === 'lp';
      };

      it('tells the server when it closes the connection', async () => {
        const connection = await openLongPolling();

        connection.disconnect(() => {});
        await tick();

        const closeMessages = requests.filter((request) => isCloseMessage(request.url));
        expect(closeMessages).to.have.length(1);
        expect(new URL(closeMessages[0].url, document.baseURI).pathname).to.equal('/app/VAADIN/push');
      });

      it('tells the server when the page is left', async () => {
        const connection = await openLongPolling();

        window.dispatchEvent(new Event('pagehide'));

        expect(beacons.filter(isCloseMessage)).to.have.length(1);
        // The page is going away, so the connection state is left as it is
        expect(connection.isActive()).to.be.true;
        connection.disconnect(() => {});
      });
    });
  });
});
