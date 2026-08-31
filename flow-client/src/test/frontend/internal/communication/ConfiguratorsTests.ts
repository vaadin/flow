import { expect } from '@open-wc/testing';
import { observe as observeLoadingIndicator } from '../../../../main/frontend/internal/client/communication/LoadingIndicatorConfigurator';
import { observe as observePoll } from '../../../../main/frontend/internal/client/communication/PollConfigurator';
import { NodeFeatures } from '../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { StateNode } from '../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../main/frontend/internal/client/flow/StateTree';
import { Reactive } from '../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { inertRegistry } from '../client/flow/stateTreeTestRegistry';

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

describe('LoadingIndicatorConfigurator', () => {
  afterEach(() => {
    delete (window as { Vaadin?: unknown }).Vaadin;
    Reactive.flush();
  });

  it('applies delay and theme properties to the connection indicator', () => {
    const indicator: Record<string, unknown> = {};
    (window as { Vaadin?: unknown }).Vaadin = { connectionIndicator: indicator };

    const node = makeNode();
    const configMap = node.getMap(NodeFeatures.LOADING_INDICATOR_CONFIGURATION);
    observeLoadingIndicator(node);

    configMap.getProperty('first').setValue(100);
    configMap.getProperty('second').setValue(200);
    configMap.getProperty('third').setValue(300);
    configMap.getProperty('theme').setValue(false);

    expect(indicator).to.deep.equal({
      firstDelay: 100,
      secondDelay: 200,
      thirdDelay: 300,
      applyDefaultTheme: false
    });
  });

  describe('beyond the Java suite', () => {
    it('falls back to the default when a property is cleared', () => {
      const indicator: Record<string, unknown> = {};
      (window as { Vaadin?: unknown }).Vaadin = { connectionIndicator: indicator };

      const node = makeNode();
      const configMap = node.getMap(NodeFeatures.LOADING_INDICATOR_CONFIGURATION);
      observeLoadingIndicator(node);

      configMap.getProperty('first').setValue(100);
      configMap.getProperty('first').removeValue();

      expect(indicator.firstDelay).to.equal(450);
    });
  });
});
