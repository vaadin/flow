import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { StateNode, type StateTree } from '../../main/frontend/internal/StateNode';
import { NodeFeatures, NodeProperties } from '../../main/frontend/internal/nodefeature/NodeFeatures';
import { TextBindingStrategy } from '../../main/frontend/internal/binding/TextBindingStrategy';
import type { BinderContext, BindingStrategy } from '../../main/frontend/internal/binding/BindingStrategy';

const tree: StateTree = {
  getNode: () => null,
  getFeatureDebugName: (id) => String(id),
  isActive: () => true,
  sendNodePropertySyncToServer: () => {}
};

const context: BinderContext = {
  createAndBind: () => document.createTextNode(''),
  bind: () => {},
  getStrategies: <T extends BindingStrategy<Node>>(): T[] => []
};

function setText(node: StateNode, value: string): void {
  node.getMap(NodeFeatures.TEXT_NODE).getProperty(NodeProperties.TEXT).setValue(value);
}

describe('TextBindingStrategy', () => {
  let strategy: TextBindingStrategy;
  beforeEach(() => {
    Reactive.reset();
    strategy = new TextBindingStrategy();
  });

  it('creates an empty text node', () => {
    const text = strategy.create(new StateNode(1, tree));
    expect(text.nodeType).to.equal(Node.TEXT_NODE);
    expect(text.data).to.equal('');
  });

  it('is applicable only when the node has the text feature', () => {
    const node = new StateNode(2, tree);
    expect(strategy.isApplicable(node)).to.equal(false);
    node.getMap(NodeFeatures.TEXT_NODE);
    expect(strategy.isApplicable(node)).to.equal(true);
  });

  it('reactively binds the text property to the DOM node and unbinds on unregister', () => {
    const node = new StateNode(3, tree);
    const text = strategy.create(node);
    setText(node, 'hello');

    strategy.bind(node, text, context);
    Reactive.flush();
    expect(text.data).to.equal('hello');

    setText(node, 'world');
    Reactive.flush();
    expect(text.data).to.equal('world');

    node.unregister();
    setText(node, 'ignored');
    Reactive.flush();
    expect(text.data).to.equal('world');
  });
});
