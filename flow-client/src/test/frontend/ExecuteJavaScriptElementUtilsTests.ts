import { expect } from '@open-wc/testing';
import {
  attachExistingElement,
  disposeInitializer,
  isPropertyDefined,
  populateModelProperties,
  registerInitializer,
  registerUpdatableModelProperties,
  resetForTesting
} from '../../main/frontend/internal/ExecuteJavaScriptElementUtils';
import { UpdatableModelProperties } from '../../main/frontend/internal/model/UpdatableModelProperties';

// A MapProperty/NodeMap/StateNode stand-in for populateModelProperties.
function makeModelNode(domNode: unknown, updatable: UpdatableModelProperties | null) {
  const props: Record<string, { value: unknown; hasValue: boolean; synced: unknown }> = {};
  const map = {
    hasPropertyValue: (name: string) => !!props[name]?.hasValue,
    getProperty: (name: string) => {
      props[name] ??= { value: undefined, hasValue: false, synced: undefined };
      return {
        setValue: (value: unknown) => {
          props[name].value = value;
          props[name].hasValue = true;
        },
        syncToServer: (newValue: unknown) => {
          props[name].synced = newValue;
        }
      };
    }
  };
  const node = {
    props,
    getDomNode: () => domNode as Node | null,
    getMap: () => map,
    getNodeData: <T>(_clazz: new (...args: never[]) => T) => updatable as unknown as T | null
  };
  return node;
}

function makeNode() {
  const listeners: Array<() => void> = [];
  return {
    listeners,
    addUnregisterListener: (listener: () => void) => listeners.push(listener),
    fireUnregister: () => listeners.forEach((listener) => listener())
  };
}

describe('ExecuteJavaScriptElementUtils', () => {
  it('isPropertyDefined is true for a declared property with a value', () => {
    const node = {
      constructor: { properties: { foo: { value: 1 }, bar: {} } }
    } as unknown as Node;
    expect(isPropertyDefined(node, 'foo')).to.be.true;
  });

  it('isPropertyDefined is false for a property without a value', () => {
    const node = {
      constructor: { properties: { bar: {} } }
    } as unknown as Node;
    expect(isPropertyDefined(node, 'bar')).to.be.false;
  });

  it('isPropertyDefined is false for missing properties or plain elements', () => {
    const node = {
      constructor: { properties: { foo: { value: 1 } } }
    } as unknown as Node;
    expect(isPropertyDefined(node, 'missing')).to.be.false;
    expect(isPropertyDefined(document.createElement('div'), 'foo')).to.be.false;
  });

  describe('initializer cleanups', () => {
    beforeEach(() => resetForTesting());

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
      node.fireUnregister();
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
      expect(() => node.fireUnregister()).to.not.throw();
      expect(cleaned).to.deep.equal(['2']);
    });
  });

  describe('registerUpdatableModelProperties', () => {
    it('stores an UpdatableModelProperties node data for non-empty properties', () => {
      const stored: object[] = [];
      const node = { setNodeData: (object: object) => stored.push(object) };
      registerUpdatableModelProperties(node, ['first', 'item.value']);
      expect(stored).to.have.length(1);
      const data = stored[0] as UpdatableModelProperties;
      expect(data).to.be.instanceOf(UpdatableModelProperties);
      expect(data.isUpdatableProperty('first')).to.be.true;
      expect(data.isUpdatableProperty('other')).to.be.false;
    });

    it('does nothing for an empty properties array', () => {
      const stored: object[] = [];
      registerUpdatableModelProperties({ setNodeData: (object: object) => stored.push(object) }, []);
      expect(stored).to.deep.equal([]);
    });
  });

  describe('populateModelProperties', () => {
    it('sets null for an undeclared property without a value', () => {
      // Plain element: no declared property and no current value -> setValue(null).
      const node = makeModelNode(document.createElement('div'), null);
      populateModelProperties(node, ['caption']);
      expect(node.props.caption.value).to.equal(null);
      expect(node.props.caption.hasValue).to.be.true;
    });

    it('syncs a declared, updatable property value to the server', () => {
      const element = document.createElement('div');
      // Declare the property (Polymer-style) and give it a runtime value.
      const ctor = { properties: { greeting: { value: '' } } };
      (element as unknown as { constructor: unknown }).constructor = ctor;
      (element as unknown as Record<string, unknown>).greeting = 'hi';

      const node = makeModelNode(element, new UpdatableModelProperties(['greeting']));
      populateModelProperties(node, ['greeting']);
      expect(node.props.greeting.synced).to.equal('hi');
    });

    it('does not sync a declared property that is not updatable', () => {
      const element = document.createElement('div');
      const ctor = { properties: { greeting: { value: '' } } };
      (element as unknown as { constructor: unknown }).constructor = ctor;
      (element as unknown as Record<string, unknown>).greeting = 'hi';

      const node = makeModelNode(element, new UpdatableModelProperties(['other']));
      populateModelProperties(node, ['greeting']);
      expect(node.props.greeting?.synced).to.equal(undefined);
      // (constructor reassigned above via the shared ctor const)
    });
  });

  describe('attachExistingElement', () => {
    // A parent state node over a real DOM element, with an element-children list
    // and a tree that records the attach-to-server call.
    function makeAttachParent(dom: Element, children: Array<{ dom: Node; id: number }>) {
      const sent: unknown[] = [];
      const elementMap = new Map<Element, number>();
      const tree = {
        sent,
        // eslint-disable-next-line @typescript-eslint/max-params -- mirrors the AttachTree contract
        sendExistingElementAttachToServer: (
          _parent: unknown,
          id: number,
          existingId: number,
          tagName: string,
          index: number
        ) => sent.push({ id, existingId, tagName, index }),
        getRegistry: () => ({
          getExistingElementMap: () => ({
            getId: (element: Element) => elementMap.get(element) ?? null,
            add: (id: number, element: Element) => elementMap.set(element, id)
          })
        })
      };
      const list = {
        length: () => children.length,
        get: (index: number) => ({ getDomNode: () => children[index].dom, getId: () => children[index].id })
      };
      const parent = { getDomNode: () => dom, getList: () => list, getTree: () => tree };
      return { parent, tree };
    }

    it('finds an existing element by tag after the previous sibling and reports it', () => {
      const parentDom = document.createElement('div');
      const span = document.createElement('span');
      const button = document.createElement('button');
      parentDom.append(span, button);

      // The span is a known child (state node id 10); the button is the new one.
      const { parent, tree } = makeAttachParent(parentDom, [{ dom: span, id: 10 }]);
      attachExistingElement(parent, span, 'button', 5);

      expect(tree.sent).to.have.length(1);
      expect(tree.sent[0]).to.deep.equal({ id: 5, existingId: 5, tagName: 'BUTTON', index: 1 });
    });

    it('reports -1 when no matching element is found', () => {
      const parentDom = document.createElement('div');
      parentDom.append(document.createElement('span'));
      const { parent, tree } = makeAttachParent(parentDom, []);
      attachExistingElement(parent, null, 'button', 7);
      expect(tree.sent[0]).to.deep.equal({ id: 7, existingId: -1, tagName: 'button', index: -1 });
    });
  });
});
