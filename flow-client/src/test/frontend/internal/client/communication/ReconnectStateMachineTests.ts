// Beyond the Java suite: ReconnectStateMachine has no Java test class in src/test/java or src/test-gwt/java —
// it is a TypeScript-only split of DefaultConnectionStateHandler, whose Gwt cases drive the handler — so every case here is beyond the Java suite.
import { testRegistry } from '../testRegistry';
import { expect } from '@open-wc/testing';
import { ConnectionState, ConnectionStateStore } from '@vaadin/common-frontend';
import { getState } from '../../../../../main/frontend/internal/client/ConnectionIndicator';
import { LoadingIndicatorStateHandler } from '../../../../../main/frontend/internal/client/communication/LoadingIndicatorStateHandler';
import { ConnectionMessageType } from '../../../../../main/frontend/internal/client/communication/ConnectionMessageType';
import { ReconnectStateMachine } from '../../../../../main/frontend/internal/client/communication/ReconnectStateMachine';

function makeRegistry(reconnectAttempts = 3) {
  const log = { endRequests: 0, restoreLoadings: 0, heartbeatIntervals: [] as number[] };
  let activeRequest = true;
  return {
    log,
    setActiveRequest: (v: boolean) => {
      activeRequest = v;
    },
    registry: testRegistry({
      UILifecycle: { isRunning: () => true },
      ReconnectConfiguration: { getReconnectAttempts: () => reconnectAttempts },
      RequestResponseTracker: { hasActiveRequest: () => activeRequest, endRequest: () => log.endRequests++ },
      LoadingIndicatorStateHandler: { restoreLoading: () => log.restoreLoadings++ },
      Heartbeat: { setInterval: (i: number) => log.heartbeatIntervals.push(i) }
    })
  };
}

describe('ReconnectStateMachine', () => {
  // ConnectionIndicator.setState writes to window.Vaadin.connectionState.
  beforeEach(() => {
    (window as { Vaadin?: unknown }).Vaadin = { connectionState: { state: '' } };
  });
  afterEach(() => {
    delete (window as { Vaadin?: unknown }).Vaadin;
  });

  it('starts reconnecting on the first recoverable error and schedules a retry', () => {
    const scheduled: unknown[] = [];
    const machine = new ReconnectStateMachine(makeRegistry(3).registry, (p) => scheduled.push(p));

    machine.handleRecoverableError(ConnectionMessageType.XHR, { rpc: 1 });
    expect(machine.isReconnecting()).to.be.true;
    expect(machine.getReconnectionCause()).to.equal(ConnectionMessageType.XHR);
    expect(machine.getReconnectAttempt()).to.equal(1);
    expect(scheduled).to.deep.equal([{ rpc: 1 }]);
  });

  it('lets a higher-priority failure take over the reconnection cause', () => {
    const machine = new ReconnectStateMachine(makeRegistry(5).registry, () => {});
    machine.handleRecoverableError(ConnectionMessageType.HEARTBEAT, null);
    expect(machine.getReconnectionCause()).to.equal(ConnectionMessageType.HEARTBEAT);
    // XHR outranks HEARTBEAT -> becomes the cause and counts as its attempt.
    machine.handleRecoverableError(ConnectionMessageType.XHR, null);
    expect(machine.getReconnectionCause()).to.equal(ConnectionMessageType.XHR);
    expect(machine.getReconnectAttempt()).to.equal(2);
  });

  it('gives up after the configured maximum attempts (CONNECTION_LOST, heartbeat paused)', () => {
    const registry = makeRegistry(2);
    const scheduled: unknown[] = [];
    const machine = new ReconnectStateMachine(registry.registry, (p) => scheduled.push(p));

    machine.handleRecoverableError(ConnectionMessageType.XHR, null); // attempt 1 -> schedule
    machine.handleRecoverableError(ConnectionMessageType.XHR, null); // attempt 2 >= 2 -> give up
    expect(machine.isReconnecting()).to.be.false;
    expect(scheduled).to.have.length(1);
    expect(registry.log.heartbeatIntervals).to.deep.equal([0]); // heartbeats paused (resumable)
    expect(registry.log.endRequests).to.equal(1);
  });

  it('resolves a temporary error only for the active cause', () => {
    const registry = makeRegistry(5);
    const cancels: number[] = [];
    const machine = new ReconnectStateMachine(
      registry.registry,
      () => {},
      () => cancels.push(1)
    );
    machine.handleRecoverableError(ConnectionMessageType.XHR, null);

    // A non-matching resolution is ignored.
    machine.resolveTemporaryError(ConnectionMessageType.PUSH);
    expect(machine.isReconnecting()).to.be.true;

    // The matching resolution clears the state and restores loading (XHR path).
    machine.resolveTemporaryError(ConnectionMessageType.XHR);
    expect(machine.isReconnecting()).to.be.false;
    expect(machine.getReconnectAttempt()).to.equal(0);
    expect(registry.log.restoreLoadings).to.equal(1);
    expect(cancels).to.deep.equal([1]);
  });

  it('does nothing when the UI is not running', () => {
    // The lifecycle is the one service this case needs to differ, so it builds
    // its own registry rather than replacing a getter on one.
    const log = { endRequests: 0, restoreLoadings: 0, heartbeatIntervals: [] as number[] };
    const registry = testRegistry({
      UILifecycle: { isRunning: () => false },
      ReconnectConfiguration: { getReconnectAttempts: () => 3 },
      RequestResponseTracker: { hasActiveRequest: () => true, endRequest: () => log.endRequests++ },
      LoadingIndicatorStateHandler: { restoreLoading: () => log.restoreLoadings++ },
      Heartbeat: { setInterval: (i: number) => log.heartbeatIntervals.push(i) }
    });
    const machine = new ReconnectStateMachine(registry, () => {});
    machine.handleRecoverableError(ConnectionMessageType.XHR, null);
    expect(machine.isReconnecting()).to.be.false;
  });

  describe('with the connection state store', () => {
    // The real store the connection indicator renders, and the real loading
    // handler resolving a PUSH or XHR failure goes through.
    function makeMachine() {
      (window as { Vaadin?: unknown }).Vaadin = {
        connectionState: new ConnectionStateStore(ConnectionState.CONNECTED)
      };
      let activeRequest = false;
      const registry = testRegistry({
        UILifecycle: { isRunning: () => true },
        ReconnectConfiguration: { getReconnectAttempts: () => 5 },
        RequestResponseTracker: { hasActiveRequest: () => activeRequest, endRequest: () => {} },
        Heartbeat: { setInterval: () => {} }
      });
      const loadingIndicatorStateHandler = new LoadingIndicatorStateHandler(registry);
      registry.register('LoadingIndicatorStateHandler', loadingIndicatorStateHandler);
      return {
        machine: new ReconnectStateMachine(registry, () => {}),
        loadingIndicatorStateHandler,
        setActiveRequest: (v: boolean) => {
          activeRequest = v;
        }
      };
    }

    // The loading handler defers its update through the scheduler.
    const afterDeferred = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, 0));

    it('shows connected after a push connection is re-established', async () => {
      const { machine } = makeMachine();
      machine.handleRecoverableError(ConnectionMessageType.PUSH, null);
      expect(getState()).to.equal(ConnectionState.RECONNECTING);

      machine.resolveTemporaryError(ConnectionMessageType.PUSH);
      await afterDeferred();
      expect(getState()).to.equal(ConnectionState.CONNECTED);
    });

    it('shows connected after a request that was loading is re-sent successfully', async () => {
      const { machine, loadingIndicatorStateHandler, setActiveRequest } = makeMachine();
      loadingIndicatorStateHandler.processMessage(null, null);
      loadingIndicatorStateHandler.startLoading();
      setActiveRequest(true);
      machine.handleRecoverableError(ConnectionMessageType.XHR, { rpc: 1 });
      expect(getState()).to.equal(ConnectionState.RECONNECTING);

      // The re-sent request succeeds, then its response has been handled.
      machine.resolveTemporaryError(ConnectionMessageType.XHR);
      await afterDeferred();
      expect(getState()).to.equal(ConnectionState.LOADING);
      setActiveRequest(false);
      loadingIndicatorStateHandler.stopLoading();
      await afterDeferred();
      expect(getState()).to.equal(ConnectionState.CONNECTED);
    });
  });
});
