import { expect } from '@open-wc/testing';
import { processChange, processChanges } from '../../main/frontend/internal/TreeChangeProcessor';
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
});
