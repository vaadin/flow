import { expect } from '@open-wc/testing';
import { observe as observePoll } from '../../../../../main/frontend/internal/client/communication/PollConfigurator';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { inertNode } from '../flow/stateTreeTestRegistry';

describe('PollConfigurator', () => {
  it('configures the poller on each poll interval change but not on registration', () => {
    // Ported from listensToProperty.
    const node = inertNode();
    const property = node.getMap(NodeFeatures.POLL_CONFIGURATION).getProperty('pollInterval');
    const intervals: number[] = [];
    observePoll(node, { setInterval: (i) => intervals.push(i) });

    // Observing must not configure the poller until the property changes.
    expect(intervals).to.deep.equal([]);

    // Numbers are always passed as doubles from the server.
    property.setValue(100.0);
    expect(intervals).to.deep.equal([100]);

    property.setValue(-1.0);
    expect(intervals).to.deep.equal([100, -1]);
  });
});
