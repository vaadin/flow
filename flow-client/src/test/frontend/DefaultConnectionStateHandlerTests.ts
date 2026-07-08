import { expect } from '@open-wc/testing';
import { DefaultConnectionStateHandler } from '../../main/frontend/internal/communication/DefaultConnectionStateHandler';
import { XhrConnectionError } from '../../main/frontend/internal/communication/XhrConnectionError';

function makeRegistry(reconnectAttempts = 3) {
  const log = {
    reconnectionAttempts: [] as number[],
    heartbeatSends: 0,
    sessionExpired: 0,
    unrecoverable: [] as string[],
    states: [] as string[]
  };
  const lifecycleHandlers: Array<(event: { getUiLifecycle(): { isTerminated(): boolean } }) => void> = [];
  let state = 'RUNNING';
  const registry = {
    log,
    lifecycleHandlers,
    getUILifecycle: () => ({
      isRunning: () => state === 'RUNNING',
      getState: () => state,
      setState: (s: string) => {
        state = s;
        log.states.push(s);
      },
      addHandler: (h: (event: { getUiLifecycle(): { isTerminated(): boolean } }) => void) => lifecycleHandlers.push(h)
    }),
    getReconnectConfiguration: () => ({
      getReconnectAttempts: () => reconnectAttempts,
      getReconnectInterval: () => 5000,
      getDialogText: () => null,
      getDialogTextGaveUp: () => null
    }),
    getRequestResponseTracker: () => ({
      hasActiveRequest: () => false,
      endRequest: () => {},
      fireReconnectionAttempt: (attempt: number) => log.reconnectionAttempts.push(attempt)
    }),
    getLoadingIndicatorStateHandler: () => ({ stopLoading: () => {} }),
    getHeartbeat: () => ({ setInterval: () => {}, getInterval: () => 300, send: () => log.heartbeatSends++ }),
    getApplicationConfiguration: () => ({ getHeartbeatInterval: () => 300 }),
    getMessageSender: () => ({ sendInvocationsToServer: () => {} }),
    getSystemErrorHandler: () => ({
      handleSessionExpiredError: () => log.sessionExpired++,
      handleUnrecoverableError: (_caption: string, message: string) => log.unrecoverable.push(message)
    })
  };
  return registry;
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
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry as never);
    handler.xhrException(xhrError({ rpc: 1 }));
    // First attempt -> immediate doReconnect -> fireReconnectionAttempt(1).
    expect(registry.log.reconnectionAttempts).to.deep.equal([1]);
  });

  it('sends a heartbeat (not a payload) to reconnect a heartbeat failure', () => {
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry as never);
    handler.heartbeatException({} as XMLHttpRequest, new Error('down'));
    expect(registry.log.heartbeatSends).to.equal(1);
    expect(registry.log.reconnectionAttempts).to.deep.equal([]);
  });

  it('treats a 403 heartbeat as session expiry and stops the application', () => {
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry as never);
    handler.heartbeatInvalidStatusCode({ status: 403 } as XMLHttpRequest);
    expect(registry.log.sessionExpired).to.equal(1);
    expect(registry.log.states).to.deep.equal(['TERMINATED']);
  });

  it('treats a 401 xhr as unauthorized (session expired) without reconnecting', () => {
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry as never);
    handler.xhrInvalidStatusCode(xhrError({}, 401));
    expect(registry.log.sessionExpired).to.equal(1);
    expect(registry.log.reconnectionAttempts).to.deep.equal([]);
  });

  it('reports an unrecoverable error for invalid xhr content (no refresh token)', () => {
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry as never);
    handler.xhrInvalidContent(xhrError({}, 200, 'not json'));
    expect(registry.log.unrecoverable).to.have.length(1);
    expect(registry.log.states).to.deep.equal(['TERMINATED']);
  });

  it('reports a push communication error', () => {
    const registry = makeRegistry(3);
    const handler = new DefaultConnectionStateHandler(registry as never);
    handler.pushError({ isBidirectional: () => true } as never, { transport: 'websocket' });
    expect(registry.log.unrecoverable[0]).to.contain('websocket');
  });
});
