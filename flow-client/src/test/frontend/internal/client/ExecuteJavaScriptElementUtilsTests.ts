import { expect } from '@open-wc/testing';
import {
  attachExistingElement,
  disposeInitializer,
  populateModelProperties,
  registerInitializer,
  registerUpdatableModelProperties
} from '../../../../main/frontend/internal/client/ExecuteJavaScriptElementUtils';
import { UpdatableModelProperties } from '../../../../main/frontend/internal/client/flow/model/UpdatableModelProperties';
import { StateNode } from '../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../main/frontend/internal/client/flow/StateTree';
import { NodeFeatures } from '../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { type RecordedCalls, recordingRegistry } from './flow/stateTreeTestRegistry';

let nextId = 2;
// The tree the current case works on, plus what it sent to the server.
let tree: StateTree;
let recorded: RecordedCalls;

function makeNode(): StateNode {
  const node = new StateNode(nextId++, tree);
  tree.registerNode(node);
  return node;
}

// A node whose element properties and node data are set up the way
// populateModelProperties reads them.
function makeModelNode(domNode: Node | null, updatable: UpdatableModelProperties | null): StateNode {
  const node = makeNode();
  if (domNode !== null) {
    node.setDomNode(domNode);
  }
  if (updatable !== null) {
    node.setNodeData(updatable);
  }
  return node;
}

// What syncToServer sent, recorded through the tree's server connector.
function syncedValue(node: StateNode, name: string): unknown {
  return recorded.syncs.get(node.getId())?.get(name);
}

// The values populateModelProperties wrote, read back off the node.
function propertyState(node: StateNode, name: string): { value: unknown; hasValue: boolean } {
  const map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
  return { value: map.getProperty(name).getValue(), hasValue: map.hasPropertyValue(name) };
}

// Ported from com.vaadin.client.GwtExecuteJavaScriptElementUtilsTest.
describe('ExecuteJavaScriptElementUtils', () => {
  beforeEach(() => {
    const built = recordingRegistry();
    recorded = built.recorded;
    tree = new StateTree(built.registry);
  });

  describe('initializer cleanups', () => {
    // The registry is keyed by node and each case builds its own, so nothing
    // leaks between them.

    it('invokes the cleanup when an initializer is disposed', () => {
      const node = makeNode();
      const cleaned: string[] = [];
      registerInitializer(node, 1, () => cleaned.push('a'));
      expect(cleaned).to.deep.equal([]);
      disposeInitializer(node, 1);
      expect(cleaned).to.deep.equal(['a']);
      // Disposing again is a no-op.
      disposeInitializer(node, 1);
      expect(cleaned).to.deep.equal(['a']);
    });

    it('invokes the previous cleanup when the same id is re-registered', () => {
      const node = makeNode();
      const cleaned: string[] = [];
      registerInitializer(node, 1, () => cleaned.push('old'));
      registerInitializer(node, 1, () => cleaned.push('new'));
      expect(cleaned).to.deep.equal(['old']); // old invoked on replace
      disposeInitializer(node, 1);
      expect(cleaned).to.deep.equal(['old', 'new']);
    });

    it('drains all cleanups when the node is unregistered', () => {
      const node = makeNode();
      const cleaned: string[] = [];
      registerInitializer(node, 1, () => cleaned.push('1'));
      registerInitializer(node, 2, () => cleaned.push('2'));
      tree.unregisterNode(node);
      expect(cleaned.sort()).to.deep.equal(['1', '2']);
      // After draining, dispose is a no-op (node entry removed).
      disposeInitializer(node, 1);
      expect(cleaned.sort()).to.deep.equal(['1', '2']);
    });

    it('keeps draining even if a cleanup throws', () => {
      const node = makeNode();
      const cleaned: string[] = [];
      registerInitializer(node, 1, () => {
        throw new Error('boom');
      });
      registerInitializer(node, 2, () => cleaned.push('2'));
      expect(() => tree.unregisterNode(node)).to.not.throw();
      expect(cleaned).to.deep.equal(['2']);
    });
  });

  describe('registerUpdatableModelProperties', () => {
    it('stores an UpdatableModelProperties node data for non-empty properties', () => {
      const node = makeNode();
      registerUpdatableModelProperties(node, ['first', 'item.value']);

      const data = node.getNodeData(UpdatableModelProperties);
      expect(data).to.be.instanceOf(UpdatableModelProperties);
      expect(data!.isUpdatableProperty('first')).to.be.true;
      expect(data!.isUpdatableProperty('other')).to.be.false;
    });

    it('does nothing for an empty properties array', () => {
      const node = makeNode();
      registerUpdatableModelProperties(node, []);
      expect(node.getNodeData(UpdatableModelProperties)).to.equal(null);
    });
  });

  describe('populateModelProperties', () => {
    it('sets null for an undeclared property without a value', () => {
      // Plain element: no declared property and no current value -> setValue(null).
      const node = makeModelNode(document.createElement('div'), null);
      populateModelProperties(node, ['caption']);
      expect(propertyState(node, 'caption').value).to.equal(null);
      expect(propertyState(node, 'caption').hasValue).to.be.true;
    });

    it('sets null for a property declared without a value', () => {
      const element = document.createElement('div');
      // Declared Polymer-style but with no `value` entry, so it does not count
      // as defined and is treated like an undeclared property.
      (element as unknown as { constructor: unknown }).constructor = { properties: { caption: {} } };

      const node = makeModelNode(element, null);
      populateModelProperties(node, ['caption']);
      expect(propertyState(node, 'caption').value).to.equal(null);
      expect(propertyState(node, 'caption').hasValue).to.be.true;
    });

    it('syncs a declared, updatable property value to the server', () => {
      const element = document.createElement('div');
      // Declare the property (Polymer-style) and give it a runtime value.
      const ctor = { properties: { greeting: { value: '' } } };
      (element as unknown as { constructor: unknown }).constructor = ctor;
      (element as unknown as Record<string, unknown>).greeting = 'hi';

      const node = makeModelNode(element, new UpdatableModelProperties(['greeting']));
      populateModelProperties(node, ['greeting']);
      expect(syncedValue(node, 'greeting')).to.equal('hi');
    });

    it('does not sync a declared property that is not updatable', () => {
      const element = document.createElement('div');
      const ctor = { properties: { greeting: { value: '' } } };
      (element as unknown as { constructor: unknown }).constructor = ctor;
      (element as unknown as Record<string, unknown>).greeting = 'hi';

      const node = makeModelNode(element, new UpdatableModelProperties(['other']));
      populateModelProperties(node, ['greeting']);
      expect(syncedValue(node, 'greeting')).to.equal(undefined);
      // (constructor reassigned above via the shared ctor const)
    });
  });

  describe('attachExistingElement', () => {
    // A real parent node over a real DOM element, with a real element-children
    // list holding a node per known child.
    function makeAttachParent(dom: Element, children: Array<{ dom: Node; id: number }>): StateNode {
      const parent = makeNode();
      parent.setDomNode(dom);
      const list = parent.getList(NodeFeatures.ELEMENT_CHILDREN);
      children.forEach((child, index) => {
        const childNode = new StateNode(child.id, tree);
        tree.registerNode(childNode);
        childNode.setDomNode(child.dom);
        list.add(index, childNode);
      });
      return parent;
    }

    it('finds an existing element by tag after the previous sibling and reports it', () => {
      const parentDom = document.createElement('div');
      const span = document.createElement('span');
      const button = document.createElement('button');
      parentDom.append(span, button);

      // The span is a known child (state node id 10); the button is the new one.
      const parent = makeAttachParent(parentDom, [{ dom: span, id: 10 }]);
      attachExistingElement(parent, span, 'button', 5);

      expect(recorded.existingElementAttaches).to.have.length(1);
      expect(recorded.existingElementAttaches[0]).to.deep.equal({
        nodeId: parent.getId(),
        id: 5,
        existingId: 5,
        tagName: 'BUTTON',
        index: 1
      });
    });

    it('reports -1 when no matching element is found', () => {
      const parentDom = document.createElement('div');
      parentDom.append(document.createElement('span'));
      const parent = makeAttachParent(parentDom, []);
      attachExistingElement(parent, null, 'button', 7);
      expect(recorded.existingElementAttaches[0]).to.deep.equal({
        nodeId: parent.getId(),
        id: 7,
        existingId: -1,
        tagName: 'button',
        index: -1
      });
    });
  });
});
