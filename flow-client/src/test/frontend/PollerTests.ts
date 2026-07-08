import { expect } from '@open-wc/testing';
import { Poller } from '../../main/frontend/internal/communication/Poller';

function makeRegistry() {
  const lifecycleHandlers: Array<(event: { getUiLifecycle(): { isTerminated(): boolean } }) => void> = [];
  const rootNode = { id: 'root' };
  const events: Array<{ node: unknown; type: string; data: unknown }> = [];
  const registry = {
    lifecycleHandlers,
    rootNode,
    events,
    getUILifecycle: () => ({
      addHandler: (handler: (event: { getUiLifecycle(): { isTerminated(): boolean } }) => void) =>
        lifecycleHandlers.push(handler)
    }),
    getStateTree: () => ({
      getRootNode: () => rootNode,
      sendEventToServer: (node: unknown, type: string, data: unknown) => events.push({ node, type, data })
    })
  };
  return registry;
}

describe('Poller', () => {
  it('poll() sends a ui-poll event on the root node', () => {
    const registry = makeRegistry();
    new Poller(registry).poll();
    expect(registry.events).to.deep.equal([{ node: registry.rootNode, type: 'ui-poll', data: null }]);
  });

  it('polls repeatedly at the configured interval, and stops', async () => {
    const registry = makeRegistry();
    const poller = new Poller(registry);
    poller.setInterval(5);
    await new Promise((resolve) => setTimeout(resolve, 25));
    const countWhilePolling = registry.events.length;
    expect(countWhilePolling).to.be.greaterThan(0);

    poller.setInterval(-1); // stop
    await new Promise((resolve) => setTimeout(resolve, 25));
    expect(registry.events.length).to.equal(countWhilePolling);
  });

  it('stops polling when the UI lifecycle terminates', async () => {
    const registry = makeRegistry();
    const poller = new Poller(registry);
    poller.setInterval(5);
    await new Promise((resolve) => setTimeout(resolve, 15));
    registry.lifecycleHandlers.forEach((h) => h({ getUiLifecycle: () => ({ isTerminated: () => true }) }));
    const count = registry.events.length;
    await new Promise((resolve) => setTimeout(resolve, 25));
    expect(registry.events.length).to.equal(count);
  });
});
