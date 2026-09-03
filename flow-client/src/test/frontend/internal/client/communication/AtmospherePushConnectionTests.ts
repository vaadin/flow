import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import { Console } from '../../../../../main/frontend/internal/client/Console';
import { testRegistry } from '../testRegistry';
import {
  AtmospherePushConnection,
  FragmentedMessage
} from '../../../../../main/frontend/internal/client/communication/AtmospherePushConnection';
import { URIResolver } from '../../../../../main/frontend/internal/client/URIResolver';

const tick = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, 0));

interface AtmosphereConfigCapture {
  config: Record<string, (...args: unknown[]) => unknown> | null;
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
    disconnected: [] as string[]
  };
  const capture: AtmosphereConfigCapture = { config: null };
  const fakeSocket = { push: (message: string) => log.pushed.push(message) };
  // Install a fake Atmosphere library.
  (window as unknown as { vaadinPush?: unknown }).vaadinPush = {
    atmosphere: {
      subscribe: (config: Record<string, (...args: unknown[]) => unknown>) => {
        capture.config = config;
        return fakeSocket;
      },
      unsubscribeUrl: (url: string) => log.disconnected.push(url)
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
      pushClientTimeout: () => {},
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
      getLastSeenServerSyncId: () => 5,
      handleMessage: (json: unknown) => log.handled.push(json)
    },
    ResourceLoader: { loadScript: () => {} }
  });
  return { registry, log, capture };
}

function response(transport: string, body = ''): { transport: string; responseBody: string } {
  // atmosphere.js exposes these as plain properties (not getX() methods).
  return { transport, responseBody: body };
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
    expect(capture.config).to.equal(null);
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

    it('subscribes with the default config and a sync-id supplier', async () => {
      // Beyond the Java suite: The Gwt suite reads the subscribe url only; the config defaults and the header supplier have no Java case.
      const { registry, capture } = setupPush();
      new AtmospherePushConnection(registry);
      await tick();

      const config = capture.config!;
      expect(config.url).to.equal('/app/VAADIN/push?v-r=push&v-uiId=1');
      expect(config.transport).to.equal('websocket');
      expect(config.fallbackTransport).to.equal('long-polling');
      expect(config.trackMessageLength).to.be.true;
      expect(config.messageDelimiter).to.equal('|');

      // The header is a supplier, so the id is re-read on every request rather
      // than frozen when the connection was made.
      const headers = config.headers as unknown as Record<string, () => unknown>;
      expect(headers['X-Vaadin-LastSeenServerSyncId']()).to.equal(5);
    });

    it('becomes connected and bidirectional on a websocket open, and fragments pushes', async () => {
      // Beyond the Java suite: No Java case drives onOpen through to a push.
      const { registry, log, capture } = setupPush();
      const connection = new AtmospherePushConnection(registry);
      await tick();
      expect(capture.config).to.not.equal(null); // subscribe was called

      capture.config!.onOpen(response('websocket'));
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
      capture.config!.onOpen(response('websocket'));

      capture.config!.onMessage(response('websocket', '{"syncId":0}'));
      expect(log.handled).to.deep.equal([{ syncId: 0 }]);

      capture.config!.onMessage(response('websocket', 'not json'));
      expect(log.pushInvalidContent).to.deep.equal(['not json']);
    });

    it('logs the connection and received-message diagnostics', async () => {
      // Beyond the Java suite: No Java case asserts on the logging.
      // SendMultibyteCharactersTest matches a console line that starts with
      // "Received " and contains "message:"; over a bidirectional websocket the
      // response never goes through XhrConnection, so this is the only one.
      const { registry, capture } = setupPush();
      const debug = sinon.stub(Console, 'debug');
      let messages: string[] = [];
      try {
        new AtmospherePushConnection(registry);
        await tick();
        capture.config!.onOpen(response('websocket'));
        capture.config!.onMessage(response('websocket', '{"syncId":0}'));
      } finally {
        messages = debug.getCalls().map((call) => String(call.args[0]));
        debug.restore();
      }

      expect(messages).to.contain('Establishing push connection');
      expect(messages).to.contain('Push connection established using websocket');
      expect(messages).to.contain('Received push (websocket) message: {"syncId":0}');
    });

    it('reports errors and closes, and disconnects an open connection', async () => {
      // Beyond the Java suite: No Java case covers onError/onClose.
      const { registry, log, capture } = setupPush();
      const connection = new AtmospherePushConnection(registry);
      await tick();
      capture.config!.onOpen(response('websocket'));

      capture.config!.onClose(response('websocket'));
      expect(log.pushClosed).to.equal(1);
      expect(connection.isActive()).to.be.true; // CONNECT_PENDING after close

      // Re-open then disconnect cleanly.
      capture.config!.onOpen(response('websocket'));
      let disconnected = false;
      connection.disconnect(() => {
        disconnected = true;
      });
      expect(disconnected).to.be.true;
      expect(log.disconnected.length).to.equal(1);
      expect(connection.isActive()).to.be.false; // DISCONNECTED
    });
    it('unsubscribes from the same url it subscribed to', async () => {
      // Ported from testDisconnect_disconnectUrlIsSameAsInConnect.
      const { registry, log, capture } = setupPush('context://foo', 'bar/');
      const connection = new AtmospherePushConnection(registry);
      await tick();
      const pushUri = capture.config!.url as unknown as string;

      capture.config!.onOpen(response('websocket'));
      connection.disconnect(() => {});

      expect(log.disconnected).to.have.length(1);
      expect(log.disconnected[0].startsWith('bar/')).to.be.true;
      expect(log.disconnected[0]).to.equal(pushUri);
    });
  });
});
