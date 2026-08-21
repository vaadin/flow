import { expect } from '@open-wc/testing';
import {
  clear,
  isEmpty,
  pushArray,
  remove,
  removeItem,
  spliceArray
} from '../../../../../main/frontend/internal/client/flow/collection/JsArray';

describe('JsArray helpers', () => {
  // Cases beyond the Java suite: GwtJsArrayTest covers the JsArray type
  // itself; these exercise the @JsOverlay helper functions the port exposes.
  describe('beyond the Java suite', () => {
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

  // The cases below mirror GwtJsArrayTest.java. The Java JsArray methods
  // get/set/push/length/splice/shift/forEach have no wrapper in the TS port
  // because they map directly to native array syntax (see JsArray.ts); they are
  // exercised here through that native syntax to keep coverage 1:1 with Java.
  // testCanCast is intentionally not ported: WidgetUtil.crazyJsCast is a
  // GWT-compiler-only trick with no runtime/type equivalent in TypeScript.
  describe('JsArray native operations', () => {});

  it('testArray - length, push, get and set', () => {
    const array: string[] = [];
    expect(array.length).to.equal(0);
    array.push('foo');
    expect(array.length).to.equal(1);
    expect(array[0]).to.equal('foo');
    array[0] = 'bar';
    expect(array[0]).to.equal('bar');
  });

  it('testArrayWithValues', () => {
    const array = ['1', '2'];
    expect(array).to.deep.equal(['1', '2']);
  });

  it('testAppendUsingSet', () => {
    const array: string[] = [];
    array[0] = '0';
    expect(array).to.deep.equal(['0']);
    array[1] = '1';
    expect(array).to.deep.equal(['0', '1']);
  });

  it('testArrayRemove', () => {
    const array = ['1', '2', '3'];
    remove(array, 1);
    expect(array).to.deep.equal(['1', '3']);
    remove(array, 1);
    expect(array).to.deep.equal(['1']);
    remove(array, 0);
    expect(array).to.deep.equal([]);
  });

  it('testArrayClear', () => {
    const array = ['1', '2', '3'];
    clear(array);
    expect(array).to.deep.equal([]);
  });

  it('testEmptyArrayClear', () => {
    const array: string[] = [];
    clear(array);
    expect(array).to.deep.equal([]);
  });

  it('testArrayIsEmpty', () => {
    const array: string[] = [];
    expect(isEmpty(array)).to.be.true;
    array.push('1');
    expect(isEmpty(array)).to.be.false;
    array.push('2');
    expect(isEmpty(array)).to.be.false;
    remove(array, 0);
    expect(isEmpty(array)).to.be.false;
    remove(array, 0);
    expect(isEmpty(array)).to.be.true;
  });

  it('testArraySplice', () => {
    const array: string[] = [];
    // 1, 2
    array.splice(0, 0, '1', '2');
    // 1, 1.3, 1.7, 2
    const noneRemoved = array.splice(1, 0, '1.3', '1.7');
    expect(noneRemoved.length).to.equal(0);
    expect(array.length).to.equal(4);
    expect(array[1]).to.equal('1.3');
    // 1, 2
    const twoRemoved = array.splice(1, 2);
    expect(twoRemoved.length).to.equal(2);
    expect(twoRemoved[1]).to.equal('1.7');
    expect(array.length).to.equal(2);
    expect(array[1]).to.equal('2');
  });

  it('testArraySpliceArray', () => {
    const array: unknown[] = ['1', '2'];
    spliceArray(array, 1, 1, ['3', '4']);
    expect(array).to.deep.equal(['1', '3', '4']);
  });

  it('testArrayPush', () => {
    const array: unknown[] = [];
    const source = ['1', '2'];
    pushArray(array, source);
    expect(array).to.deep.equal(['1', '2']);
    pushArray(array, source);
    expect(array).to.deep.equal(['1', '2', '1', '2']);
    array.push('3', '4');
    expect(array).to.deep.equal(['1', '2', '1', '2', '3', '4']);
  });

  it('testArrayPushSelf', () => {
    const array: unknown[] = ['1', '2'];
    // Spread evaluates the current contents before push runs, so pushing the
    // array onto itself appends a snapshot, matching JsArray.pushArray(self).
    pushArray(array, array);
    expect(array).to.deep.equal(['1', '2', '1', '2']);
  });

  it('testShift', () => {
    const array = ['1', '2', '3'];
    expect(array.shift()).to.equal('1');
    expect(array.length).to.equal(2);
    expect(array.shift()).to.equal('2');
    expect(array.length).to.equal(1);
    expect(array.shift()).to.equal('3');
    expect(array.length).to.equal(0);
    // Java JsArray.shift() returns null on an empty array; native JS returns
    // undefined, which is the JS analog of Java's null here.
    expect(array.shift()).to.equal(undefined);
    expect(array.length).to.equal(0);
  });

  it('testForEach', () => {
    const seenValues: number[] = [];
    const array = [1, 2, 3, 4];
    array.forEach((value) => seenValues.push(value));
    expect(seenValues).to.deep.equal([1, 2, 3, 4]);
  });
});
