import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import { Console } from '../../../../../main/frontend/internal/client/Console';
import type { PushConnection } from '../../../../../main/frontend/internal/client/communication/PushConnection';
import { type StateChangeHandler, UIState } from '../../../../../main/frontend/internal/client/UILifecycle';
import type { EventRemover } from '../../../../../main/frontend/internal/EventRemover';
import { testRegistry } from '../testRegistry';
import { DefaultConnectionStateHandler } from '../../../../../main/frontend/internal/client/communication/DefaultConnectionStateHandler';
import { XhrConnectionError } from '../../../../../main/frontend/internal/client/communication/XhrConnectionError';
import {
  CONNECTED,
  CONNECTION_LOST,
  getState,
  RECONNECTING
} from '../../../../../main/frontend/internal/client/ConnectionIndicator';

function makeRegistry(reconnectAttempts = 3, configuredHeartbeatInterval = 300) {
  const log = {
    reconnectionAttempts: [] as number[],
    heartbeatSends: 0,
    sessionExpired: 0,
    unrecoverable: [] as string[],
    states: [] as string[]
  };
  const lifecycleHandlers: StateChangeHandler[] = [];
  let state: UIState = UIState.RUNNING;
  let heartbeatInterval = 300;
  return {
    log,
    lifecycleHandlers,
    registry: testRegistry({
      UILifecycle: {
        isRunning: () => state === UIState.RUNNING,
        getState: () => state,
        setState: (s: UIState) => {
          state = s;
          log.states.push(s);
        },
        addHandler: (handler: StateChangeHandler): EventRemover => {
          lifecycleHandlers.push(handler);
          return { remove: () => lifecycleHandlers.splice(lifecycleHandlers.indexOf(handler), 1) };
        }
      },
      ReconnectConfiguration: {
        getReconnectAttempts: () => reconnectAttempts,
        getReconnectInterval: () => 5000,
        getDialogText: () => null,
        getDialogTextGaveUp: () => null
      },
      RequestResponseTracker: {
        hasActiveRequest: () => false,
        endRequest: () => {},
        fireReconnectionAttempt: (attempt: number) => log.reconnectionAttempts.push(attempt)
      },
      LoadingIndicatorStateHandler: { stopLoading: () => {} },
      Heartbeat: {
        setInterval: (interval: number) => {
          heartbeatInterval = interval;
        },
        getInterval: () => heartbeatInterval,
        send: () => log.heartbeatSends++
      },
      ApplicationConfiguration: { getHeartbeatInterval: () => configuredHeartbeatInterval },
      MessageSender: { sendInvocationsToServer: () => {} },
      SystemErrorHandler: {
        handleSessionExpiredError: () => log.sessionExpired++,
        handleUnrecoverableError: (_caption: string, message: string) => log.unrecoverable.push(message)
      }
    })
  };
}

// The handler listens on window for the browser's connectivity events.
function dispatch(type: 'online' | 'offline'): void {
  window.dispatchEvent(new Event(type));
}

function xhrError(payload: Record<string, unknown>, status = 500, responseText = ''): XhrConnectionError {
  const xhr = { status, responseText } as unknown as XMLHttpRequest;
  return new XhrConnectionError(xhr, payload, null);
}

describe('DefaultConnectionStateHandler', () => {
  beforeEach(() => {
    (window as { Vaadin?: unknown }).Vaadin = { connectionState: { state: '' } };
  });
  afterEach(() => {
    delete (window as { Vaadin?: unknown }).Vaadin;
  });

  it('re-sends the queued payload immediately on the first xhr failure', () => {
    // Beyond the Java suite: GwtDefaultConnectionStateHandlerTest covers the browser events only; the xhr and push entry points have no Java case.
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry.registry);
    handler.xhrException(xhrError({ rpc: 1 }));
    // First attempt -> immediate doReconnect -> fireReconnectionAttempt(1).
    expect(registry.log.reconnectionAttempts).to.deep.equal([1]);
  });

  it('sends a heartbeat (not a payload) to reconnect a heartbeat failure', () => {
    // Beyond the Java suite: No Java case covers the heartbeat reconnect path.
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry.registry);
    handler.heartbeatException({} as XMLHttpRequest, new Error('down'));
    expect(registry.log.heartbeatSends).to.equal(1);
    expect(registry.log.reconnectionAttempts).to.deep.equal([]);
  });

  it('treats a 403 heartbeat as session expiry and stops the application', () => {
    // Beyond the Java suite: No Java case covers the 403 heartbeat.
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry.registry);
    handler.heartbeatInvalidStatusCode({ status: 403 } as XMLHttpRequest);
    expect(registry.log.sessionExpired).to.equal(1);
    expect(registry.log.states).to.deep.equal(['TERMINATED']);
  });

  it('treats a 401 xhr as unauthorized (session expired) without reconnecting', () => {
    // Beyond the Java suite: No Java case covers the 401 xhr.
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry.registry);
    handler.xhrInvalidStatusCode(xhrError({}, 401));
    expect(registry.log.sessionExpired).to.equal(1);
    expect(registry.log.reconnectionAttempts).to.deep.equal([]);
  });

  it('reports an unrecoverable error for invalid xhr content (no refresh token)', () => {
    // Beyond the Java suite: No Java case covers invalid xhr content.
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry.registry);
    handler.xhrInvalidContent(xhrError({}, 200, 'not json'));
    expect(registry.log.unrecoverable).to.have.length(1);
    expect(registry.log.states).to.deep.equal(['TERMINATED']);
  });

  it('reports a push communication error', () => {
    // Beyond the Java suite: No Java case covers pushError.
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry.registry);
    handler.pushError({ isBidirectional: () => true } as never, { transport: 'websocket' });
    expect(registry.log.unrecoverable[0]).to.contain('websocket');
  });
  it('stops heartbeats while the browser is offline and resumes them', () => {
    // Ported from test_browserEvents_stopsHeartbeats.
    // The Java suite configures the same interval it sets on the heartbeat,
    // because resuming restores the configured one.
    const registry = makeRegistry(3, 10);
    new DefaultConnectionStateHandler(registry.registry);
    registry.registry.getHeartbeat().setInterval(10);

    dispatch('offline');
    expect(registry.registry.getHeartbeat().getInterval()).to.equal(0);

    dispatch('online');
    expect(registry.registry.getHeartbeat().getInterval()).to.equal(10);
  });

  it('goes to connection-lost offline, reconnecting online, and back', () => {
    // Ported from test_onlineEventFollowedByOffline_connectionLost.
    const registry = makeRegistry(3);
    new DefaultConnectionStateHandler(registry.registry);

    dispatch('offline');
    expect(getState()).to.equal(CONNECTION_LOST);

    dispatch('online');
    expect(getState()).to.equal(RECONNECTING);

    dispatch('offline');
    expect(getState()).to.equal(CONNECTION_LOST);
  });

  it('reports connected once the verifying heartbeat succeeds', () => {
    // Ported from test_onlineEventHeartbeatSucceeds_connected.
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry.registry);

    dispatch('offline');
    expect(getState()).to.equal(CONNECTION_LOST);

    dispatch('online');
    expect(getState()).to.equal(RECONNECTING);

    handler.heartbeatOk();
    expect(getState()).to.equal(CONNECTED);
  });

  it('logs that the push connection is being reopened', () => {
    // Beyond the Java suite: No Java case asserts on the logging. ReconnectTest
    // waits for this exact console line to count a reconnection attempt, for
    // both the bidirectional and the long-polling transport.
    const registry = makeRegistry();
    const handler = new DefaultConnectionStateHandler(registry.registry);
    const longPolling = { isBidirectional: () => false } as unknown as PushConnection;
    const debug = sinon.stub(Console, 'debug');
    let messages: string[] = [];
    try {
      handler.pushReconnectPending(longPolling);
    } finally {
      messages = debug.getCalls().map((call) => String(call.args[0]));
      debug.restore();
    }

    expect(messages).to.contain('Reopening push connection');
  });

  it('keeps reconnecting while heartbeats fail, then gives up', () => {
    // Ported from test_onlineEventButHeartbeatFails_continuesReconnectingAndFinallyGivesUp.
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry.registry);

    dispatch('offline');
    expect(getState()).to.equal(CONNECTION_LOST);

    dispatch('online');
    expect(getState()).to.equal(RECONNECTING);

    // Second attempt (the first one follows the transition to RECONNECTING):
    // still reconnecting.
    handler.heartbeatException(new XMLHttpRequest(), new Error('some exception'));
    expect(getState()).to.equal(RECONNECTING);

    // Third attempt: gives up.
    handler.heartbeatException(new XMLHttpRequest(), new Error('some exception'));
    expect(getState()).to.equal(CONNECTION_LOST);
  });
});
