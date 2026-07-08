import { expect } from '@open-wc/testing';
import { updateProperty } from '../../main/frontend/internal/binding/SimpleElementBindingStrategy';

function fakeProperty(config: { name: string; hasValue: boolean; value?: unknown; previousDomValue?: unknown }) {
  const cleared: boolean[] = [];
  return {
    cleared,
    getName: () => config.name,
    hasValue: () => config.hasValue,
    getValue: () => config.value,
    getPreviousDomValue: () => config.previousDomValue,
    clearPreviousDomValue: () => cleared.push(true)
  };
}

describe('SimpleElementBindingStrategy property binding', () => {
  it('sets the element property from the tree value', () => {
    const element = document.createElement('div');
    const property = fakeProperty({ name: 'foo', hasValue: true, value: 'bar' });
    updateProperty(property, element);
    expect((element as any).foo).to.equal('bar');
    expect(property.cleared).to.deep.equal([true]);
  });

  it('does not overwrite when the previous DOM value already matches the tree value', () => {
    const element = document.createElement('div');
    const property = fakeProperty({ name: 'foo', hasValue: true, value: 'bar', previousDomValue: 'bar' });
    updateProperty(property, element);
    expect((element as any).foo).to.equal(undefined);
  });

  it('deletes an own property when the value is removed', () => {
    const element = document.createElement('div');
    (element as any).foo = 'x';
    const property = fakeProperty({ name: 'foo', hasValue: false });
    updateProperty(property, element);
    expect('foo' in element).to.be.false;
  });

  it('clears a non-own property to null when the value is removed', () => {
    const element = document.createElement('div');
    const property = fakeProperty({ name: 'customThing', hasValue: false });
    updateProperty(property, element);
    expect((element as any).customThing).to.equal(null);
  });
});
