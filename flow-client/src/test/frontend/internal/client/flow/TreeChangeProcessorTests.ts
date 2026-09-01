import { inertRegistry } from './stateTreeTestRegistry';
import { expect } from '@open-wc/testing';
import { processChange, processChanges } from '../../../../../main/frontend/internal/client/flow/TreeChangeProcessor';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree, type Registry } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { JsonConstants } from '../../../../../main/frontend/internal/flow/shared/JsonConstants';

type Change = Record<string, unknown>;

function makeTree(): StateTree {
  const registry: Registry = inertRegistry();
  return new StateTree(registry);
}

// Change builders mirroring the private static helpers in
// TreeChangeProcessorTest.java: every wire key and change-type value is taken
// from JsonConstants by name rather than hard-coded.
function baseChange(node: number, type: string): Change {
  return {
    [JsonConstants.CHANGE_TYPE]: type,
    [JsonConstants.CHANGE_NODE]: node
  };
}

function mapBaseChange(node: number, ns: number, type: string, key: string): Change {
  const change = baseChange(node, type);
  change[JsonConstants.CHANGE_FEATURE] = ns;
  change[JsonConstants.CHANGE_MAP_KEY] = key;
  return change;
}

function attachChange(node: number): Change {
  return baseChange(node, JsonConstants.CHANGE_TYPE_ATTACH);
}

function populateChange(node: number, isList: boolean, featureId: number): Change {
  const change = baseChange(node, JsonConstants.CHANGE_TYPE_NOOP);
  change[JsonConstants.CHANGE_FEATURE] = featureId;
  change[JsonConstants.CHANGE_FEATURE_TYPE] = isList;
  return change;
}

function detachChange(node: number): Change {
  return baseChange(node, JsonConstants.CHANGE_TYPE_DETACH);
}

function putChange(node: number, ns: number, key: string, value: unknown): Change {
  const change = mapBaseChange(node, ns, JsonConstants.CHANGE_TYPE_PUT, key);
  change[JsonConstants.CHANGE_PUT_VALUE] = value;
  return change;
}

function removeChange(node: number, ns: number, key: string): Change {
  return mapBaseChange(node, ns, JsonConstants.CHANGE_TYPE_REMOVE, key);
}

function putNodeChange(node: number, ns: number, key: string, child: number): Change {
  const change = mapBaseChange(node, ns, JsonConstants.CHANGE_TYPE_PUT, key);
  change[JsonConstants.CHANGE_PUT_NODE_VALUE] = child;
  return change;
}

function spliceBaseChange(node: number, ns: number, index: number, remove: number): Change {
  const change = baseChange(node, JsonConstants.CHANGE_TYPE_SPLICE);
  change[JsonConstants.CHANGE_FEATURE] = ns;
  change[JsonConstants.CHANGE_SPLICE_INDEX] = index;
  if (remove > 0) {
    change[JsonConstants.CHANGE_SPLICE_REMOVE] = remove;
  }
  return change;
}

// eslint-disable-next-line @typescript-eslint/max-params -- positional params deliberately match the Java helper spliceChange(node, ns, index, remove, add...)
function spliceChange(node: number, ns: number, index: number, remove: number, add: unknown[] = []): Change {
  const change = spliceBaseChange(node, ns, index, remove);
  if (add.length !== 0) {
    change[JsonConstants.CHANGE_SPLICE_ADD] = add;
  }
  return change;
}

// eslint-disable-next-line @typescript-eslint/max-params -- positional params deliberately match the Java helper nodeSpliceChange(node, ns, index, remove, children...)
function nodeSpliceChange(node: number, ns: number, index: number, remove: number, children: number[] = []): Change {
  const change = spliceBaseChange(node, ns, index, remove);
  if (children.length !== 0) {
    change[JsonConstants.CHANGE_SPLICE_ADD_NODES] = children;
  }
  return change;
}

describe('TreeChangeProcessor', () => {
  it('applies a put change with a scalar value', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    const node = processChange(tree, putChange(rootId, ns, 'myKey', 'myValue'));

    const value = tree.getRootNode().getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal('myValue');
    expect(node).to.equal(tree.getRootNode());
  });

  it('ignores a detach for a nonexistent node during resync', () => {
    const tree = makeTree();
    // No node with id 2 is registered, so the detach targets an unknown id;
    // during resync the change is skipped instead of failing an assertion.
    tree.prepareForResync();
    expect(() => processChange(tree, detachChange(2))).to.not.throw();
  });

  it('removes a property value on a remove change', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;
    const property = tree.getRootNode().getMap(ns).getProperty('myKey');
    property.setValue('myValue');

    const node = processChange(tree, removeChange(rootId, ns, 'myKey'));

    expect(property.hasValue()).to.be.false;
    expect(node).to.equal(tree.getRootNode());
  });

  it('re-adds a removed map value with a node value', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;
    const property = tree.getRootNode().getMap(ns).getProperty('myKey');
    property.setValue('myValue');

    processChange(tree, removeChange(rootId, ns, 'myKey'));

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    const node = processChange(tree, putNodeChange(rootId, ns, 'myKey', child.getId()));

    expect(property.getValue()).to.equal(child);
    expect(node).to.equal(tree.getRootNode());
  });

  it('applies a put change with a node value', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    const node = processChange(tree, putNodeChange(rootId, ns, 'myKey', child.getId()));

    const value = tree.getRootNode().getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal(child);
    expect(node).to.equal(tree.getRootNode());
  });

  // Also the counterpart of GwtTreeChangeProcessorTest.testPrimitiveSplice:
  // a CHANGE_SPLICE_ADD of scalar values grows the list.
  it('applies primitive splice changes to a list', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    let node = processChange(tree, spliceChange(rootId, ns, 0, 0, ['foo', 'bar']));
    const list = tree.getRootNode().getList(ns);
    expect(list.length()).to.equal(2);
    expect(list.get(0)).to.equal('foo');
    expect(list.get(1)).to.equal('bar');
    expect(node).to.equal(tree.getRootNode());

    node = processChange(tree, spliceChange(rootId, ns, 1, 0, ['baz']));
    expect(list.length()).to.equal(3);
    expect(list.get(0)).to.equal('foo');
    expect(list.get(1)).to.equal('baz');
    expect(list.get(2)).to.equal('bar');
    expect(node).to.equal(tree.getRootNode());

    node = processChange(tree, spliceChange(rootId, ns, 1, 1));
    expect(list.length()).to.equal(2);
    expect(list.get(0)).to.equal('foo');
    expect(list.get(1)).to.equal('bar');
    expect(node).to.equal(tree.getRootNode());
  });

  it('applies a splice change adding child nodes', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    const node = processChange(tree, nodeSpliceChange(rootId, ns, 0, 0, [child.getId()]));

    const list = tree.getRootNode().getList(ns);
    expect(list.length()).to.equal(1);
    expect(list.get(0)).to.equal(child);
    expect(node).to.equal(tree.getRootNode());
  });

  it('applies a put change before the corresponding attach change', () => {
    const tree = makeTree();
    const nodeId = 2;
    const ns = 0;

    const updatedNodes = processChanges(tree, [putChange(nodeId, ns, 'myKey', 'myValue'), attachChange(nodeId)]);

    const value = tree.getNode(nodeId)!.getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal('myValue');

    expect(updatedNodes.size).to.equal(1);
    expect(updatedNodes.has(tree.getNode(nodeId)!)).to.be.true;
  });

  it('removes a node and notifies its unregister listener on detach', () => {
    const tree = makeTree();
    let unregisterCount = 0;

    const childNode = new StateNode(2, tree);
    childNode.addUnregisterListener(() => {
      unregisterCount++;
    });
    tree.registerNode(childNode);

    expect(tree.getNode(childNode.getId())).to.equal(childNode);
    expect(unregisterCount).to.equal(0);

    const updatedNodes = processChanges(tree, [detachChange(childNode.getId())]);

    expect(tree.getNode(childNode.getId())).to.equal(null);
    expect(unregisterCount).to.equal(1);
    expect(updatedNodes.size).to.equal(1);
    expect(updatedNodes.has(childNode)).to.be.true;
  });

  it('populates a map feature on a noop change', () => {
    const tree = makeTree();
    const node = new StateNode(2, tree);
    tree.registerNode(node);
    const featureId = 11;

    const updatedNode = processChange(tree, populateChange(node.getId(), false, featureId));

    expect(node.hasFeature(featureId)).to.be.true;
    // No assertion error because of a wrong feature instance
    node.getMap(featureId);
    expect(updatedNode).to.equal(node);
  });

  it('populates a list feature on a noop change', () => {
    const tree = makeTree();
    const node = new StateNode(3, tree);
    tree.registerNode(node);
    const featureId = 12;

    const updatedNode = processChange(tree, populateChange(node.getId(), true, featureId));

    expect(node.hasFeature(featureId)).to.be.true;
    // No assertion error because of a wrong feature instance
    node.getList(featureId);
    expect(updatedNode).to.equal(node);
  });

  it('sets the parent of a node put as a map value', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    const node = processChange(tree, putNodeChange(rootId, ns, 'myKey', child.getId()));

    const value = tree.getRootNode().getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal(child);
    expect(node).to.equal(tree.getRootNode());
    expect(child.getParent()).to.equal(node);
  });

  it('assigns correct parents down a node chain', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    const subChild = new StateNode(3, tree);
    tree.registerNode(subChild);
    child.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, child);

    let node = processChange(tree, putNodeChange(rootId, ns, 'myKey', child.getId()));
    expect(node).to.equal(tree.getRootNode());

    const value = tree.getRootNode().getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal(child);
    expect(child.getParent()).to.equal(node);

    node = processChange(tree, putNodeChange(child.getId(), ns, 'myKey', subChild.getId()));
    expect(node).to.equal(child);
    expect(subChild.getParent()).to.equal(child);
  });

  it('clears the parent when a node is detached', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    const node = processChange(tree, putNodeChange(rootId, ns, 'myKey', child.getId()));
    expect(node).to.equal(tree.getRootNode());

    const value = tree.getRootNode().getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal(child);
    expect(child.getParent()).to.equal(node);

    processChange(tree, detachChange(child.getId()));
    expect(child.getParent()).to.equal(null);
  });

  // Beyond the Java suite: no equivalent @Test in TreeChangeProcessorTest.java.
  // Exercises the attach change and the affected-node set returned by
  // processChanges.
  it('attaches new nodes and returns the affected set', () => {
    const tree = makeTree();
    const nodes = processChanges(tree, [attachChange(2)]);
    expect(tree.getNode(2)).to.not.equal(null);
    expect([...nodes].map((n) => n.getId())).to.deep.equal([2]);
  });
});
