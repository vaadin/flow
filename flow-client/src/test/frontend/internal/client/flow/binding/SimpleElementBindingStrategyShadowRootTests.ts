import { expect } from '@open-wc/testing';
import { Reactive } from '../../../../../../main/frontend/internal/client/flow/reactive/Reactive';
import { BindGuardStateNode, type CollectingTree, makeCollectingTree } from '../bindingTestHelpers';
import { NodeFeatures } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeProperties';
import { StateNode } from '../../../../../../main/frontend/internal/client/flow/StateNode';
import { bind } from '../../../../../../main/frontend/internal/client/flow/binding/Binder';

// Shadow root binding is exercised through a bound element: the SHADOW_ROOT_DATA
// property carries the state node of the shadow root, whose children end up in
// the element's real shadow root.
describe('SimpleElementBindingStrategy shadow root binding', () => {
  let harness: CollectingTree;
  let node: StateNode;
  let element: HTMLElement;

  beforeEach(() => {
    Reactive.reset();
    harness = makeCollectingTree();
    node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA);
    element = document.createElement('div');
    node.setDomNode(element);
  });

  afterEach(() => Reactive.flush());

  // Beyond the Java suite: the GWT suite reaches the shadow root only through
  // the virtual child and double-bind cases, and has no test for attaching it.
  describe('beyond the Java suite', () => {
    it('attaches an open shadow root and binds its children', () => {
      const shadowRootNode = new StateNode(3, harness.tree);
      harness.tree.registerNode(shadowRootNode);
      const shadowChild = new StateNode(4, harness.tree);
      harness.tree.registerNode(shadowChild);
      shadowChild.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('span');
      shadowRootNode.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, shadowChild);

      node.getMap(NodeFeatures.SHADOW_ROOT_DATA).getProperty(NodeProperties.SHADOW_ROOT).setValue(shadowRootNode);

      bind(node, element);
      Reactive.flush();

      expect(element.shadowRoot).to.not.equal(null);
      expect(shadowRootNode.getDomNode()).to.equal(element.shadowRoot);
      expect(element.shadowRoot!.children).to.have.length(1);
      expect(element.shadowRoot!.children[0].tagName).to.equal('SPAN');
    });

    it('attaches no shadow root when there is no shadow root node', () => {
      bind(node, element);
      Reactive.flush();

      expect(element.shadowRoot).to.equal(null);
    });
  });

  // Ported from GwtMultipleBindingTest.testBindShadowRootDoubleBind: a second
  // bind must not re-read the shadow-root feature.
  it('binding twice does not re-read the shadow-root feature', () => {
    Reactive.reset();
    const { tree } = makeCollectingTree();
    const node = new BindGuardStateNode(2, tree, (m) => expect.fail(m));
    node.getMap(NodeFeatures.ELEMENT_DATA);
    const element = document.createElement('div');

    bind(node, element);

    const shadowChild = new StateNode(3, tree);
    shadowChild.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
    node.getMap(NodeFeatures.SHADOW_ROOT_DATA).getProperty(NodeProperties.SHADOW_ROOT).setValue(shadowChild);
    Reactive.flush();

    node.setBound();
    bind(node, element);
    Reactive.flush();
  });
});
