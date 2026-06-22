import { expect } from '@open-wc/testing';
import { isPropertyDefined } from '../../main/frontend/internal/ExecuteJavaScriptElementUtils';

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
});
