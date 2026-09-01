// Beyond the Java suite: Poller has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.
import { expect } from '@open-wc/testing';
import { Poller } from '../../../../../main/frontend/internal/client/communication/Poller';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { UILifecycle, UIState } from '../../../../../main/frontend/internal/client/UILifecycle';
import { recordingRegistry } from '../flow/stateTreeTestRegistry';

// Poller reaches the server through a real StateTree, and its lifecycle handler
// through the real UILifecycle, so both are built here rather than faked.
function makeRegistry() {
  const { registry, recorded } = recordingRegistry();
  const tree = new StateTree(registry);
  const uiLifecycle = new UILifecycle();
  registry.register('StateTree', tree).register('UILifecycle', uiLifecycle);
  return { registry, events: recorded.events, tree, uiLifecycle };
}

describe('Poller', () => {
  it('poll() sends a ui-poll event on the root node', () => {
    const registry = makeRegistry();
    new Poller(registry.registry).poll();
    expect(registry.events).to.deep.equal([
      { nodeId: registry.tree.getRootNode().getId(), eventType: 'ui-poll', eventData: null }
    ]);
  });

  it('polls repeatedly at the configured interval, and stops', async () => {
    const registry = makeRegistry();
    const poller = new Poller(registry.registry);
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
    const poller = new Poller(registry.registry);
    poller.setInterval(5);
    await new Promise((resolve) => setTimeout(resolve, 15));
    // Only single forward steps are allowed, so run the UI before terminating it.
    registry.uiLifecycle.setState(UIState.RUNNING);
    registry.uiLifecycle.setState(UIState.TERMINATED);
    const count = registry.events.length;
    await new Promise((resolve) => setTimeout(resolve, 25));
    expect(registry.events.length).to.equal(count);
  });
});
