import { expect } from '@open-wc/testing';
import { observe as observePoll } from '../../../../../main/frontend/internal/client/communication/PollConfigurator';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { inertRegistry } from '../flow/stateTreeTestRegistry';

// Both configurators observe a real StateNode: the change events they react to
// are the ones a real MapProperty fires when the server sets a value.
function makeNode(): StateNode {
  const tree = new StateTree(inertRegistry());
  const node = new StateNode(2, tree);
  tree.registerNode(node);
  return node;
}

describe('PollConfigurator', () => {
  it('configures the poller on each poll interval change but not on registration', () => {
    const node = makeNode();
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
