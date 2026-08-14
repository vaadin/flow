import { expect } from '@open-wc/testing';
import { clear, isEmpty, pushArray, remove, removeItem, spliceArray } from '../../main/frontend/internal/JsArray';

describe('JsArray helpers', () => {
  it('pushArray appends all values and returns the new length', () => {
    const arr = [1, 2];
    const len = pushArray(arr, [3, 4, 5]);
    expect(len).to.equal(5);
    expect(arr).to.deep.equal([1, 2, 3, 4, 5]);
  });

  it('pushArray with no values leaves the array unchanged', () => {
    const arr = [1];
    expect(pushArray(arr, [])).to.equal(1);
    expect(arr).to.deep.equal([1]);
  });

  it('spliceArray removes and inserts, returning the removed elements', () => {
    const arr = ['a', 'b', 'c', 'd'];
    const removed = spliceArray(arr, 1, 2, ['x', 'y', 'z']);
    expect(removed).to.deep.equal(['b', 'c']);
    expect(arr).to.deep.equal(['a', 'x', 'y', 'z', 'd']);
  });

  it('spliceArray with no additions just removes', () => {
    const arr = [1, 2, 3];
    expect(spliceArray(arr, 0, 1, [])).to.deep.equal([1]);
    expect(arr).to.deep.equal([2, 3]);
  });

  it('clear empties the array', () => {
    const arr = [1, 2, 3];
    clear(arr);
    expect(arr).to.deep.equal([]);
  });

  it('isEmpty reflects whether the array has elements', () => {
    expect(isEmpty([])).to.equal(true);
    expect(isEmpty([1])).to.equal(false);
  });

  it('remove returns the removed item and mutates the array', () => {
    const arr = ['a', 'b', 'c'];
    expect(remove(arr, 1)).to.equal('b');
    expect(arr).to.deep.equal(['a', 'c']);
  });

  it('removeItem removes the first matching item and returns true', () => {
    const arr = [1, 2, 3, 2];
    expect(removeItem(arr, 2)).to.equal(true);
    expect(arr).to.deep.equal([1, 3, 2]);
  });

  it('removeItem returns false when the item is absent', () => {
    const arr = [1, 2, 3];
    expect(removeItem(arr, 9)).to.equal(false);
    expect(arr).to.deep.equal([1, 2, 3]);
  });
});
