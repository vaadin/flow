import { expect } from '@open-wc/testing';
import {
  AtmospherePushConnection,
  createConfig,
  doConnect,
  doDisconnect,
  doPush,
  FragmentedMessage,
  isAtmosphereLoaded
} from '../../main/frontend/internal/AtmospherePushConnection';

const tick = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, 0));

interface AtmosphereConfigCapture {
  config: Record<string, (...args: unknown[]) => unknown> | null;
}

function setupPush() {
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
  const registry = {
    log,
    getUILifecycle: () => ({ addHandler: () => {} }),
    getPushConfiguration: () => ({
      getParameters: () => new Map<string, string>(),
      getPushServletMapping: () => null,
      isAlwaysXhrToServer: () => false
    }),
    getConnectionStateHandler: () => ({
      pushOk: () => log.pushOk++,
      pushError: () => log.pushError++,
      pushClosed: () => log.pushClosed++,
      pushClientTimeout: () => {},
      pushReconnectPending: () => {},
      pushInvalidContent: (_c: unknown, message: string) => log.pushInvalidContent.push(message),
      pushNotConnected: () => log.pushNotConnected++,
      pushScriptLoadError: () => {}
    }),
    getApplicationConfiguration: () => ({
      getServiceUrl: () => '/app/',
      getContextRootUrl: () => '/',
      getUIId: () => 1,
      isProductionMode: () => false
    }),
    getURIResolver: () => ({ resolveVaadinUri: (uri: string) => uri }),
    getMessageHandler: () => ({
      getPushId: () => null,
      getLastSeenServerSyncId: () => 5,
      handleMessage: (json: unknown) => log.handled.push(json)
    }),
    getResourceLoader: () => ({ loadScript: () => {} })
  };
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

  it('isAtmosphereLoaded reflects window.vaadinPush.atmosphere', () => {
    win.vaadinPush = undefined;
    expect(isAtmosphereLoaded()).to.be.false;
    win.vaadinPush = { atmosphere: { subscribe: () => ({}), unsubscribeUrl: () => {} } };
    expect(isAtmosphereLoaded()).to.be.true;
  });

  it('doPush pushes the message over the socket', () => {
    let pushed: string | undefined;
    const socket = {
      push: (message: string) => {
        pushed = message;
      }
    };
    doPush(socket, 'hello');
    expect(pushed).to.equal('hello');
  });

  it('doDisconnect unsubscribes the url via atmosphere', () => {
    let unsubscribed: string | undefined;
    win.vaadinPush = {
      atmosphere: {
        subscribe: () => ({}),
        unsubscribeUrl: (url: string) => {
          unsubscribed = url;
        }
      }
    };
    doDisconnect('http://localhost/push');
    expect(unsubscribed).to.equal('http://localhost/push');
  });

  it('doConnect wires url and callbacks onto the config and subscribes', () => {
    let subscribedConfig: any;
    win.vaadinPush = {
      atmosphere: {
        subscribe: (config: unknown) => {
          subscribedConfig = config;
          return { socket: true };
        },
        unsubscribeUrl: () => {}
      }
    };

    const calls: string[] = [];
    const callbacks = {
      onOpen: () => calls.push('onOpen'),
      onReopen: () => calls.push('onReopen'),
      onMessage: () => calls.push('onMessage'),
      onError: () => calls.push('onError'),
      onTransportFailure: () => calls.push('onTransportFailure'),
      onClose: () => calls.push('onClose'),
      onReconnect: () => calls.push('onReconnect'),
      onClientTimeout: () => calls.push('onClientTimeout'),
      getLastSeenServerSyncId: () => 42
    };

    const config: Record<string, unknown> = { transport: 'websocket' };
    const socket = doConnect('http://localhost/push', config, callbacks);

    expect(socket).to.deep.equal({ socket: true });
    expect(subscribedConfig).to.equal(config);
    expect(config.url).to.equal('http://localhost/push');
    expect(config.transport).to.equal('websocket');

    // Each handler is wired straight through.
    (config.onOpen as () => void)();
    (config.onMessage as () => void)();
    expect(calls).to.deep.equal(['onOpen', 'onMessage']);

    // The sync-id header is a supplier read on every request.
    const headers = config.headers as Record<string, () => unknown>;
    expect(headers['X-Vaadin-LastSeenServerSyncId']()).to.equal(42);
  });

  it('createConfig builds the default config with the given message delimiter', () => {
    const config = createConfig('|'.charCodeAt(0));
    expect(config.transport).to.equal('websocket');
    expect(config.fallbackTransport).to.equal('long-polling');
    expect(config.trackMessageLength).to.be.true;
    expect(config.messageDelimiter).to.equal('|');
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
      const fragments = fragmentsOf('hello');
      expect(fragments).to.deep.equal(['5|hello']);
      expect(reassemble(fragments)).to.equal('hello');
    });

    it('splits a long message into multiple fragments that reassemble', () => {
      const message = 'a'.repeat(5000); // > the 4095-char fragment size
      const fragments = fragmentsOf(message);
      expect(fragments.length).to.be.greaterThan(1);
      // Each fragment is at most the websocket fragment size.
      for (const fragment of fragments) {
        expect(fragment.length).to.be.at.most(4095);
      }
      expect(reassemble(fragments)).to.equal(message);
    });

    it('starts the first fragment with the message length and delimiter', () => {
      const fragmented = new FragmentedMessage('abc');
      expect(fragmented.getNextFragment().startsWith('3|')).to.be.true;
      expect(fragmented.hasNextFragment()).to.be.false;
    });
  });

  describe('class', () => {
    afterEach(() => {
      delete (window as { vaadinPush?: unknown }).vaadinPush;
    });

    it('is active and not yet bidirectional before connecting', async () => {
      const { registry } = setupPush();
      const connection = new AtmospherePushConnection(registry as never);
      await tick(); // let the deferred connect() run
      expect(connection.isActive()).to.be.true; // CONNECT_PENDING
      expect(connection.isBidirectional()).to.be.false; // no transport yet
      expect(connection.getTransportType()).to.equal('');
    });

    it('becomes connected and bidirectional on a websocket open, and fragments pushes', async () => {
      const { registry, log, capture } = setupPush();
      const connection = new AtmospherePushConnection(registry as never);
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
      const { registry, log, capture } = setupPush();
      new AtmospherePushConnection(registry as never);
      await tick();
      capture.config!.onOpen(response('websocket'));

      capture.config!.onMessage(response('websocket', '{"syncId":0}'));
      expect(log.handled).to.deep.equal([{ syncId: 0 }]);

      capture.config!.onMessage(response('websocket', 'not json'));
      expect(log.pushInvalidContent).to.deep.equal(['not json']);
    });

    it('reports errors and closes, and disconnects an open connection', async () => {
      const { registry, log, capture } = setupPush();
      const connection = new AtmospherePushConnection(registry as never);
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
  });
});
