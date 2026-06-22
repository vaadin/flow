import { expect } from '@open-wc/testing';
import {
  createConfig,
  doConnect,
  doDisconnect,
  doPush,
  isAtmosphereLoaded
} from '../../main/frontend/internal/AtmospherePushConnection';

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
});
