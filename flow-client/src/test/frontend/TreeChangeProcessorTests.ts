import { expect } from '@open-wc/testing';
import { processChange, processChanges } from '../../main/frontend/internal/TreeChangeProcessor';
import { StateNode } from '../../main/frontend/internal/StateNode';
import { StateTree } from '../../main/frontend/internal/StateTree';

const ELEMENT_PROPERTIES = 1;
const ELEMENT_CHILDREN = 2;

function makeTree(): StateTree {
  const registry: any = {
    getInitialPropertiesHandler: () => ({
      flushPropertyUpdates: () => {},
      nodeRegistered: () => {},
      handlePropertyUpdate: () => false
    }),
    getServerConnector: () => ({})
  };
  return new StateTree(registry);
}

describe('TreeChangeProcessor', () => {
  it('attaches new nodes and returns the affected set', () => {
    const tree = makeTree();
    const nodes = processChanges(tree, [{ type: 'attach', node: 2 }]);
    expect(tree.getNode(2)).to.not.equal(null);
    expect([...nodes].map((n) => n.getId())).to.deep.equal([2]);
  });

  it('applies a put change with a scalar value', () => {
    const tree = makeTree();
    processChanges(tree, [
      { type: 'attach', node: 2 },
      { type: 'put', node: 2, feat: ELEMENT_PROPERTIES, key: 'title', value: 'hi' }
    ]);
    expect(tree.getNode(2)!.getMap(ELEMENT_PROPERTIES).getProperty('title').getValue()).to.equal('hi');
  });

  it('applies a put change with a node value and sets the parent', () => {
    const tree = makeTree();
    processChanges(tree, [
      { type: 'attach', node: 2 },
      { type: 'attach', node: 3 },
      { type: 'put', node: 2, feat: ELEMENT_PROPERTIES, key: 'child', nodeValue: 3 }
    ]);
    const parent = tree.getNode(2)!;
    const child = tree.getNode(3)!;
    expect(parent.getMap(ELEMENT_PROPERTIES).getProperty('child').getValue()).to.equal(child);
    expect(child.getParent()).to.equal(parent);
  });

  it('applies a splice change adding child nodes', () => {
    const tree = makeTree();
    processChanges(tree, [
      { type: 'attach', node: 2 },
      { type: 'attach', node: 3 },
      { type: 'splice', node: 2, feat: ELEMENT_CHILDREN, index: 0, addNodes: [3] }
    ]);
    const list = tree.getNode(2)!.getList(ELEMENT_CHILDREN);
    expect(list.length()).to.equal(1);
    expect(list.get(0)).to.equal(tree.getNode(3));
  });

  it('removes a property value on a remove change', () => {
    const tree = makeTree();
    processChanges(tree, [
      { type: 'attach', node: 2 },
      { type: 'put', node: 2, feat: ELEMENT_PROPERTIES, key: 'title', value: 'hi' }
    ]);
    processChange(tree, { type: 'remove', node: 2, feat: ELEMENT_PROPERTIES, key: 'title' });
    expect(tree.getNode(2)!.getMap(ELEMENT_PROPERTIES).getProperty('title').hasValue()).to.be.false;
  });

  it('detaches a node on a detach change', () => {
    const tree = makeTree();
    processChanges(tree, [{ type: 'attach', node: 2 }]);
    expect(tree.getNode(2)).to.not.equal(null);
    processChange(tree, { type: 'detach', node: 2 });
    expect(tree.getNode(2)).to.equal(null);
  });

  it('applies primitive splice changes to a list', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    let node = processChange(tree, { type: 'splice', node: rootId, feat: ns, index: 0, add: ['foo', 'bar'] });
    const list = tree.getRootNode().getList(ns);
    expect(list.length()).to.equal(2);
    expect(list.get(0)).to.equal('foo');
    expect(list.get(1)).to.equal('bar');
    expect(node).to.equal(tree.getRootNode());

    node = processChange(tree, { type: 'splice', node: rootId, feat: ns, index: 1, add: ['baz'] });
    expect(list.length()).to.equal(3);
    expect(list.get(0)).to.equal('foo');
    expect(list.get(1)).to.equal('baz');
    expect(list.get(2)).to.equal('bar');
    expect(node).to.equal(tree.getRootNode());

    node = processChange(tree, { type: 'splice', node: rootId, feat: ns, index: 1, remove: 1 });
    expect(list.length()).to.equal(2);
    expect(list.get(0)).to.equal('foo');
    expect(list.get(1)).to.equal('bar');
    expect(node).to.equal(tree.getRootNode());
  });

  it('populates a map feature on a noop change', () => {
    const tree = makeTree();
    const node = new StateNode(2, tree);
    tree.registerNode(node);
    const featureId = 11;

    const updatedNode = processChange(tree, { type: 'empty', node: 2, feat: featureId, featType: false });

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

    const updatedNode = processChange(tree, { type: 'empty', node: 3, feat: featureId, featType: true });

    expect(node.hasFeature(featureId)).to.be.true;
    // No assertion error because of a wrong feature instance
    node.getList(featureId);
    expect(updatedNode).to.equal(node);
  });

  it('ignores a detach for a nonexistent node during resync', () => {
    const tree = makeTree();
    // Register the node but never in the tree map, so the detach targets an
    // unknown id
    tree.prepareForResync();
    expect(() => processChange(tree, { type: 'detach', node: 2 })).to.not.throw();
  });

  it('re-adds a removed map value with a node value', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;
    const property = tree.getRootNode().getMap(ns).getProperty('myKey');
    property.setValue('myValue');

    processChange(tree, { type: 'remove', node: rootId, feat: ns, key: 'myKey' });

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    const node = processChange(tree, { type: 'put', node: rootId, feat: ns, key: 'myKey', nodeValue: 2 });

    expect(property.getValue()).to.equal(child);
    expect(node).to.equal(tree.getRootNode());
  });

  it('assigns correct parents down a node chain', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    const subChild = new StateNode(3, tree);
    tree.registerNode(subChild);
    child.getList(ELEMENT_CHILDREN).add(0, child);

    let node = processChange(tree, { type: 'put', node: rootId, feat: ns, key: 'myKey', nodeValue: 2 });
    expect(node).to.equal(tree.getRootNode());

    const value = tree.getRootNode().getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal(child);
    expect(child.getParent()).to.equal(node);

    node = processChange(tree, { type: 'put', node: child.getId(), feat: ns, key: 'myKey', nodeValue: 3 });
    expect(node).to.equal(child);
    expect(subChild.getParent()).to.equal(child);
  });

  it('applies a put change before the corresponding attach change', () => {
    const tree = makeTree();
    const nodeId = 2;
    const ns = 0;

    const updatedNodes = processChanges(tree, [
      { type: 'put', node: nodeId, feat: ns, key: 'myKey', value: 'myValue' },
      { type: 'attach', node: nodeId }
    ]);

    const value = tree.getNode(nodeId)!.getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal('myValue');

    expect(updatedNodes.size).to.equal(1);
    expect(updatedNodes.has(tree.getNode(nodeId)!)).to.be.true;
  });

  it('clears the parent when a node is detached', () => {
    const tree = makeTree();
    const rootId = tree.getRootNode().getId();
    const ns = 0;

    const child = new StateNode(2, tree);
    tree.registerNode(child);

    let node = processChange(tree, { type: 'put', node: rootId, feat: ns, key: 'myKey', nodeValue: 2 });
    expect(node).to.equal(tree.getRootNode());

    const value = tree.getRootNode().getMap(ns).getProperty('myKey').getValue();
    expect(value).to.equal(child);
    expect(child.getParent()).to.equal(node);

    processChange(tree, { type: 'detach', node: child.getId() });
    expect(child.getParent()).to.equal(null);
  });
});
