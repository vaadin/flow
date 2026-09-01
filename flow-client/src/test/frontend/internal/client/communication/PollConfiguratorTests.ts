import { expect } from '@open-wc/testing';
import { observe as observePoll } from '../../../../../main/frontend/internal/client/communication/PollConfigurator';
import { Poller } from '../../../../../main/frontend/internal/client/communication/Poller';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { inertNode } from '../flow/stateTreeTestRegistry';
import { UILifecycle } from '../../../../../main/frontend/internal/client/UILifecycle';
import { testRegistry } from '../testRegistry';

// The configurator reconfigures the ported Poller, so the case drives a real one
// and records what it was told through the method under observation.
class RecordingPoller extends Poller {
  readonly intervals: number[] = [];

  override setInterval(interval: number): void {
    this.intervals.push(interval);
    super.setInterval(interval);
  }
}

describe('PollConfigurator', () => {
  it('configures the poller on each poll interval change but not on registration', () => {
    // Ported from listensToProperty.
    const node = inertNode();
    const property = node.getMap(NodeFeatures.POLL_CONFIGURATION).getProperty('pollInterval');
    const poller = new RecordingPoller(testRegistry({ StateTree: node.getTree(), UILifecycle: new UILifecycle() }));
    const { intervals } = poller;
    observePoll(node, poller);

    // Observing must not configure the poller until the property changes.
    expect(intervals).to.deep.equal([]);

    // Numbers are always passed as doubles from the server.
    property.setValue(100.0);
    expect(intervals).to.deep.equal([100]);

    property.setValue(-1.0);
    expect(intervals).to.deep.equal([100, -1]);
    // -1 stops the real poller, so nothing is left running after the case.
  });
});
