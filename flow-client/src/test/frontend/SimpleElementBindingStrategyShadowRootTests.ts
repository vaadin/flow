import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { bindShadowRoot, BindingContext } from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

const ELEMENT_CHILDREN = 2;
const SHADOW_ROOT_DATA = 20;

const noExistingMap = { getElement: () => null, remove: () => {} };

function fakeChild(id: number): any {
  let domNode: Node | null = null;
  return {
    getId: () => id,
    getDomNode: () => domNode,
    setDomNode: (n: Node | null) => {
      domNode = n;
    },
    getTree: () => ({ getRegistry: () => ({ getExistingElementMap: () => noExistingMap }) })
  };
}

// A node carrying a SHADOW_ROOT_DATA map and (for the shadow node) an
// ELEMENT_CHILDREN list.
function fakeNode(config: { shadowRootNode?: any; children?: any[] }): any {
  const shadowProperty = { getValue: () => config.shadowRootNode ?? null };
  const shadowMap = { getProperty: () => shadowProperty, addPropertyAddListener: () => ({ remove: () => {} }) };
  const childList = {
    length: () => (config.children ?? []).length,
    get: (i: number) => (config.children ?? [])[i],
    hasBeenCleared: () => false,
    forEach: () => {},
    addSpliceListener: () => ({ remove: () => {} })
  };
  let domNode: Node | null = null;
  return {
    getDomNode: () => domNode,
    setDomNode: (n: Node | null) => {
      domNode = n;
    },
    getMap: (feature: number) => (feature === SHADOW_ROOT_DATA ? shadowMap : {}),
    getList: (feature: number) => (feature === ELEMENT_CHILDREN ? childList : childList)
  };
}

function binderContext(): any {
  return {
    createAndBind: (child: any) => {
      let el = child.getDomNode();
      if (el === null) {
        el = document.createElement('span');
        child.setDomNode(el);
      }
      return el;
    },
    bind: () => {},
    getStrategies: () => []
  };
}

describe('SimpleElementBindingStrategy shadow root binding', () => {
  afterEach(() => Reactive.flush());

  it('attaches an open shadow root and binds its children', () => {
    const element = document.createElement('div');
    const shadowRootNode = fakeNode({ children: [fakeChild(2)] });
    const node = fakeNode({ shadowRootNode });
    bindShadowRoot(new BindingContext(node, element, binderContext()));

    expect(element.shadowRoot).to.not.equal(null);
    expect(shadowRootNode.getDomNode()).to.equal(element.shadowRoot);
    expect(element.shadowRoot!.children).to.have.length(1);
  });

  it('does nothing when there is no shadow root node', () => {
    const element = document.createElement('div');
    bindShadowRoot(new BindingContext(fakeNode({}), element, binderContext()));
    expect(element.shadowRoot).to.equal(null);
  });
});
