import { expect } from '@open-wc/testing';
import {
  create,
  getNamespace,
  getTag,
  hasSameTag,
  isApplicable,
  isVisible,
  needsRebind
} from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

const ELEMENT_DATA = 0;
const SVG_NS = 'http://www.w3.org/2000/svg';

function fakeNode(
  data: Record<string, unknown>,
  opts: { hasElementData?: boolean; parent?: any; domNode?: Node | null; tree?: any } = {}
): any {
  return {
    getMap: (_feature: number) => ({ getProperty: (name: string) => ({ getValue: () => data[name] }) }),
    hasFeature: (feature: number) => (feature === ELEMENT_DATA ? opts.hasElementData ?? true : false),
    getParent: () => opts.parent ?? null,
    getDomNode: () => opts.domNode ?? null,
    getTree: () => opts.tree ?? null
  };
}

describe('SimpleElementBindingStrategy creation & identity', () => {
  it('getTag and getNamespace read the element data', () => {
    const node = fakeNode({ tag: 'div', namespace: SVG_NS });
    expect(getTag(node)).to.equal('div');
    expect(getNamespace(node)).to.equal(SVG_NS);
    expect(getNamespace(fakeNode({ tag: 'div' }))).to.equal(null);
  });

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

  it('hasSameTag compares case-insensitively and allows a null tag', () => {
    const element = document.createElement('div');
    expect(hasSameTag(fakeNode({ tag: 'DIV' }), element)).to.be.true;
    expect(hasSameTag(fakeNode({ tag: 'span' }), element)).to.be.false;
    expect(hasSameTag(fakeNode({}), element)).to.be.true;
  });

  it('needsRebind is true only for an explicit false bound value', () => {
    expect(needsRebind(fakeNode({ bound: false }))).to.be.true;
    expect(needsRebind(fakeNode({ bound: true }))).to.be.false;
    expect(needsRebind(fakeNode({}))).to.be.false;
  });

  it('isVisible delegates to the tree', () => {
    const node = fakeNode({}, { tree: { getRootNode: () => null, isVisible: () => true } });
    expect(isVisible(node)).to.be.true;
    const hidden = fakeNode({}, { tree: { getRootNode: () => null, isVisible: () => false } });
    expect(isVisible(hidden)).to.be.false;
  });
});
