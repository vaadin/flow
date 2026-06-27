import { expect } from '@open-wc/testing';
import {
  disposeInitializer,
  isPropertyDefined,
  registerInitializer,
  registerUpdatableModelProperties,
  resetForTesting
} from '../../main/frontend/internal/ExecuteJavaScriptElementUtils';
import { UpdatableModelProperties } from '../../main/frontend/internal/model/UpdatableModelProperties';

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
});
