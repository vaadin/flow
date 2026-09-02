import { expect } from '@open-wc/testing';
import { testRegistry } from './testRegistry';
import { InitialPropertiesHandler } from '../../../../main/frontend/internal/client/InitialPropertiesHandler';
import { StateNode } from '../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../main/frontend/internal/client/flow/StateTree';
import { Reactive } from '../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { NodeFeatures } from '../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { type RecordedCalls, recordingRegistry } from './flow/stateTreeTestRegistry';

// Ported from com.vaadin.client.InitialPropertiesHandlerTest.
describe('InitialPropertiesHandler', () => {
  let tree: StateTree;
  let recorded: RecordedCalls;
  let handler: InitialPropertiesHandler;
  let updateInProgress: boolean;

  beforeEach(() => {
    Reactive.reset();
    const built = recordingRegistry();
    recorded = built.recorded;
    tree = new StateTree(built.registry);
    // The handler asks its registry for the tree; the tree is the real one, so
    // the update-in-progress flag is set on it directly.
    handler = new InitialPropertiesHandler(testRegistry({ StateTree: tree }));
    updateInProgress = false;
  });

  // A node whose element-properties feature holds the given server values.
  function makeNode(id: number, initialProps?: Record<string, unknown>): StateNode {
    const node = new StateNode(id, tree);
    tree.registerNode(node);
    if (initialProps !== undefined) {
      const map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
      Object.entries(initialProps).forEach(([name, value]) => map.getProperty(name).setValue(value));
    }
    return node;
  }

  function property(node: StateNode, name: string, value: unknown) {
    const mapProperty = node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty(name);
    mapProperty.setValue(value);
    return mapProperty;
  }

  function setUpdateInProgress(value: boolean): void {
    updateInProgress = value;
    tree.setUpdateInProgress(value);
  }

  it('queues property updates only for newly created nodes', () => {
    // Ported from flushPropertyUpdates_updateIsNotInProgress_collectInitialProperties.
    const node = makeNode(2, {});

    // Not registered with the handler yet, so the caller sends it normally.
    expect(handler.handlePropertyUpdate(property(node, 'foo', 'x'))).to.be.false;

    handler.nodeRegistered(node);
    expect(handler.handlePropertyUpdate(property(node, 'foo', 'x'))).to.be.true;
  });

  it('does nothing while a server update is in progress', () => {
    // Ported from flushPropertyUpdates_updateInProgress_noInteractions.
    setUpdateInProgress(true);

    handler.flushPropertyUpdates();
    Reactive.flush();

    // Nothing was collected, so nothing is reset or sent.
    expect(recorded.syncs.size).to.equal(0);
    setUpdateInProgress(false);
  });

  it('resets properties with a server initial value and sends the rest', () => {
    // Ported from flushPropertyUpdates_updateIsNotInProgress_flushForEechProperty.
    // Only 'color' arrives from the server.
    const node = makeNode(2, { color: 'red' });
    handler.nodeRegistered(node);

    // The initial values are collected here, before the client changes them.
    handler.flushPropertyUpdates();

    const colorProperty = property(node, 'color', 'blue');
    const sizeProperty = property(node, 'size', 'L');
    handler.handlePropertyUpdate(colorProperty);
    handler.handlePropertyUpdate(sizeProperty);

    Reactive.flush();

    // 'color' had a server initial value, so it is reset to it and not sent.
    expect(colorProperty.getValue()).to.equal('red');
    // 'size' had none, so it goes to the server.
    expect(recorded.syncs.get(node.getId())?.has('size')).to.equal(true);
    expect(recorded.syncs.get(node.getId())?.has('color')).to.not.equal(true);
    expect(updateInProgress).to.be.false;
  });
});
