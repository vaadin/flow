import { expect } from '@open-wc/testing';
import * as AtmosphereJS from 'atmosphere.js';
import { testRegistry } from '../testRegistry';
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

function setupPush(serviceUrl = '/app/', contextRootUrl = '/') {
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
    lastSeenServerSyncId: 5,
    subscribeError: null as Error | null,
    // When set, subscribe() resolves only once this is called
    releaseSubscription: null as (() => void) | null
  };
  const capture: AtmosphereCapture = { request: null, handlers: null };
  const fakeSubscription = {
    push: (message: string) => log.pushed.push(message),
    close: async () => {
      log.closed++;
    }
  };
  // Install a fake Atmosphere library.
  (window as unknown as { vaadinPush?: unknown }).vaadinPush = {
    atmosphere: {
      Atmosphere: class {
        async subscribe(request: Record<string, unknown>, handlers: Record<string, Handler>) {
          capture.request = request;
          capture.handlers = handlers;
          if (log.releaseSubscription) {
            await new Promise<void>((resolve) => {
              log.releaseSubscription = resolve;
            });
          }
          if (log.subscribeError) {
            throw log.subscribeError;
          }
          return fakeSubscription;
        }
      }
    }
  };
  const registry = testRegistry({
    UILifecycle: { addHandler: () => ({ remove: () => {} }) },
    PushConfiguration: {
      getParameters: () => new Map<string, string>(),
      getPushServletMapping: () => null,
      isAlwaysXhrToServer: () => false
    },
    ConnectionStateHandler: {
      pushOk: () => log.pushOk++,
      pushError: () => log.pushError++,
      pushClosed: () => log.pushClosed++,
      pushClientTimeout: () => log.pushClientTimeout++,
      pushReconnectPending: () => {},
      pushInvalidContent: (_c: unknown, message: string) => log.pushInvalidContent.push(message),
      pushNotConnected: () => log.pushNotConnected++,
      pushScriptLoadError: () => {}
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
  const win = window as unknown as { vaadinPush?: unknown };
  let saved: unknown;
  beforeEach(() => {
    saved = win.vaadinPush;
  });
  afterEach(() => {
    win.vaadinPush = saved;
  });

  it('does nothing until the atmosphere library is available', async () => {
    // Beyond the Java suite: GwtAtmospherePushConnectionTest sets the library up in every case, so the not-yet-loaded gate has no Java case.
    // isAtmosphereLoaded gates the deferred connect: with no library installed,
    // no subscription happens.
    const { registry, capture } = setupPush();
    win.vaadinPush = undefined;
    new AtmospherePushConnection(registry);
    await tick();
    expect(capture.request).to.equal(null);
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
    afterEach(() => {
      delete (window as { vaadinPush?: unknown }).vaadinPush;
    });

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
    });

    it('reads the last seen sync id anew for every request', async () => {
      // Ported from the header-supplier case: atmosphere.js reads the headers for each request it makes.
      const { registry, log, capture } = setupPush();
      new AtmospherePushConnection(registry);
      await tick();
      const headers = capture.request!.headers as Record<string, string>;
      expect(headers['X-Vaadin-LastSeenServerSyncId']).to.equal('5');

      log.lastSeenServerSyncId = 8;
      expect(headers['X-Vaadin-LastSeenServerSyncId']).to.equal('8');
    });

    it('sends pushes made before the subscription is handed out, in order', async () => {
      // atmosphere.js may report a connection open before subscribe() resolves.
      const { registry, log, capture } = setupPush();
      log.releaseSubscription = () => {};
      const connection = new AtmospherePushConnection(registry);
      await tick();
      capture.handlers!.open(response('websocket'));
      connection.push({ a: 1 });
      connection.push({ b: 2 });
      expect(log.pushed).to.deep.equal([]);

      log.releaseSubscription();
      await tick();
      expect(log.pushed).to.deep.equal(['7|{"a":1}', '7|{"b":2}']);
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
      delete (window as { vaadinPush?: unknown }).vaadinPush;
    });

    // setupPush installs a fake library; replace it with the real one.
    function setupAtmosphere() {
      const setup = setupPush();
      (window as unknown as { vaadinPush?: unknown }).vaadinPush = { atmosphere: AtmosphereJS };
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

      socket.open();
      expect(log.pushOk).to.equal(0); // waits for the handshake
      socket.receive(handshake('abc'));
      expect(log.pushOk).to.equal(1);
      expect(connection.getTransportType()).to.equal('websocket');

      socket.receive('12|{"syncId":0}');
      expect(log.handled).to.deep.equal([{ syncId: 0 }]);

      await tick();
      connection.push({ a: 1 });
      expect(socket.sent).to.deep.equal(['7|{"a":1}']);
    });

    it('does not reconnect a connection it closed', async () => {
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
  });
});
