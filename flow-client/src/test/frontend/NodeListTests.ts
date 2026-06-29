import { expect } from '@open-wc/testing';
import { Computation, Reactive } from '../../main/frontend/internal/reactive/reactive';
import { NodeList, type ListSpliceEvent } from '../../main/frontend/internal/nodefeature/NodeList';
import type { NodeFeatureNode } from '../../main/frontend/internal/nodefeature/NodeFeature';

const node: NodeFeatureNode = {
  getTree: () => {
    throw new Error('tree not available in this test');
  },
  getDebugJson: () => null
};

function countingComputation(reader: () => void): { getCount: () => number } {
  let count = 0;
  void new Computation(() => {
    count++;
    reader();
  });
  return { getCount: () => count };
}

describe('NodeList', () => {
  let list: NodeList;
  beforeEach(() => {
    Reactive.reset();
    list = new NodeList(0, node);
  });

  it('is initially empty', () => {
    expect(list.length()).to.equal(0);
  });

  it('splices items in and out', () => {
    list.splice(0, 0, ['1', '2', '3']);
    expect(list.length()).to.equal(3);
    expect(list.get(0)).to.equal('1');
    expect(list.get(1)).to.equal('2');
    expect(list.get(2)).to.equal('3');

    list.splice(0, 2);
    expect(list.length()).to.equal(1);
    expect(list.get(0)).to.equal('3');
  });

  it('fires splice events', () => {
    const lastEvent: { value: ListSpliceEvent | null } = { value: null };
    const remover = list.addSpliceListener((event) => {
      expect(lastEvent.value, 'Got unexpected event').to.equal(null);
      lastEvent.value = event;
    });

    list.splice(0, 0, ['1', '2', '3']);
    const addEvent = lastEvent.value!;
    expect(addEvent.getSource()).to.equal(list);
    expect(addEvent.getIndex()).to.equal(0);
    expect(addEvent.getRemove().length).to.equal(0);
    expect(addEvent.getAdd()).to.deep.equal(['1', '2', '3']);

    lastEvent.value = null;
    list.splice(1, 2);
    const removeEvent = lastEvent.value!;
    expect(removeEvent.getIndex()).to.equal(1);
    expect(removeEvent.getAdd().length).to.equal(0);
    expect(removeEvent.getRemove()).to.deep.equal(['2', '3']);

    remover.remove();
    list.splice(0, 0, ['1', '2', '3']);
    expect(lastEvent.value).to.equal(removeEvent);
  });

  it('invalidates a length read when items are added', () => {
    const { getCount } = countingComputation(() => list.length());
    Reactive.flush();
    expect(getCount()).to.equal(1);
    list.add(0, '1');
    expect(getCount()).to.equal(1);
    Reactive.flush();
    expect(getCount()).to.equal(2);
    list.get(0);
    Reactive.flush();
    expect(getCount()).to.equal(2);
  });

  it('iterates items in order', () => {
    list.add(0, 'foo');
    list.add(1, 'bar');
    list.add(0, 'baz');

    const forEachList: unknown[] = [];
    list.forEach((item) => forEachList.push(item));
    expect(forEachList).to.deep.equal(['baz', 'foo', 'bar']);
  });
});
