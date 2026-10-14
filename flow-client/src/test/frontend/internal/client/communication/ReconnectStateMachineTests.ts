// Beyond the Java suite: ReconnectStateMachine has no Java test class in src/test/java or src/test-gwt/java —
// it is a TypeScript-only split of DefaultConnectionStateHandler, whose Gwt cases drive the handler — so every case here is beyond the Java suite.
import { testRegistry } from '../testRegistry';
import { expect } from '@open-wc/testing';
import { ConnectionMessageType } from '../../../../../main/frontend/internal/client/communication/ConnectionMessageType';
import { ReconnectStateMachine } from '../../../../../main/frontend/internal/client/communication/ReconnectStateMachine';

function makeRegistry(reconnectAttempts = 3) {
  const log = { endRequests: 0, stopLoadings: 0, heartbeatIntervals: [] as number[] };
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
      LoadingIndicatorStateHandler: { stopLoading: () => log.stopLoadings++ },
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

    // The matching resolution clears the state and stops loading (XHR path).
    machine.resolveTemporaryError(ConnectionMessageType.XHR);
    expect(machine.isReconnecting()).to.be.false;
    expect(machine.getReconnectAttempt()).to.equal(0);
    expect(registry.log.stopLoadings).to.equal(1);
    expect(cancels).to.deep.equal([1]);
  });

  it('does nothing when the UI is not running', () => {
    // The lifecycle is the one service this case needs to differ, so it builds
    // its own registry rather than replacing a getter on one.
    const log = { endRequests: 0, stopLoadings: 0, heartbeatIntervals: [] as number[] };
    const registry = testRegistry({
      UILifecycle: { isRunning: () => false },
      ReconnectConfiguration: { getReconnectAttempts: () => 3 },
      RequestResponseTracker: { hasActiveRequest: () => true, endRequest: () => log.endRequests++ },
      LoadingIndicatorStateHandler: { stopLoading: () => log.stopLoadings++ },
      Heartbeat: { setInterval: (i: number) => log.heartbeatIntervals.push(i) }
    });
    const machine = new ReconnectStateMachine(registry, () => {});
    machine.handleRecoverableError(ConnectionMessageType.XHR, null);
    expect(machine.isReconnecting()).to.be.false;
  });
});
