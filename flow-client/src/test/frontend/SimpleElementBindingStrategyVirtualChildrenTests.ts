import { expect } from '@open-wc/testing';
import { Reactive } from '../../main/frontend/internal/reactive/reactive';
import { bindVirtualChildren, BindingContext } from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

const ELEMENT_DATA = 0;
const VIRTUAL_CHILDREN = 24;
const SHADOW_ROOT_DATA = 20;

// Marks an element "ready" so PolymerUtils.isReady returns true (it checks `.$`).
function markReady<T extends Element>(element: T): T {
  (element as any).$ = {};
  return element;
}

// A virtual-child state node with a payload (type + payload) and a recordable
// setDomNode.
function fakeVirtualChild(id: number, payload: Record<string, unknown>): any {
  let domNode: Node | null = null;
  const tree = {
    getRegistry: () => ({
      getInitialPropertiesHandler: () => ({ nodeRegistered: () => {}, flushPropertyUpdates: () => {} })
    }),
    sendExistingElementWithIdAttachToServer: () => {}
  };
  return {
    getId: () => id,
    getDomNode: () => domNode,
    setDomNode: (n: Node | null) => {
      domNode = n;
    },
    hasFeature: () => false,
    getMap: (feature: number) =>
      feature === ELEMENT_DATA
        ? { getProperty: (name: string) => ({ getValue: () => (name === 'payload' ? payload : 'div') }) }
        : { getProperty: () => ({ getValue: () => 'div' }) },
    getList: () => ({ length: () => 0, get: () => undefined }),
    getTree: () => tree
  };
}

// The host node carrying a VIRTUAL_CHILDREN list.
function hostNode(children: any[]): any {
  return {
    hasFeature: (feature: number) => feature !== SHADOW_ROOT_DATA,
    getMap: () => ({ getProperty: () => ({ getValue: () => 'div' }) }),
    getList: (feature: number) =>
      feature === VIRTUAL_CHILDREN
        ? {
            length: () => children.length,
            get: (i: number) => children[i],
            hasBeenCleared: () => false,
            forEach: () => {},
            addSpliceListener: () => ({ remove: () => {} })
          }
        : {
            length: () => 0,
            get: () => undefined,
            hasBeenCleared: () => false,
            forEach: () => {},
            addSpliceListener: () => ({ remove: () => {} })
          },
    getTree: () => ({
      getRegistry: () => ({
        getInitialPropertiesHandler: () => ({ nodeRegistered: () => {}, flushPropertyUpdates: () => {} })
      }),
      sendExistingElementWithIdAttachToServer: () => {}
    })
  };
}

function binderContext() {
  const bound: unknown[] = [];
  return {
    bound,
    ctx: {
      createAndBind: (node: any) => {
        bound.push(node);
        return node.getDomNode();
      },
      bind: () => {},
      getStrategies: () => []
    } as any
  };
}

describe('SimpleElementBindingStrategy virtual children', () => {
  afterEach(() => Reactive.flush());

  it('creates and binds an in-memory virtual child', () => {
    const child = fakeVirtualChild(2, { type: 'inMemory' });
    const { bound, ctx } = binderContext();
    const host = document.createElement('div');
    bindVirtualChildren(new BindingContext(hostNode([child]), host, ctx));
    expect(bound).to.deep.equal([child]);
  });

  it('attaches an inject-by-id virtual child to the existing element and binds it', () => {
    const target = document.createElement('div');
    target.id = 'injected';
    const host = markReady(document.createElement('div'));
    host.appendChild(target);

    const child = fakeVirtualChild(2, { type: '@id', payload: 'injected' });
    const { bound, ctx } = binderContext();
    bindVirtualChildren(new BindingContext(hostNode([child]), host, ctx));

    expect(child.getDomNode()).to.equal(target);
    expect(bound).to.deep.equal([child]);
  });

  it('does not bind when the inject-by-id element is missing', () => {
    const host = markReady(document.createElement('div'));
    const child = fakeVirtualChild(2, { type: '@id', payload: 'missing' });
    const { bound, ctx } = binderContext();
    bindVirtualChildren(new BindingContext(hostNode([child]), host, ctx));
    expect(bound).to.deep.equal([]);
    expect(child.getDomNode()).to.equal(null);
  });
});
