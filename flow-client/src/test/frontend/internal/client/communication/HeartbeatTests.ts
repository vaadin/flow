import { expect } from '@open-wc/testing';
import { Heartbeat } from '../../../../../main/frontend/internal/client/communication/Heartbeat';
import { UILifecycle, UIState } from '../../../../../main/frontend/internal/client/UILifecycle';

function makeRegistry(heartbeatInterval: number) {
  // Heartbeat registers its handler on the real UILifecycle, which is ported.
  const uiLifecycle = new UILifecycle();
  const connectionCalls: string[] = [];
  const registry = {
    uiLifecycle,
    connectionCalls,
    getApplicationConfiguration: () => ({
      getHeartbeatInterval: () => heartbeatInterval,
      getServiceUrl: () => '/app',
      getUIId: () => 7
    }),
    getUILifecycle: () => uiLifecycle,
    getConnectionStateHandler: () => ({
      heartbeatOk: () => connectionCalls.push('ok'),
      heartbeatInvalidStatusCode: () => connectionCalls.push('invalid'),
      heartbeatException: () => connectionCalls.push('exception')
    })
  };
  return registry;
}

describe('Heartbeat', () => {
  it('initializes the interval from the application configuration', () => {
    const heartbeat = new Heartbeat(makeRegistry(300));
    expect(heartbeat.getInterval()).to.equal(300);
    heartbeat.setInterval(-1); // cancel the scheduled timer
  });

  it('setInterval(-1) disables the heartbeat', () => {
    const heartbeat = new Heartbeat(makeRegistry(300));
    heartbeat.setInterval(-1);
    expect(heartbeat.getInterval()).to.equal(-1);
  });

  it('disables the heartbeat when the UI lifecycle terminates', () => {
    const registry = makeRegistry(300);
    const heartbeat = new Heartbeat(registry);
    // Only single forward steps are allowed, so run the UI before terminating it.
    registry.uiLifecycle.setState(UIState.RUNNING);
    registry.uiLifecycle.setState(UIState.TERMINATED);
    expect(heartbeat.getInterval()).to.equal(-1);
  });

  it('send() is a no-op (no request, no handler calls) when terminated', () => {
    const registry = makeRegistry(300);
    const heartbeat = new Heartbeat(registry);
    heartbeat.setInterval(-1);
    heartbeat.send();
    expect(registry.connectionCalls).to.deep.equal([]);
  });
});
