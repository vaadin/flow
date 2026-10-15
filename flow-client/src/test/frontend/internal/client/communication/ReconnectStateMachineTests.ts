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
      LoadingIndicatorStateHandler: { restoreLoading: () => log.restoreLoadings++, loadingDropped: () => {} },
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

    // The matching resolution clears the state and restores loading.
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
      LoadingIndicatorStateHandler: { restoreLoading: () => log.restoreLoadings++, loadingDropped: () => {} },
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
      const connectionState = new ConnectionStateStore(ConnectionState.CONNECTED);
      (window as { Vaadin?: unknown }).Vaadin = { connectionState };
      const transitions: string[] = [];
      connectionState.addStateChangeListener((_, state) => transitions.push(state));
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
        connectionState,
        transitions,
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

    for (const type of [ConnectionMessageType.XHR, ConnectionMessageType.HEARTBEAT]) {
      it(`keeps showing a loading request after a ${type} reconnect`, async () => {
        const { machine, transitions, loadingIndicatorStateHandler, setActiveRequest } = makeMachine();
        loadingIndicatorStateHandler.processMessage(null, null);
        loadingIndicatorStateHandler.startLoading();
        setActiveRequest(true);
        await afterDeferred();
        machine.handleRecoverableError(type, type === ConnectionMessageType.XHR ? { rpc: 1 } : null);
        expect(getState()).to.equal(ConnectionState.RECONNECTING);

        // The connection is re-established while the request is still in
        // progress, then its response has been handled.
        machine.resolveTemporaryError(type);
        await afterDeferred();
        expect(getState()).to.equal(ConnectionState.LOADING);
        setActiveRequest(false);
        loadingIndicatorStateHandler.stopLoading();
        await afterDeferred();
        expect(getState()).to.equal(ConnectionState.CONNECTED);
        // Straight from reconnecting to loading, never connected in between.
        expect(transitions).to.deep.equal([
          ConnectionState.LOADING,
          ConnectionState.RECONNECTING,
          ConnectionState.LOADING,
          ConnectionState.CONNECTED
        ]);
      });
    }

    it('shows connected after a request started during a heartbeat reconnect', async () => {
      const { machine, loadingIndicatorStateHandler, setActiveRequest } = makeMachine();
      machine.handleRecoverableError(ConnectionMessageType.HEARTBEAT, null);
      // A heartbeat failure does not hold back requests, so the user's next
      // action is sent and shows loading while the heartbeat reconnects.
      loadingIndicatorStateHandler.processMessage(null, null);
      loadingIndicatorStateHandler.startLoading();
      setActiveRequest(true);
      await afterDeferred();
      expect(getState()).to.equal(ConnectionState.LOADING);

      machine.resolveTemporaryError(ConnectionMessageType.HEARTBEAT);
      await afterDeferred();
      expect(getState()).to.equal(ConnectionState.LOADING);
      setActiveRequest(false);
      loadingIndicatorStateHandler.stopLoading();
      await afterDeferred();
      expect(getState()).to.equal(ConnectionState.CONNECTED);
    });

    for (const type of [ConnectionMessageType.XHR, ConnectionMessageType.HEARTBEAT]) {
      it(`keeps loading started by another party during a ${type} reconnect`, async () => {
        const { machine, connectionState } = makeMachine();
        machine.handleRecoverableError(type, null);
        // E.g. a Hilla endpoint call starts while Flow is reconnecting.
        connectionState.loadingStarted();

        machine.resolveTemporaryError(type);
        await afterDeferred();
        expect(getState()).to.equal(ConnectionState.LOADING);
        connectionState.loadingFinished();
        expect(getState()).to.equal(ConnectionState.CONNECTED);
      });
    }

    // A request finishing after a reconnect, or giving up (e.g. going offline),
    // dropped its loading must not finish the loading of another party.
    for (const type of [ConnectionMessageType.XHR, ConnectionMessageType.HEARTBEAT, 'give-up'] as const) {
      it(`keeps loading started by another party when a request finishes after ${type}`, async () => {
        const { machine, connectionState, loadingIndicatorStateHandler, setActiveRequest } = makeMachine();
        loadingIndicatorStateHandler.processMessage(null, null);
        loadingIndicatorStateHandler.startLoading();
        setActiveRequest(true);
        await afterDeferred();
        if (type === 'give-up') {
          machine.giveUp();
        } else {
          machine.handleRecoverableError(type, null);
        }
        connectionState.loadingStarted();

        setActiveRequest(false);
        loadingIndicatorStateHandler.stopLoading();
        await afterDeferred();
        expect(getState()).to.equal(ConnectionState.LOADING);
      });
    }

    it('shows connected after a reconnect during which another party lost the connection', async () => {
      const { machine, connectionState } = makeMachine();
      machine.handleRecoverableError(ConnectionMessageType.XHR, { rpc: 1 });
      // E.g. a Hilla endpoint call starts and fails while Flow is reconnecting.
      connectionState.loadingStarted();
      connectionState.loadingFailed();
      expect(getState()).to.equal(ConnectionState.CONNECTION_LOST);

      machine.resolveTemporaryError(ConnectionMessageType.XHR);
      await afterDeferred();
      expect(getState()).to.equal(ConnectionState.CONNECTED);
    });
  });
});
