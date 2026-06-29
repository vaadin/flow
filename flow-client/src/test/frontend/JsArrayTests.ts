import { expect } from '@open-wc/testing';
import { clear, pushArray, spliceArray } from '../../main/frontend/internal/JsArray';

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
});
