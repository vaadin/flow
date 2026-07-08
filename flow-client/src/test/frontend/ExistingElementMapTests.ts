import { expect } from '@open-wc/testing';
import { ExistingElementMap } from '../../main/frontend/internal/ExistingElementMap';

describe('ExistingElementMap', () => {
  it('maps an id to an element and back after add', () => {
    const map = new ExistingElementMap();
    const element = document.createElement('div');
    map.add(5, element);
    expect(map.getElement(5)).to.equal(element);
    expect(map.getId(element)).to.equal(5);
  });

  it('returns null for unknown ids and elements', () => {
    const map = new ExistingElementMap();
    expect(map.getElement(0)).to.equal(null);
    expect(map.getId(document.createElement('span'))).to.equal(null);
  });

  it('clears both directions on remove', () => {
    const map = new ExistingElementMap();
    const element = document.createElement('div');
    map.add(3, element);
    map.remove(3);
    expect(map.getElement(3)).to.equal(null);
    expect(map.getId(element)).to.equal(null);
    // Removing an unknown id is a no-op.
    expect(() => map.remove(99)).to.not.throw();
  });
});
