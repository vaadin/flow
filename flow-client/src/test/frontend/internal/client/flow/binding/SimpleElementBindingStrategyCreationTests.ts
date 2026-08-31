import { NodeFeatures } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';
import { expect } from '@open-wc/testing';
import {
  SimpleElementBindingStrategy,
  needsRebind
} from '../../../../../../main/frontend/internal/client/flow/binding/SimpleElementBindingStrategy';
import { makeCollectingTree } from '../bindingTestHelpers';
import { NodeProperties } from '../../../../../../main/frontend/internal/flow/internal/nodefeature/NodeProperties';
import { StateNode } from '../../../../../../main/frontend/internal/client/flow/StateNode';
import { bind } from '../../../../../../main/frontend/internal/client/flow/binding/Binder';

const SVG_NS = 'http://www.w3.org/2000/svg';

function fakeNode(
  data: Record<string, unknown>,
  opts: { hasElementData?: boolean; parent?: any; domNode?: Node | null; tree?: any } = {}
): any {
  return {
    getMap: (_feature: number) => ({ getProperty: (name: string) => ({ getValue: () => data[name] }) }),
    hasFeature: (feature: number) => feature === NodeFeatures.ELEMENT_DATA && (opts.hasElementData ?? true),
    getParent: () => opts.parent ?? null,
    getDomNode: () => opts.domNode ?? null,
    getTree: () => opts.tree ?? null
  };
}

describe('SimpleElementBindingStrategy creation & identity', () => {
  // create and isApplicable are public methods of the strategy, so they are
  // exercised through an instance of it.
  const strategy = new SimpleElementBindingStrategy();
  const create = (node: any): Element => strategy.create(node);
  const isApplicable = (node: any): boolean => strategy.isApplicable(node);

  // Beyond the Java suite: the GWT suite has no case for create or
  // isApplicable, which it only reaches indirectly through Binder.bind.
  describe('beyond the Java suite', () => {
    it('create uses the node namespace when present', () => {
      const element = create(fakeNode({ tag: 'svg', namespace: SVG_NS }));
      expect(element.namespaceURI).to.equal(SVG_NS);
      expect(element.tagName.toLowerCase()).to.equal('svg');
    });

    it('create inherits the parent element namespace', () => {
      const parentDom = document.createElementNS(SVG_NS, 'svg');
      const element = create(fakeNode({ tag: 'rect' }, { parent: fakeNode({}, { domNode: parentDom }) }));
      expect(element.namespaceURI).to.equal(SVG_NS);
    });

    it('create falls back to a plain HTML element', () => {
      const element = create(fakeNode({ tag: 'div' }));
      expect(element.namespaceURI).to.equal('http://www.w3.org/1999/xhtml');
      expect(element.tagName.toLowerCase()).to.equal('div');
    });

    it('isApplicable is true with element data, or for the root node', () => {
      expect(isApplicable(fakeNode({}, { hasElementData: true }))).to.be.true;

      const root = fakeNode({}, { hasElementData: false });
      (root as any).getTree = () => ({ getRootNode: () => root, isVisible: () => true });
      expect(isApplicable(root)).to.be.true;

      const other = fakeNode(
        {},
        { hasElementData: false, tree: { getRootNode: () => fakeNode({}), isVisible: () => true } }
      );
      expect(isApplicable(other)).to.be.false;
    });
  });

  it('binding an element with the wrong tag throws', () => {
    // Ported from testBindWrongTagThrows.
    const harness = makeCollectingTree();
    const node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('span');
    const element = document.createElement('div');

    expect(() => bind(node, element)).to.throw();
  });

  it('binds an element whose tag matches', () => {
    // Ported from testBindRightTagOk.
    const harness = makeCollectingTree();
    const node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).setValue('div');
    const element = document.createElement('div');

    expect(() => bind(node, element)).to.not.throw();
  });

  it('needsRebind flips to true once the element is marked not visibility-bound', () => {
    // Ported from testSimpleElementBindingStrategy_regularElement_needsBind.
    const harness = makeCollectingTree();
    const node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA);

    expect(needsRebind(node)).to.be.false;

    node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY).setValue(false);

    expect(needsRebind(node)).to.be.true;
  });

  it('needsRebind is false for a node without the element-data feature', () => {
    // Ported from testSimpleElementBindingStrategy_elementWithoutFeature_needsBind.
    const harness = makeCollectingTree();
    const node = new StateNode(2, harness.tree);
    harness.tree.registerNode(node);
    node.getMap(NodeFeatures.ELEMENT_DATA);

    const emptyNode = new StateNode(45, harness.tree);
    // self control
    expect(emptyNode.hasFeature(NodeFeatures.ELEMENT_DATA)).to.be.false;

    expect(needsRebind(node)).to.be.false;
  });
});
